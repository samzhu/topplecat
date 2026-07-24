package io.github.samzhu.topplecat.core;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractDefinitionJsonTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void canonicalDigestIgnoresSourceCoordinatesAndDiscoveryOrderButChangesForContractContent() throws Exception {
        ContractDefinition first = definition("Title", "/inputs/cart/id", "customer-1", "Fixture.java", 1);
        ContractDefinition reorderedAndReformatted = definition("Title", "/inputs/cart/id", "customer-1", "Renamed.java", 99);
        ContractDefinition changedTemplate = definition("Changed title", "/inputs/cart/id", "customer-1", "Fixture.java", 1);
        ContractDefinition changedBinding = definition("Title", "/inputs/cart/customerId", "customer-1", "Fixture.java", 1);
        ContractDefinition changedCase = definition("Title", "/inputs/cart/id", "customer-2", "Fixture.java", 1);

        assertEquals(first.digest(), reorderedAndReformatted.digest());
        assertNotEquals(first.digest(), changedTemplate.digest());
        assertNotEquals(first.digest(), changedBinding.digest());
        assertNotEquals(first.digest(), changedCase.digest());
    }

    @Test
    void jsonRoundTripsAndUnknownSchemaFailsClosed() throws Exception {
        ContractDefinition definition = definition("Title", "/inputs/cart/id", "customer-1", "Fixture.java", 1);

        assertEquals(definition, ContractDefinitionJson.read(ContractDefinitionJson.write(definition)));
        assertThrows(ToppleCatException.class, () -> ContractDefinitionJson.read("""
                {"schemaVersion":"topplecat.contract-definition.v999","digest":"abc","acceptanceConditions":[]}
                """));
    }

    @Test
    void rejectsTemplatePlaceholdersWithoutMatchingStructuredBindings() {
        assertThrows(ToppleCatException.class, () -> new StepTemplate("fixture.Stage#step()V", StepPhase.GIVEN,
                List.of(new StepToken(StepTokenKind.ARGUMENT, "1")),
                List.of(new ArgumentBinding(0, "value", "/inputs/value")), new SourceRef("Fixture.java", 1, 1)));
    }

    private static ContractDefinition definition(String title, String binding, String customer, String file, long line)
            throws Exception {
        StepTemplate step = new StepTemplate("fixture.Given#a_cart(Lfixture/Cart;)Lfixture/Given;", StepPhase.GIVEN,
                List.of(new StepToken(StepTokenKind.PHASE, "GIVEN"), new StepToken(StepTokenKind.LITERAL, "prepare "),
                        new StepToken(StepTokenKind.ARGUMENT, "0")),
                List.of(new ArgumentBinding(0, "customerId", binding)), new SourceRef(file, line, 1));
        CaseDefinition later = new CaseDefinition("case-z", "AC-DEFINITION", CaseVisibility.PUBLIC,
                JSON.readTree("{\"cart\":{\"id\":\"" + customer + "\"}}"), JSON.readTree("{\"receipt\":true}"));
        CaseDefinition earlier = new CaseDefinition("case-a", "AC-DEFINITION", CaseVisibility.HIDDEN,
                JSON.readTree("{\"cart\":{\"id\":\"" + customer + "\"}}"), JSON.readTree("{\"receipt\":true}"));
        return ContractDefinition.withComputedDigest(List.of(new AcceptanceContract("AC-DEFINITION", title,
                new ScenarioTemplate("AC-DEFINITION|fixture.Test#accepts(LToppleCase;)V", "fixture.Test#accepts(LToppleCase;)V",
                        new SourceRef(file, line, 1), List.of(step)), List.of(later, earlier))));
    }
}
