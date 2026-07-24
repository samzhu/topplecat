package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.NarrativeExecution;
import io.github.samzhu.topplecat.core.NarrativeStep;
import io.github.samzhu.topplecat.core.NarrativeStepStatus;
import io.github.samzhu.topplecat.core.ExpectedConsumptionExecution;
import io.github.samzhu.topplecat.core.ContractDefinition;
import io.github.samzhu.topplecat.core.ContractDefinitionJson;
import io.github.samzhu.topplecat.core.ScenarioTemplate;
import io.github.samzhu.topplecat.core.ScenarioTemplateRenderer;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.AttachmentRef;
import io.github.samzhu.topplecat.core.CaseVisibility;
import io.github.samzhu.topplecat.core.Hashing;
import io.github.samzhu.topplecat.core.ToppleCatException;
import tools.jackson.databind.json.JsonMapper;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Internal per-invocation stage state and verifier-only narrative sidecar writer. */
final class ToppleNarrative {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Object FILE_LOCK = new Object();
    private static final long MAX_ATTACHMENT_REPORT_BYTES = 100L * 1024 * 1024;
    private static final Pattern SENSITIVE_VALUE = Pattern.compile("(?i)(authorization|cookie|set-cookie|token|password|secret)"
            + "(\\s*[:=]\\s*)([^,\\s\\\"}]+|\\\"[^\\\"]*\\\")");

    private ToppleNarrative() {
    }

    static Session start(ToppleCase testCase) {
        return new Session(new Execution(testCase.caseId(), testCase.visibility(), scenario(testCase.acId())));
    }

    /** Per-JUnit-invocation state passed explicitly into each injected Stage and ToppleCase. */
    static final class Session {
        private final Execution execution;

        private Session(Execution execution) {
            this.execution = execution;
        }

        void injectStages(Object testInstance) {
            for (Field field : fields(testInstance.getClass())) {
                if (field.getAnnotation(ToppleStageField.class) == null) {
                    continue;
                }
                validateStageField(field);
                ToppleStage<?> stage = newStage(field);
                stage.bindNarrative(this);
                set(field, testInstance, stage);
                execution.stages.add(stage);
            }
        }

        void record(ToppleStage<?> stage, StackFrame caller, Object[] arguments) {
            if (!execution.stages.contains(stage)) {
                throw new ToppleCatException("ToppleStage " + stage.getClass().getName()
                        + " must be declared on the test with @ToppleStageField.");
            }
            execution.begin(stage, ToppleStage.stepId(caller), caller, arguments);
        }

        void markCurrentStepFailed() {
            execution.markCurrentStepFailed();
        }

        boolean hasTerminalFailure() {
            return execution.terminalFailure;
        }

        AssertionError parityFailure() {
            return execution.parityFailure();
        }

        void attach(ToppleAttachment attachment) {
            if (execution.active == null || execution.active.status != null) {
                throw new ToppleCatException("Topple attachment requires a currently active Stage step.");
            }
            execution.active.attach(writeAttachment(attachment, execution.visibility));
        }

