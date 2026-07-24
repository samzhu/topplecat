package io.github.samzhu.topplecat.junit;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import io.github.samzhu.topplecat.core.ArgumentBinding;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compiler-backed validator and descriptor emitter for the deliberately narrow
 * {@link ToppleTest} orchestration grammar. Java symbol resolution is delegated to javac;
 * this processor never guesses imports, types, overloads, or argument semantics from text.
 */
@SupportedAnnotationTypes("io.github.samzhu.topplecat.junit.ToppleTest")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedOptions("org.gradle.annotation.processing.isolating")
public final class ToppleScenarioProcessor extends AbstractProcessor {
    private static final String DESCRIPTOR_DIRECTORY = "META-INF/topplecat/contracts/";
    private static final String INDEX = DESCRIPTOR_DIRECTORY + "index";

    private Trees trees;
    private Elements elements;
    private Types types;
    private TypeMirror toppleStage;
    private TypeMirror toppleCase;
    private final Set<String> handledMethods = new LinkedHashSet<>();
    private final Set<String> descriptorFiles = new LinkedHashSet<>();
    private boolean wroteIndex;

    @Override
    public synchronized void init(ProcessingEnvironment environment) {
        super.init(environment);
        trees = Trees.instance(environment);
        elements = environment.getElementUtils();
        types = environment.getTypeUtils();
        toppleStage = typeOf(ToppleStage.class.getCanonicalName());
        toppleCase = typeOf(ToppleCase.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        if (!round.processingOver()) {
            for (Element element : round.getElementsAnnotatedWith(ToppleTest.class)) {
                if (element.getKind() == ElementKind.METHOD && handledMethods.add(methodKey((ExecutableElement) element))) {
                    processMethod((ExecutableElement) element);
                }
            }
        } else if (!wroteIndex && !round.errorRaised()) {
            writeIndex();
        }
        return false;
    }

    private void processMethod(ExecutableElement method) {
        TreePath methodPath = trees.getPath(method);
        MethodTree methodTree = methodPath == null ? null : (MethodTree) trees.getTree(method);
        if (methodPath == null || methodTree == null || methodTree.getBody() == null) {
            return;
        }
        String acId = annotationValue(method, ToppleTest.class.getCanonicalName(), "value");
        boolean valid = true;
        if (acId == null || acId.isBlank()) {
            error("unknown", methodPath, methodTree, "an explicit AC id is required",
                    "Use @ToppleTest(\"AC-...\") on this canonical method.");
            return;
        }
        List<StepTemplate> steps = new ArrayList<>();
        for (StatementTree statement : methodTree.getBody().getStatements()) {
            TreePath statementPath = new TreePath(methodPath, statement);
            StepTemplate step = parseCanonicalStatement(acId, method, statementPath, statement);
            if (step == null) {
                valid = false;
            } else {
                steps.add(step);
            }
        }
        if (steps.isEmpty() && valid) {
            error(acId, methodPath, methodTree, "a canonical method must contain direct Stage calls",
                    "Move orchestration into direct @ToppleStageField.step(...); statements.");
            valid = false;
        }
        if (!valid) {
            return;
        }
        TypeElement owner = (TypeElement) method.getEnclosingElement();
        String ownerBinary = elements.getBinaryName(owner).toString();
        String descriptor = methodDescriptor(method);
        String scenarioId = acId + "|" + ownerBinary + "#" + method.getSimpleName() + descriptor;
        CompilerScenarioDescriptor output = new CompilerScenarioDescriptor(
                CompilerScenarioDescriptor.SCHEMA_VERSION,
                acId,
                title(method),
                scenarioId,
                ownerBinary,
                method.getSimpleName().toString(),
                descriptor,
                sourceRef(methodPath, methodTree),
                steps
        );
        writeDescriptor(output);
    }

    private StepTemplate parseCanonicalStatement(String acId, ExecutableElement canonicalMethod,
                                                 TreePath statementPath, StatementTree statement) {
        if (!(statement instanceof ExpressionStatementTree expressionStatement)
                || !(expressionStatement.getExpression() instanceof MethodInvocationTree invocation)
                || !(invocation.getMethodSelect() instanceof MemberSelectTree select)) {
            error(acId, statementPath, statement, "only direct Stage method calls are allowed",
                    "Move locals, assertions, control flow, lambdas, and business work into a ToppleStage step.");
            return null;
        }
        TreePath invocationPath = new TreePath(statementPath, invocation);
        TreePath selectPath = new TreePath(invocationPath, select);
        Element receiver = trees.getElement(new TreePath(selectPath, select.getExpression()));
        if (!(receiver instanceof VariableElement field) || field.getKind() != ElementKind.FIELD
                || !hasAnnotation(field, ToppleStageField.class.getCanonicalName())
                || !field.getEnclosingElement().equals(canonicalMethod.getEnclosingElement())) {
            error(acId, statementPath, select.getExpression(), "the receiver must be a same-class @ToppleStageField",
                    "Call a Stage field declared on this test class directly.");
            return null;
        }
        Element resolved = trees.getElement(selectPath);
        // A malformed Java invocation has no executable symbol yet. Leave that diagnostic to javac so a
        // type/syntax error is never relabelled as a ToppleCat DSL violation or followed by a descriptor.
        if (!(resolved instanceof ExecutableElement stepMethod)) {
            return null;
        }
        if (!isStageMethod(stepMethod)) {
            error(acId, statementPath, invocation.getMethodSelect(), "the call must resolve to a ToppleStage method",
                    "Move production calls and helpers into a Stage method, then call that field method directly.");
            return null;
        }
        if (!validateStageDefinition(acId, statementPath, stepMethod)) {
            return null;
        }
        Map<String, ArgumentPath> incoming = new LinkedHashMap<>();
        List<? extends VariableElement> parameters = stepMethod.getParameters();
        if (parameters.size() != invocation.getArguments().size()) {
            error(acId, statementPath, invocation, "the resolved Stage overload has an invalid argument shape",
                    "Use the resolved Stage method signature exactly.");
            return null;
        }
        for (int index = 0; index < parameters.size(); index++) {
            ExpressionTree argument = invocation.getArguments().get(index);
            ArgumentPath argumentPath = canonicalArgument(acId, invocationPath, argument, canonicalMethod);
            if (argumentPath == null) {
                return null;
            }
            incoming.put(parameters.get(index).getSimpleName().toString(), argumentPath);
        }
        Recorded recorded = recordedInvocation(stepMethod);
        if (recorded == null) {
            error(acId, statementPath, invocation, "the resolved Stage method has no first recorded(...) call",
                    "Start every Stage step with recorded(...) before business work.");
            return null;
        }
        List<ArgumentBinding> bindings = new ArrayList<>();
        for (int index = 0; index < recorded.arguments().size(); index++) {
            ExpressionTree argument = recorded.arguments().get(index);
            ArgumentPath path = recordedArgument(argument, recorded.path(), incoming);
            if (path == null) {
                error(acId, statementPath, invocation, "recorded(...) arguments must be Stage parameters or their property paths",
                        "Record a literal, a direct parameter, or a record/bean/field property rooted at that parameter.");
                return null;
            }
            bindings.add(new ArgumentBinding(index, path.displayName(), path.pointer()));
        }
        StepPhase phase = phase(field.getSimpleName().toString());
        List<StepToken> tokens = tokens(stepMethod, phase, bindings.size());
        for (StepToken token : tokens) {
            if (token.kind() == StepTokenKind.ARGUMENT && Integer.parseInt(token.value()) >= bindings.size()) {
                error(acId, statementPath, invocation, "the @As template references an unavailable recorded argument",
                        "Use only placeholders for recorded(...) arguments, starting at {0}.");
                return null;
            }
        }
        TypeElement stepOwner = (TypeElement) stepMethod.getEnclosingElement();
        String stepId = elements.getBinaryName(stepOwner) + "#" + stepMethod.getSimpleName() + methodDescriptor(stepMethod);
        return new StepTemplate(stepId, phase, tokens, bindings, sourceRef(statementPath, invocation));
    }

    private boolean validateStageDefinition(String acId, TreePath canonicalPath, ExecutableElement stepMethod) {
        TreePath path = trees.getPath(stepMethod);
        MethodTree tree = path == null ? null : (MethodTree) trees.getTree(stepMethod);
        if (path == null || tree == null || tree.getBody() == null) {
            error(acId, canonicalPath, canonicalPath.getLeaf(), "the Stage method must be available as Java source",
                    "Declare the Stage vocabulary in compiled test source so ToppleCat can verify its recording contract.");
            return false;
        }
        List<? extends StatementTree> statements = tree.getBody().getStatements();
        if (statements.isEmpty() || !isRecordedStatement(new TreePath(path, statements.getFirst()), statements.getFirst())) {
            error(acId, path, tree, "a Stage method must call recorded(...) as its first executable action",
                    "Put recorded(...) before the step's business work.");
            return false;
        }
        StatementTree last = statements.getLast();
        if (!(last instanceof ReturnTree returnTree) || !(returnTree.getExpression() instanceof MethodInvocationTree call)
                || !"self".contentEquals(methodName(call))) {
            error(acId, path, last, "a Stage method must end with return self()",
                    "Return self() after the step's work to keep the Stage API fluent and explicit.");
            return false;
        }
        return true;
    }

    private boolean isRecordedStatement(TreePath path, StatementTree statement) {
        if (!(statement instanceof ExpressionStatementTree expression)
                || !(expression.getExpression() instanceof MethodInvocationTree invocation)) {
            return false;
        }
        TreePath invocationPath = new TreePath(path, invocation);
        Element method = trees.getElement(new TreePath(invocationPath, invocation.getMethodSelect()));
        return method instanceof ExecutableElement executable
                && "recorded".contentEquals(executable.getSimpleName())
                && isAssignable(executable.getEnclosingElement().asType(), toppleStage);
    }

    private Recorded recordedInvocation(ExecutableElement method) {
        TreePath methodPath = trees.getPath(method);
        MethodTree tree = methodPath == null ? null : (MethodTree) trees.getTree(method);
        if (tree == null || tree.getBody() == null || tree.getBody().getStatements().isEmpty()) {
            return null;
        }
        StatementTree first = tree.getBody().getStatements().getFirst();
        if (!(first instanceof ExpressionStatementTree statement)
                || !(statement.getExpression() instanceof MethodInvocationTree invocation)
                || !isRecordedStatement(new TreePath(methodPath, first), first)) {
            return null;
        }
        return new Recorded(new TreePath(new TreePath(methodPath, first), invocation), invocation.getArguments());
    }

    private ArgumentPath canonicalArgument(String acId, TreePath parent, ExpressionTree expression,
                                           ExecutableElement canonicalMethod) {
        if (expression instanceof LiteralTree literal) {
            return new ArgumentPath("", String.valueOf(literal.getValue()));
        }
        if (expression instanceof IdentifierTree identifier) {
            Element element = trees.getElement(new TreePath(parent, identifier));
            if (element instanceof VariableElement parameter && parameter.getKind() == ElementKind.PARAMETER
                    && canonicalMethod.getParameters().contains(parameter) && isAssignable(parameter.asType(), toppleCase)) {
                return new ArgumentPath("", "case");
            }
            error(acId, parent, expression, "local variables and arbitrary identifiers are not allowed as Stage arguments",
                    "Pass a literal, ToppleCase input/expected value, or a property rooted at one of them.");
            return null;
        }
        if (expression instanceof MethodInvocationTree invocation) {
            TreePath invocationPath = new TreePath(parent, invocation);
            Element target = trees.getElement(new TreePath(invocationPath, invocation.getMethodSelect()));
            if (target instanceof ExecutableElement method && isToppleCaseAccessor(method, invocation, invocationPath)) {
                String side = "input".contentEquals(method.getSimpleName()) ? "inputs" : "expected";
                Object key = ((LiteralTree) invocation.getArguments().getFirst()).getValue();
                return new ArgumentPath("/" + side + "/" + pointerEscape(String.valueOf(key)), String.valueOf(key));
            }
            ArgumentPath property = propertyInvocation(invocation, invocationPath,
                    (rootPath, propertyRoot) -> canonicalArgument(acId, rootPath, propertyRoot, canonicalMethod));
            if (property != null) {
                return property;
            }
            error(acId, parent, expression, "helper, SUT, constructor, and unrelated method calls are not allowed in Stage arguments",
                    "Move that execution into a ToppleStage step; pass only literals or values rooted at ToppleCase input/expected.");
            return null;
        }
        if (expression instanceof MemberSelectTree select) {
            TreePath selectPath = new TreePath(parent, select);
            Element member = trees.getElement(selectPath);
            if (member instanceof VariableElement field && field.getKind() == ElementKind.FIELD && !field.getModifiers().contains(Modifier.STATIC)) {
                ArgumentPath root = canonicalArgument(acId, selectPath, select.getExpression(), canonicalMethod);
                return root == null ? null : root.child(field.getSimpleName().toString());
            }
        }
        error(acId, parent, expression, "this argument expression is not part of the allowed case-data grammar",
                "Use a literal, ToppleCase input/expected accessor, or an allowed property path.");
        return null;
    }

    private ArgumentPath recordedArgument(ExpressionTree expression, TreePath parent, Map<String, ArgumentPath> incoming) {
        if (expression instanceof LiteralTree literal) {
            return new ArgumentPath("", String.valueOf(literal.getValue()));
        }
        if (expression instanceof IdentifierTree identifier) {
            ArgumentPath value = incoming.get(identifier.getName().toString());
            return value == null ? null : value;
        }
        if (expression instanceof MemberSelectTree select) {
            TreePath selectPath = new TreePath(parent, select);
            Element member = trees.getElement(selectPath);
            if (member instanceof VariableElement field && field.getKind() == ElementKind.FIELD && !field.getModifiers().contains(Modifier.STATIC)) {
                ArgumentPath root = recordedArgument(select.getExpression(), selectPath, incoming);
                return root == null ? null : root.child(field.getSimpleName().toString());
            }
        }
        if (expression instanceof MethodInvocationTree invocation) {
            TreePath invocationPath = new TreePath(parent, invocation);
            return propertyInvocation(invocation, invocationPath,
                    (rootPath, value) -> recordedArgument(value, rootPath, incoming));
        }
        return null;
    }

    private ArgumentPath propertyInvocation(MethodInvocationTree invocation, TreePath parent,
                                            java.util.function.BiFunction<TreePath, ExpressionTree, ArgumentPath> rootResolver) {
        if (!(invocation.getMethodSelect() instanceof MemberSelectTree select) || !invocation.getArguments().isEmpty()) {
            return null;
        }
        Element target = trees.getElement(new TreePath(parent, invocation.getMethodSelect()));
        if (!(target instanceof ExecutableElement method) || !isPropertyMethod(method)) {
            return null;
        }
        TreePath selectPath = new TreePath(parent, select);
        ArgumentPath root = rootResolver.apply(selectPath, select.getExpression());
        return root == null ? null : root.child(propertyName(method));
    }

    private boolean isToppleCaseAccessor(ExecutableElement method, MethodInvocationTree invocation, TreePath parent) {
        if (!("input".contentEquals(method.getSimpleName()) || "expected".contentEquals(method.getSimpleName()))
                || !isAssignable(method.getEnclosingElement().asType(), toppleCase)
                || invocation.getArguments().isEmpty() || !(invocation.getArguments().getFirst() instanceof LiteralTree literal)
                || !(literal.getValue() instanceof String)) {
            return false;
        }
        if (!(invocation.getMethodSelect() instanceof MemberSelectTree select)) {
            return false;
        }
        TreePath selectPath = new TreePath(parent, select);
        Element receiver = trees.getElement(new TreePath(selectPath, select.getExpression()));
        return receiver instanceof VariableElement variable && variable.getKind() == ElementKind.PARAMETER
                && isAssignable(variable.asType(), toppleCase);
    }

    private boolean isPropertyMethod(ExecutableElement method) {
        if (!method.getParameters().isEmpty() || method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }
        TypeElement owner = (TypeElement) method.getEnclosingElement();
        if (owner.getKind() == ElementKind.RECORD) {
            return owner.getRecordComponents().stream().map(RecordComponentElement::getAccessor)
                    .anyMatch(accessor -> accessor.equals(method));
        }
        String name = method.getSimpleName().toString();
        return (name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2);
    }

    private String propertyName(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return name;
    }

    private List<StepToken> tokens(ExecutableElement method, StepPhase phase, int arguments) {
        List<StepToken> tokens = new ArrayList<>();
        tokens.add(new StepToken(StepTokenKind.PHASE, phase.name()));
        String template = annotationValue(method, As.class.getCanonicalName(), "value");
        if (template == null || template.isBlank()) {
            tokens.add(new StepToken(StepTokenKind.LITERAL, words(method.getSimpleName().toString())));
            for (int index = 0; index < arguments; index++) {
                tokens.add(new StepToken(StepTokenKind.ARGUMENT, Integer.toString(index)));
            }
            return List.copyOf(tokens);
        }
        int index = 0;
        while (index < template.length()) {
            int opening = template.indexOf('{', index);
            if (opening < 0) {
                addLiteral(tokens, template.substring(index));
                break;
            }
            addLiteral(tokens, template.substring(index, opening));
            int closing = template.indexOf('}', opening + 1);
            if (closing < 0 || !template.substring(opening + 1, closing).matches("\\d+")) {
                tokens.add(new StepToken(StepTokenKind.LITERAL, template.substring(opening)));
                break;
            }
            tokens.add(new StepToken(StepTokenKind.ARGUMENT, template.substring(opening + 1, closing)));
            index = closing + 1;
        }
        return List.copyOf(tokens);
    }

    private static void addLiteral(List<StepToken> tokens, String value) {
        if (!value.isEmpty()) {
            tokens.add(new StepToken(StepTokenKind.LITERAL, value));
        }
    }

    private StepPhase phase(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("given")) {
            return StepPhase.GIVEN;
        }
        if (normalized.startsWith("when")) {
            return StepPhase.WHEN;
        }
        if (normalized.startsWith("then")) {
            return StepPhase.THEN;
        }
        return StepPhase.AND;
    }

