package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ToppleCaseData;
import io.github.samzhu.topplecat.core.ToppleCatException;
import io.github.samzhu.topplecat.core.CaseVisibility;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** A typed, one-row input/expected contract injected into an AC-bound JUnit test. */
public final class ToppleCase {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ToppleCaseData data;
    private final Map<String, ExpectedConsumption> consumption = new LinkedHashMap<>();
    private ToppleNarrative.Session narrative;

    public ToppleCase(ToppleCaseData data) {
        this.data = data;
        data.expected().propertyNames().forEach(key -> consumption.put(key, ExpectedConsumption.UNTOUCHED));
    }

    public String caseId() {
        return data.caseId();
    }

    public String acId() {
        return data.acId();
    }

    public boolean hidden() {
        return data.visibility() == CaseVisibility.HIDDEN;
    }

    /** Visibility of this row for report-boundary decisions. */
    public CaseVisibility visibility() {
        return data.visibility();
    }

    public JsonNode inputs() {
        return data.inputs().deepCopy();
    }

    public JsonNode expected() {
        return data.expected().deepCopy();
    }

    public <T> T input(String key, Class<T> type) {
        return convert("input", data.inputs(), key, type);
    }

    public <T> T input(String key, TypeReference<T> type) {
        return convert("input", data.inputs(), key, type);
    }

    /** Reads an expected value but does not fulfil its verification obligation. */
    public <T> T expected(String key, Class<T> type) {
        markRead(key);
        return convert("expected", data.expected(), key, type);
    }

    /** Reads an expected value but does not fulfil its verification obligation. */
    public <T> T expected(String key, TypeReference<T> type) {
        markRead(key);
        return convert("expected", data.expected(), key, type);
    }

    /** Deeply compares an actual value with a declared {@code expected.<key>} contract. */
    public void verify(String key, Object actual) {
        JsonNode expected = value("expected", data.expected(), key);
        consumption.put(key, ExpectedConsumption.ASSERTED);
        JsonNode actualNode;
        try {
            actualNode = actual instanceof JsonNode node ? node : JSON.valueToTree(actual);
        } catch (RuntimeException exception) {
            throw new ToppleCatException("Topple case " + caseId() + " cannot serialize actual for expected." + key, exception);
        }
        if (!expected.equals(actualNode)) {
            if (narrative != null) {
                narrative.markCurrentStepFailed();
            }
            throw new AssertionError("Topple case " + caseId() + " expected." + key + " did not match actual. "
                    + "Expected " + expected + " but was " + actualNode + ".");
        }
    }

    public Map<String, ExpectedConsumption> expectedConsumption() {
        return Map.copyOf(consumption);
    }

    String firstUntouchedExpectedKey() {
        return consumption.entrySet().stream()
                .filter(entry -> entry.getValue() == ExpectedConsumption.UNTOUCHED)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    void bindNarrative(ToppleNarrative.Session session) {
        this.narrative = session;
    }

    private <T> T convert(String side, JsonNode payload, String key, Class<T> type) {
        try {
            return JSON.treeToValue(value(side, payload, key), type);
        } catch (RuntimeException exception) {
            throw conversionFailure(side, key, type.getTypeName(), exception);
        }
    }

    private <T> T convert(String side, JsonNode payload, String key, TypeReference<T> type) {
        try {
            return JSON.treeToValue(value(side, payload, key), type);
        } catch (RuntimeException exception) {
            throw conversionFailure(side, key, type.getType().getTypeName(), exception);
        }
    }

    private JsonNode value(String side, JsonNode payload, String key) {
        if (key == null || key.isBlank()) {
            throw new ToppleCatException("Topple case " + caseId() + " " + side + " key is required.");
        }
        JsonNode value = payload.get(key);
        if (value == null) {
            throw new ToppleCatException("Topple case " + caseId() + " has no " + side + "." + key + ".");
        }
        return value;
    }

    private void markRead(String key) {
        value("expected", data.expected(), key);
        consumption.computeIfPresent(key, (ignored, state) -> state == ExpectedConsumption.ASSERTED
                ? state : ExpectedConsumption.READ);
    }

    private ToppleCatException conversionFailure(String side, String key, String type, RuntimeException exception) {
        return new ToppleCatException("Topple case " + caseId() + " cannot deserialize " + side + "." + key
                + " as " + type + ": " + exception.getMessage(), exception);
    }

    @Override
    public String toString() {
        return "[case:" + caseId() + "]";
    }
}