        void finish(Throwable failure, Map<String, ExpectedConsumption> consumption) {
            execution.finish(failure);
            writeNarrative(execution.snapshot());
            writeExpectedConsumption(new ExpectedConsumptionExecution(execution.caseId, consumption.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().name(),
                            (left, right) -> right, LinkedHashMap::new))));
        }
    }

    private static void writeNarrative(NarrativeExecution execution) {
        String configured = System.getProperty(ToppleJunit.NARRATIVE_EVENTS_FILE_PROPERTY);
        if (configured == null || configured.isBlank() || execution.steps().isEmpty()) {
            return;
        }
        append(Path.of(configured), execution, "narrative");
    }

    private static void writeExpectedConsumption(ExpectedConsumptionExecution execution) {
        String configured = System.getProperty(ToppleJunit.EXPECTED_CONSUMPTION_EVENTS_FILE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return;
        }
        append(Path.of(configured), execution, "expected-consumption");
    }

    private static void append(Path file, Object execution, String name) {
        try {
            synchronized (FILE_LOCK) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, JSON.writeValueAsString(execution) + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot write ToppleCat " + name + " sidecar " + file + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static ScenarioTemplate scenario(String acId) {
        String configured = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return null;
        }
        try {
            ContractDefinition definition = ContractDefinitionJson.read(Files.readString(Path.of(configured)));
            return definition.acceptanceConditions().stream().filter(contract -> contract.acId().equals(acId))
                    .map(io.github.samzhu.topplecat.core.AcceptanceContract::scenario).findFirst()
                    .orElseThrow(() -> new ToppleCatException("ToppleCat contract definition has no scenario for " + acId + "."));
        } catch (IOException exception) {
            throw new ToppleCatException("Cannot read ToppleCat contract definition " + configured + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static AttachmentRef writeAttachment(ToppleAttachment attachment, CaseVisibility visibility) {
        String configured = System.getProperty(ToppleJunit.ATTACHMENTS_DIRECTORY_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new ToppleCatException("Topple attachments are available only during a configured verification run.");
        }
        byte[] bytes = attachment.content();
        if (attachment.mediaType().startsWith("text/") || attachment.mediaType().equals("application/json")) {
            bytes = redact(new String(bytes, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        }
        String digest = Hashing.sha256(bytes);
        String filename = digest + "." + attachment.extension();
        Path directory = Path.of(configured).toAbsolutePath().normalize();
        Path target = directory.resolve(filename).normalize();
        if (!target.getParent().equals(directory)) {
            throw new ToppleCatException("Topple attachment path escaped its configured directory.");
        }
        synchronized (FILE_LOCK) {
            try {
                Files.createDirectories(directory);
                if (!Files.exists(target)) {
                    long current = attachmentBytes(directory);
                    if (!attachmentReportCapacityAllows(current, bytes.length)) {
                        throw new ToppleCatException("Topple verification attachments exceed the 100 MiB report limit.");
                    }
                    Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
                }
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot write Topple attachment " + filename + ": " + exception.getMessage(), exception);
            }
        }
        return new AttachmentRef(digest, attachment.title(), attachment.mediaType(), bytes.length, visibility,
                "attachments/" + filename);
    }

    static boolean attachmentReportCapacityAllows(long currentBytes, long candidateBytes) {
        return currentBytes >= 0 && candidateBytes >= 0 && candidateBytes <= MAX_ATTACHMENT_REPORT_BYTES - currentBytes;
    }

    private static long attachmentBytes(Path directory) throws IOException {
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException exception) {
                    throw new AttachmentSizeException(exception);
                }
            }).sum();
        } catch (AttachmentSizeException exception) {
            throw exception.cause;
        }
    }

    private static String redact(String value) {
        return SENSITIVE_VALUE.matcher(value).replaceAll("$1$2***REDACTED***");
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                result.add(field);
            }
        }
        return result;
    }

    private static void validateStageField(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)
                || !ToppleStage.class.isAssignableFrom(field.getType())) {
            throw new ToppleCatException("@ToppleStageField " + field + " must be a non-static, non-final ToppleStage field.");
        }
    }

    private static ToppleStage<?> newStage(Field field) {
        try {
            Constructor<?> constructor = field.getType().getDeclaredConstructor();
            constructor.setAccessible(true);
            return (ToppleStage<?>) constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new ToppleCatException("@ToppleStageField " + field + " requires an accessible no-argument constructor.", exception);
        }
    }

    private static void set(Field field, Object target, Object value) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new ToppleCatException("Cannot inject @ToppleStageField " + field + ".", exception);
        }
    }

    private static final class Execution {
        private final String caseId;
        private final CaseVisibility visibility;
        private final ScenarioTemplate scenario;
        private final List<ToppleStage<?>> stages = new ArrayList<>();
        private final Map<FieldIdentity, StateValue> provided = new LinkedHashMap<>();
        private final List<MutableStep> steps = new ArrayList<>();
        private boolean terminalFailure;
        private int nextStep;
        private MutableStep active;

        private Execution(String caseId, CaseVisibility visibility, ScenarioTemplate scenario) {
            this.caseId = caseId;
            this.visibility = visibility;
            this.scenario = scenario;
        }

        private void begin(ToppleStage<?> stage, String runtimeStepId, StackFrame caller, Object[] arguments) {
            if (terminalFailure) {
                String sentence = scenario == null ? ToppleStage.sentence(caller, arguments)
                        : ScenarioTemplateRenderer.template(scenario.steps().get(Math.min(nextStep, scenario.steps().size() - 1)));
                steps.add(new MutableStep(runtimeStepId, sentence, arguments, NarrativeStepStatus.SKIPPED));
                throw new ToppleCatException("A prior ToppleStage step failed; skipped subsequent step: " + sentence);
            }
            completeActive(NarrativeStepStatus.PASS);
            StepTemplate expected = expected(runtimeStepId);
            String sentence = expected == null ? ToppleStage.sentence(caller, arguments)
                    : ScenarioTemplateRenderer.render(expected, strings(arguments));
            active = new MutableStep(runtimeStepId, sentence, arguments, null);
            steps.add(active);
            try {
                publishProvidedState();
                injectExpectedState(stage);
            } catch (RuntimeException exception) {
                markCurrentStepFailed();
                throw exception;
            }
        }

        private void finish(Throwable failure) {
            publishProvidedState();
            if (active != null && active.status == null) {
                active.status = failure == null ? NarrativeStepStatus.PASS
                        : failure instanceof TestAbortedException ? NarrativeStepStatus.ABORTED : NarrativeStepStatus.FAIL;
                active.finish();
            }
            if (failure != null && scenario != null) {
                while (nextStep < scenario.steps().size()) {
                    StepTemplate skipped = scenario.steps().get(nextStep++);
                    steps.add(new MutableStep(skipped.stepId(), ScenarioTemplateRenderer.template(skipped), new Object[0],
                            NarrativeStepStatus.SKIPPED));
                }
            }
        }

        private void completeActive(NarrativeStepStatus status) {
            if (active != null && active.status == null) {
                active.status = status;
                active.finish();
            }
        }

        private void markCurrentStepFailed() {
            if (active != null && active.status == null) {
                active.status = NarrativeStepStatus.FAIL;
                active.finish();
            }
            terminalFailure = true;
        }

        private StepTemplate expected(String runtimeStepId) {
            if (scenario == null) {
                return null;
            }
            if (nextStep >= scenario.steps().size()) {
                terminalFailure = true;
                throw new ToppleCatException("ToppleCat scenario " + scenario.scenarioId() + " recorded unexpected extra step "
                        + runtimeStepId + ". Remove it or add it to the canonical descriptor.");
            }
            StepTemplate expected = scenario.steps().get(nextStep++);
            if (!expected.stepId().equals(runtimeStepId)) {
                terminalFailure = true;
                throw new ToppleCatException("ToppleCat scenario " + scenario.scenarioId() + " expected step " + expected.stepId()
                        + " but runtime recorded " + runtimeStepId + ". Restore canonical Stage order.");
            }
            return expected;
        }

        private AssertionError parityFailure() {
            if (scenario == null || terminalFailure || nextStep == scenario.steps().size()) {
                return null;
            }
            return new AssertionError("ToppleCat scenario " + scenario.scenarioId() + " recorded " + nextStep + " of "
                    + scenario.steps().size() + " compiled steps. Call each canonical Stage step in source order.");
        }

        private void publishProvidedState() {
            for (ToppleStage<?> stage : stages) {
                for (Field field : fields(stage.getClass())) {
                    if (field.getAnnotation(ProvidedState.class) == null) {
                        continue;
                    }
                    if (Modifier.isStatic(field.getModifiers())) {
                        throw new ToppleCatException("@ProvidedState " + field + " must not be static.");
                    }
                    Object value = get(field, stage);
                    FieldIdentity identity = new FieldIdentity(stage, field);
                    if (value == null) {
                        provided.remove(identity);
                    } else {
                        provided.put(identity, new StateValue(field.getName(), field.getType(), value));
                    }
                }
            }
        }

        private void injectExpectedState(ToppleStage<?> stage) {
            for (Field field : fields(stage.getClass())) {
                ExpectedState expected = field.getAnnotation(ExpectedState.class);
                if (expected == null) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    throw new ToppleCatException("@ExpectedState " + field + " must be a non-static, non-final field.");
                }
                List<StateValue> matches = provided.values().stream()
                        .filter(value -> field.getType().isAssignableFrom(value.type()))
                        .toList();
                StateValue selected = select(field, matches);
                if (selected == null) {
                    if (expected.required()) {
                        throw new ToppleCatException("Required @ExpectedState " + field.getName() + " on "
                                + stage.getClass().getName() + " has no compatible @ProvidedState value.");
                    }
                    continue;
                }
                set(field, stage, selected.value());
            }
        }

        private static StateValue select(Field expected, List<StateValue> matches) {
            if (matches.isEmpty()) {
                return null;
            }
            if (matches.size() == 1) {
                return matches.getFirst();
            }
            List<StateValue> named = matches.stream().filter(value -> expected.getName().equals(value.name())).toList();
            if (named.size() == 1) {
                return named.getFirst();
            }
            throw new ToppleCatException("@ExpectedState " + expected + " matches multiple @ProvidedState values. "
                    + "Use a unique type or the same field name as the intended provider.");
        }

        private NarrativeExecution snapshot() {
            return new NarrativeExecution(scenario == null ? "" : definitionDigest(), caseId, steps.stream().map(MutableStep::snapshot).toList());
        }

        private String definitionDigest() {
            String configured = System.getProperty(ToppleJunit.CONTRACT_DEFINITION_FILE_PROPERTY);
            if (configured == null || configured.isBlank()) {
                return "";
            }
            try {
                return ContractDefinitionJson.read(Files.readString(Path.of(configured))).digest();
            } catch (IOException exception) {
                throw new ToppleCatException("Cannot read ToppleCat contract definition digest.", exception);
            }
        }
    }

    private record FieldIdentity(ToppleStage<?> stage, Field field) {
    }

    private record StateValue(String name, Class<?> type, Object value) {
    }

    private static final class MutableStep {
        private final String stepId;
        private final String sentence;
        private final List<tools.jackson.databind.JsonNode> arguments;
        private final List<AttachmentRef> attachments = new ArrayList<>();
        private final long startedAt = System.nanoTime();
        private long durationNanos;
        private NarrativeStepStatus status;

        private MutableStep(String stepId, String sentence, Object[] arguments, NarrativeStepStatus status) {
            this.stepId = stepId;
            this.sentence = sentence;
            this.arguments = java.util.Arrays.stream(arguments)
                    .map(value -> (tools.jackson.databind.JsonNode) JSON.valueToTree(value)).toList();
            this.status = status;
            if (status != null) {
                finish();
            }
        }

        private void finish() {
            if (durationNanos == 0) {
                durationNanos = Math.max(0, System.nanoTime() - startedAt);
            }
        }

        private void attach(AttachmentRef attachment) {
            attachments.add(attachment);
        }

        private NarrativeStep snapshot() {
            return new NarrativeStep(stepId, sentence, status == null ? NarrativeStepStatus.SKIPPED : status,
                    durationNanos, arguments, attachments, "");
        }
    }

    private static final class AttachmentSizeException extends RuntimeException {
        private final IOException cause;

        private AttachmentSizeException(IOException cause) {
            this.cause = cause;
        }
    }

    private static List<String> strings(Object[] values) {
        return java.util.Arrays.stream(values).map(String::valueOf).toList();
    }

    private static Object get(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new ToppleCatException("Cannot read narrative state " + field + ".", exception);
        }
    }
}