    private boolean isStageMethod(ExecutableElement method) {
        return method.getEnclosingElement() instanceof TypeElement owner && isAssignable(owner.asType(), toppleStage)
                && !method.getModifiers().contains(Modifier.STATIC);
    }

    private boolean isAssignable(TypeMirror candidate, TypeMirror target) {
        return candidate != null && target != null && types.isAssignable(types.erasure(candidate), types.erasure(target));
    }

    private TypeMirror typeOf(String name) {
        TypeElement type = elements.getTypeElement(name);
        return type == null ? null : type.asType();
    }

    private boolean hasAnnotation(Element element, String type) {
        return annotationValue(element, type, "value") != null || element.getAnnotationMirrors().stream()
                .anyMatch(annotation -> annotation.getAnnotationType().toString().equals(type));
    }

    private String annotationValue(Element element, String annotationType, String member) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (!annotation.getAnnotationType().toString().equals(annotationType)) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry
                    : elements.getElementValuesWithDefaults(annotation).entrySet()) {
                if (member.contentEquals(entry.getKey().getSimpleName())) {
                    Object value = entry.getValue().getValue();
                    return value instanceof String string ? string : String.valueOf(value);
                }
            }
            return "";
        }
        return null;
    }

    private String title(ExecutableElement method) {
        String displayName = annotationValue(method, "org.junit.jupiter.api.DisplayName", "value");
        return displayName == null || displayName.isBlank() ? words(method.getSimpleName().toString()) : displayName;
    }

    private static String words(String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').trim();
    }

    private SourceRef sourceRef(TreePath path, Tree tree) {
        long position = trees.getSourcePositions().getStartPosition(path.getCompilationUnit(), tree);
        long line = position < 0 ? 1 : path.getCompilationUnit().getLineMap().getLineNumber(position);
        long column = position < 0 ? 1 : path.getCompilationUnit().getLineMap().getColumnNumber(position);
        return new SourceRef(sourceName(path), Math.max(1, line), Math.max(1, column));
    }

    private void error(String acId, TreePath path, Tree tree, String rule, String repair) {
        SourceRef source = sourceRef(path, tree);
        String message = "AC " + acId + " at " + source.file() + ":" + source.line() + ":" + source.column()
                + " violates the @ToppleTest Stage DSL:\n" + rule + ". " + repair;
        trees.printMessage(Diagnostic.Kind.ERROR, message, tree, path.getCompilationUnit());
    }

    private String sourceName(TreePath path) {
        URI uri = path.getCompilationUnit().getSourceFile().toUri();
        try {
            String value = java.nio.file.Path.of(uri).getFileName().toString();
            return value.isBlank() ? path.getCompilationUnit().getSourceFile().getName() : value;
        } catch (RuntimeException ignored) {
            return path.getCompilationUnit().getSourceFile().getName();
        }
    }

    private Name methodName(MethodInvocationTree invocation) {
        if (invocation.getMethodSelect() instanceof IdentifierTree identifier) {
            return identifier.getName();
        }
        if (invocation.getMethodSelect() instanceof MemberSelectTree select) {
            return select.getIdentifier();
        }
        return null;
    }

    private String methodKey(ExecutableElement method) {
        return ((TypeElement) method.getEnclosingElement()).getQualifiedName() + "#" + method.getSimpleName()
                + methodDescriptor(method);
    }

    private String methodDescriptor(ExecutableElement method) {
        StringBuilder result = new StringBuilder("(");
        method.getParameters().forEach(parameter -> result.append(typeDescriptor(parameter.asType())));
        return result.append(')').append(typeDescriptor(method.getReturnType())).toString();
    }

    private String typeDescriptor(TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "Z";
            case BYTE -> "B";
            case SHORT -> "S";
            case INT -> "I";
            case LONG -> "J";
            case CHAR -> "C";
            case FLOAT -> "F";
            case DOUBLE -> "D";
            case VOID -> "V";
            case ARRAY -> "[" + typeDescriptor(((ArrayType) type).getComponentType());
            case DECLARED -> "L" + elements.getBinaryName((TypeElement) ((DeclaredType) type).asElement())
                    .toString().replace('.', '/') + ";";
            default -> "L" + types.erasure(type).toString().replace('.', '/') + ";";
        };
    }

    private void writeDescriptor(CompilerScenarioDescriptor descriptor) {
        String name = sha256(descriptor.scenarioId()) + ".json";
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", DESCRIPTOR_DIRECTORY + name);
            try (Writer writer = file.openWriter()) {
                writer.write(json(descriptor));
            }
            descriptorFiles.add(name);
        } catch (IOException exception) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "ToppleCat could not write compiler descriptor: " + exception.getMessage());
        }
    }

    private void writeIndex() {
        wroteIndex = true;
        if (descriptorFiles.isEmpty()) {
            return;
        }
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", INDEX);
            try (Writer writer = file.openWriter()) {
                descriptorFiles.stream().sorted().forEach(name -> {
                    try {
                        writer.write(name + "\n");
                    } catch (IOException exception) {
                        throw new IndexWriteException(exception);
                    }
                });
            } catch (IndexWriteException exception) {
                throw exception.cause;
            }
        } catch (IOException exception) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "ToppleCat could not write compiler descriptor index: " + exception.getMessage());
        }
    }

    private String json(CompilerScenarioDescriptor descriptor) {
        StringBuilder out = new StringBuilder("{\n");
        property(out, "schemaVersion", descriptor.schemaVersion(), true);
        property(out, "acId", descriptor.acId(), true);
        property(out, "title", descriptor.title(), true);
        property(out, "scenarioId", descriptor.scenarioId(), true);
        property(out, "declaringBinaryName", descriptor.declaringBinaryName(), true);
        property(out, "methodName", descriptor.methodName(), true);
        property(out, "methodDescriptor", descriptor.methodDescriptor(), true);
        out.append("  \"sourceRef\": ").append(sourceJson(descriptor.sourceRef())).append(",\n");
        out.append("  \"steps\": [\n");
        for (int stepIndex = 0; stepIndex < descriptor.steps().size(); stepIndex++) {
            StepTemplate step = descriptor.steps().get(stepIndex);
            out.append("    {\n");
            property(out, "stepId", step.stepId(), true, "      ");
            property(out, "phase", step.phase().name(), true, "      ");
            out.append("      \"tokens\": [");
            for (int tokenIndex = 0; tokenIndex < step.tokens().size(); tokenIndex++) {
                StepToken token = step.tokens().get(tokenIndex);
                out.append("{\"kind\":\"").append(jsonEscape(token.kind().name())).append("\",\"value\":\"")
                        .append(jsonEscape(token.value())).append("\"}");
                if (tokenIndex + 1 < step.tokens().size()) {
                    out.append(',');
                }
            }
            out.append("],\n      \"argumentBindings\": [");
            for (int bindingIndex = 0; bindingIndex < step.argumentBindings().size(); bindingIndex++) {
                ArgumentBinding binding = step.argumentBindings().get(bindingIndex);
                out.append("{\"index\":").append(binding.index()).append(",\"displayName\":\"")
                        .append(jsonEscape(binding.displayName())).append("\",\"jsonPointer\":\"")
                        .append(jsonEscape(binding.jsonPointer())).append("\"}");
                if (bindingIndex + 1 < step.argumentBindings().size()) {
                    out.append(',');
                }
            }
            out.append("],\n      \"sourceRef\": ").append(sourceJson(step.sourceRef())).append("\n    }");
            if (stepIndex + 1 < descriptor.steps().size()) {
                out.append(',');
            }
            out.append('\n');
        }
        return out.append("  ]\n}\n").toString();
    }

    private static String sourceJson(SourceRef source) {
        return "{\"file\":\"" + jsonEscape(source.file()) + "\",\"line\":" + source.line()
                + ",\"column\":" + source.column() + "}";
    }

    private static void property(StringBuilder output, String key, String value, boolean comma) {
        property(output, key, value, comma, "  ");
    }

    private static void property(StringBuilder output, String key, String value, boolean comma, String prefix) {
        output.append(prefix).append('"').append(key).append("\": \"").append(jsonEscape(value)).append('"');
        if (comma) {
            output.append(',');
        }
        output.append('\n');
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : digest) {
                output.append(String.format("%02x", item));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String pointerEscape(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }

    private record ArgumentPath(String pointer, String displayName) {
        private ArgumentPath child(String name) {
            String next = pointer.isEmpty() ? "" : pointer + "/" + pointerEscape(name);
            return new ArgumentPath(next, name);
        }
    }

    private record Recorded(TreePath path, List<? extends ExpressionTree> arguments) {
    }

    private static final class IndexWriteException extends RuntimeException {
        private final IOException cause;

        private IndexWriteException(IOException cause) {
            this.cause = cause;
        }
    }
}
