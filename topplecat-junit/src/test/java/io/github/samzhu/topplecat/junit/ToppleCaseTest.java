package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.ToppleCaseData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ToppleCaseTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void injectsNestedDtosAndVerifiesStructuredExpectedValues() throws Exception {
        ToppleCase testCase = testCase("""
                {"cart":{"items":[{"sku":"book","quantity":2}],"subtotal":500},
                 "expectedReceipt":{"discount":100,"discountedSubtotal":400}}
                """, """
                {"receipt":{"discount":100,"discountedSubtotal":400}}
                """);

        Cart cart = testCase.input("cart", Cart.class);
        Receipt receipt = testCase.input("expectedReceipt", Receipt.class);
        testCase.verify("receipt", receipt);

        assertEquals("book", cart.items().getFirst().sku());
        assertEquals(ExpectedConsumption.ASSERTED, testCase.expectedConsumption().get("receipt"));
    }

    @Test
    void readingExpectedDoesNotCountAsVerification() throws Exception {
        ToppleCase testCase = testCase("{\"subtotal\":500}", "{\"discount\":100}");

        assertEquals(100, testCase.expected("discount", Integer.class));

        assertEquals(ExpectedConsumption.READ, testCase.expectedConsumption().get("discount"));
    }

    @Test
    void reportsAUsefulMismatch() throws Exception {
        ToppleCase testCase = testCase("{\"subtotal\":500}", "{\"discount\":100}");

        AssertionError error = assertThrows(AssertionError.class, () -> testCase.verify("discount", 99));

        assertEquals(true, error.getMessage().contains("expected.discount"));
    }

    @Test
    void numericContractAcceptsMathematicallyEqualValuesWithDifferentScale() throws Exception {
        ToppleCase testCase = testCase("{}", "{\"amount\":200}");

        testCase.verify("amount", new BigDecimal("200.00"));

        assertEquals(ExpectedConsumption.ASSERTED, testCase.expectedConsumption().get("amount"));
    }

    @Test
    void numericContractAcceptsNestedMathematicallyEqualValuesWithDifferentScale() throws Exception {
        ToppleCase testCase = testCase("{}", """
                {"receipt":{"subtotal":1,"components":[1.0,{"tax":1.00}]}}
                """);

        testCase.verify("receipt", Map.of(
                "subtotal", new BigDecimal("1.00"),
                "components", List.of(new BigDecimal("1.000"), Map.of("tax", new BigDecimal("1.0")))
        ));

        assertEquals(ExpectedConsumption.ASSERTED, testCase.expectedConsumption().get("receipt"));
    }

    @Test
    void numericContractRejectsMathematicallyDifferentValues() throws Exception {
        ToppleCase testCase = testCase("{}", "{\"amount\":200.00}");

        assertThrows(AssertionError.class, () -> testCase.verify("amount", new BigDecimal("200.01")));
    }

    @Test
    void numericContractDoesNotEquateNumbersWithText() throws Exception {
        ToppleCase testCase = testCase("{}", "{\"amount\":200.00}");

        assertThrows(AssertionError.class, () -> testCase.verify("amount", "200.00"));
    }

    @Test
    void numericContractRejectsMissingObjectFields() throws Exception {
        ToppleCase testCase = testCase("{}", "{\"receipt\":{\"total\":200,\"tax\":10}}");

        assertThrows(AssertionError.class, () -> testCase.verify("receipt", Map.of("total", 200)));
    }

    @Test
    void numericContractRejectsAdditionalObjectFields() throws Exception {
        ToppleCase testCase = testCase("{}", "{\"receipt\":{\"total\":200}}");

        assertThrows(AssertionError.class, () -> testCase.verify("receipt", Map.of("total", 200, "tax", 10)));
    }

    @Test
    void numericContractRejectsReorderedArrays() throws Exception {
        ToppleCase testCase = testCase("{}", "{\"amounts\":[1,2]}");

        assertThrows(AssertionError.class, () -> testCase.verify("amounts", List.of(2, 1)));
    }

    private static ToppleCase testCase(String inputs, String expected) throws Exception {
        return new ToppleCase(new ToppleCaseData("coupon-public-500", "AC-CART-COUPON", CaseVisibility.PUBLIC,
                JSON.readTree(inputs), JSON.readTree(expected), Path.of("cases.json")));
    }

    private record Cart(java.util.List<Item> items, int subtotal) {
    }

    private record Item(String sku, int quantity) {
    }

    private record Receipt(int discount, int discountedSubtotal) {
    }
}
