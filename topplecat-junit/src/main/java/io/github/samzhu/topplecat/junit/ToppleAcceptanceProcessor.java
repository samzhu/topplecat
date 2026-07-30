package io.github.samzhu.topplecat.junit;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import io.github.samzhu.topplecat.core.ArgumentBinding;
import io.github.samzhu.topplecat.core.CompilerPropertyDescriptor;
import io.github.samzhu.topplecat.core.CompilerScenarioDescriptor;
import io.github.samzhu.topplecat.core.ScenarioStage;
import io.github.samzhu.topplecat.core.SourceRef;
import io.github.samzhu.topplecat.core.StepPhase;
import io.github.samzhu.topplecat.core.StepTemplate;
import io.github.samzhu.topplecat.core.StepToken;
import io.github.samzhu.topplecat.core.StepTokenKind;
import io.github.samzhu.topplecat.junit.property.PropertyTrials;
import io.github.samzhu.topplecat.junit.property.ToppleProperty;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
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

/**
 * Compiler-backed validator and descriptor emitter for the deliberately narrow {@link
 * ToppleAcceptanceTest} orchestration grammar. Java symbol resolution is delegated to javac; this
 * processor never guesses imports, types, overloads, or argument semantics from text.
 */
@SupportedAnnotationTypes({
  "io.github.samzhu.topplecat.junit.ToppleAcceptanceTest",
  "io.github.samzhu.topplecat.junit.property.ToppleProperty"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
@SupportedOptions("org.gradle.annotation.processing.isolating")
public final class ToppleAcceptanceProcessor extends AbstractProcessor {
  private static final String DESCRIPTOR_DIRECTORY = "META-INF/topplecat/contracts/";
  private static final String INDEX = DESCRIPTOR_DIRECTORY + "index";
  private static final String PROPERTY_DESCRIPTOR_DIRECTORY = "META-INF/topplecat/properties/";
  private static final String PROPERTY_INDEX = PROPERTY_DESCRIPTOR_DIRECTORY + "index";

  private Trees trees;
  private Elements elements;
  private Types types;
  private TypeMirror toppleStage;
  private TypeMirror toppleCase;
  private TypeMirror toppleScenario;
  private TypeMirror propertyTrial;
  private final Set<String> handledMethods = new LinkedHashSet<>();
  private final Set<String> descriptorFiles = new LinkedHashSet<>();
  private final Set<String> propertyDescriptorFiles = new LinkedHashSet<>();
  private boolean wroteIndex;

  @Override
  public synchronized void init(ProcessingEnvironment environment) {
    super.init(environment);
    trees = Trees.instance(environment);
    elements = environment.getElementUtils();
    types = environment.getTypeUtils();
    toppleStage = typeOf(ToppleStage.class.getCanonicalName());
    toppleCase = typeOf(ToppleCase.class.getCanonicalName());
    toppleScenario = typeOf(ToppleScenario.class.getCanonicalName());
    propertyTrial = typeOf(PropertyTrials.class.getCanonicalName());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
    if (!round.processingOver()) {
      for (Element element : round.getElementsAnnotatedWith(ToppleAcceptanceTest.class)) {
        if (element.getKind() == ElementKind.METHOD
            && handledMethods.add("scenario:" + methodKey((ExecutableElement) element))) {
          processMethod((ExecutableElement) element);
        }
      }
      for (Element element : round.getElementsAnnotatedWith(ToppleProperty.class)) {
        if (element.getKind() == ElementKind.METHOD
            && handledMethods.add("property:" + methodKey((ExecutableElement) element))) {
          processProperty((ExecutableElement) element);
        }
      }
    } else if (!wroteIndex && !round.errorRaised()) {
      writeIndex();
    }
    return false;
  }

  private void processProperty(ExecutableElement method) {
    TreePath methodPath = trees.getPath(method);
    MethodTree methodTree = methodPath == null ? null : (MethodTree) trees.getTree(method);
    if (methodPath == null || methodTree == null) {
      return;
    }
    String acId = annotationValue(method, ToppleProperty.class.getCanonicalName(), "value");
    int tries = annotationInt(method, "tries");
    int maxDiscards = annotationInt(method, "maxDiscards");
    int maxShrinks = annotationInt(method, "maxShrinks");
    if (acId == null
        || acId.isBlank()
        || !literalPropertyAcId(methodTree)
        || method.getReturnType().getKind() != TypeKind.VOID
        || method.getParameters().size() != 1
        || !isAssignable(method.getParameters().getFirst().asType(), propertyTrial)
        || tries < 1
        || tries > 100_000
        || maxDiscards < 0
        || maxShrinks < 0) {
      propertyError(
          acId == null ? "unknown" : acId,
          methodPath,
          methodTree,
          "a Property must use a literal AC id, return void, declare exactly one PropertyTrials"
              + " parameter, and use valid limits",
          "Use @ToppleProperty(\"AC-...\") void method(PropertyTrials trial) with tries 1..100000"
              + " and non-negative limits.");
      return;
    }
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    CompilerPropertyDescriptor output =
        new CompilerPropertyDescriptor(
            CompilerPropertyDescriptor.SCHEMA_VERSION,
            acId,
            title(method),
            elements.getBinaryName(owner).toString(),
            method.getSimpleName().toString(),
            methodDescriptor(method),
            sourceRef(methodPath, methodTree),
            tries,
            maxDiscards,
            maxShrinks,
            sourceDigest(methodPath, methodTree));
    writePropertyDescriptor(output);
  }

  private void processMethod(ExecutableElement method) {
    TreePath methodPath = trees.getPath(method);
    MethodTree methodTree = methodPath == null ? null : (MethodTree) trees.getTree(method);
    if (methodPath == null || methodTree == null || methodTree.getBody() == null) {
      return;
    }
    String acId = annotationValue(method, ToppleAcceptanceTest.class.getCanonicalName(), "value");
    boolean valid = true;
    if (acId == null || acId.isBlank()) {
      error(
          "unknown",
          methodPath,
          methodTree,
          "an explicit AC id is required",
          "Use @ToppleAcceptanceTest(\"AC-...\") on this acceptance method.");
      return;
    }
    ScenarioParameters scenarioParameters =
        scenarioParameters(acId, methodPath, methodTree, method);
    if (scenarioParameters == null) {
      return;
    }
    List<StepTemplate> steps = new ArrayList<>();
    NewScenarioState newScenarioState = new NewScenarioState();
    for (StatementTree statement : methodTree.getBody().getStatements()) {
      TreePath statementPath = new TreePath(methodPath, statement);
      StepTemplate step =
          parseNewScenarioStatement(
              acId, method, scenarioParameters, newScenarioState, statementPath, statement);
      if (step == null) {
        valid = false;
      } else {
        steps.add(step);
      }
    }
    if (steps.isEmpty() && valid) {
      error(
          acId,
          methodPath,
          methodTree,
          "an acceptance method must contain compiler-described Scenario Steps",
          "Call scenario.given|when|then|and(stage).step(...) directly.");
      valid = false;
    }
    if (!valid) {
      return;
    }
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    String ownerBinary = elements.getBinaryName(owner).toString();
    String descriptor = methodDescriptor(method);
    String scenarioId = acId + "|" + ownerBinary + "#" + method.getSimpleName() + descriptor;
    CompilerScenarioDescriptor output =
        new CompilerScenarioDescriptor(
            CompilerScenarioDescriptor.SCHEMA_VERSION,
            acId,
            title(method),
            scenarioId,
            ownerBinary,
            method.getSimpleName().toString(),
            descriptor,
            sourceRef(methodPath, methodTree),
            steps,
            scenarioParameters.scenarioParameterIndex(),
            scenarioParameters.stageParameters());
    writeDescriptor(output);
  }

  private ScenarioParameters scenarioParameters(
      String acId, TreePath methodPath, MethodTree methodTree, ExecutableElement method) {
    List<? extends VariableElement> parameters = method.getParameters();
    boolean valid = true;
    if (parameters.isEmpty() || !sameErasure(parameters.getFirst().asType(), toppleCase)) {
      error(
          acId,
          methodPath,
          methodTree,
          "ToppleCase must be the first parameter of an acceptance method",
          "Declare ToppleCase first, ToppleScenario second, then one or more concrete Stages.");
      valid = false;
    }
    List<Integer> scenarioPositions = new ArrayList<>();
    for (int index = 0; index < parameters.size(); index++) {
      if (sameErasure(parameters.get(index).asType(), toppleScenario)) {
        scenarioPositions.add(index);
      }
    }
    if (scenarioPositions.size() != 1 || scenarioPositions.getFirst() != 1) {
      error(
          acId,
          methodPath,
          methodTree,
          "ToppleScenario must be the one non-generic second parameter",
          "Declare exactly one ToppleScenario immediately after ToppleCase.");
      valid = false;
    }

    List<StageParameter> stages = new ArrayList<>();
    Set<String> stageTypes = new LinkedHashSet<>();
    for (int index = 2; index < parameters.size(); index++) {
      VariableElement parameter = parameters.get(index);
      TypeElement stageType = stageType(parameter.asType());
      if (stageType == null || !isAssignable(parameter.asType(), toppleStage)) {
        error(
            acId,
            methodPath,
            methodTree,
            "only concrete ToppleStage parameters may follow ToppleScenario",
            "Move non-Stage parameters before the method or keep orchestration inside a Stage.");
        valid = false;
        continue;
      }
      if (!validateStageParameter(acId, methodPath, methodTree, stageType)) {
        valid = false;
        continue;
      }
      String binaryName = elements.getBinaryName(stageType).toString();
      if (!stageTypes.add(binaryName)) {
        error(
            acId,
            methodPath,
            methodTree,
            "a concrete Stage type may appear only once in one Scenario",
            "Use one coordinating Stage or distinct role-specific Stage types.");
        valid = false;
        continue;
      }
      stages.add(new StageParameter(parameter, stageType, index, binaryName));
    }
    if (stages.isEmpty()) {
      error(
          acId,
          methodPath,
          methodTree,
          "a new Scenario requires one or more concrete Stage parameters",
          "Declare a proxyable ToppleStage parameter after ToppleScenario.");
      valid = false;
    }
    if (!valid) {
      return null;
    }
    return new ScenarioParameters(
        parameters.get(1),
        List.copyOf(stages),
        1,
        stages.stream()
            .map(stage -> new ScenarioStage(stage.index(), stage.binaryName()))
            .toList());
  }

  private TypeElement stageType(TypeMirror type) {
    if (!(type instanceof DeclaredType declared) || !declared.getTypeArguments().isEmpty()) {
      return null;
    }
    return declared.asElement() instanceof TypeElement stage ? stage : null;
  }

  private boolean validateStageParameter(
      String acId, TreePath path, MethodTree methodTree, TypeElement stageType) {
    boolean valid = true;
    if (stageType.getKind() != ElementKind.CLASS
        || stageType.getModifiers().contains(Modifier.ABSTRACT)
        || stageType.getModifiers().contains(Modifier.FINAL)
        || stageType.getModifiers().contains(Modifier.SEALED)
        || stageType.getModifiers().contains(Modifier.PRIVATE)) {
      error(
          acId,
          path,
          methodTree,
          "a Scenario Stage must be a non-final concrete proxyable class",
          "Use a non-private, non-final ToppleStage class with an accessible no-argument"
              + " constructor.");
      valid = false;
    }
    Element enclosing = stageType.getEnclosingElement();
    if (enclosing instanceof TypeElement && !stageType.getModifiers().contains(Modifier.STATIC)) {
      error(
          acId,
          path,
          methodTree,
          "a nested Scenario Stage must be static",
          "Declare the Stage as a top-level class or static nested class.");
      valid = false;
    }
    if (enclosing instanceof ExecutableElement || stageType.getSimpleName().length() == 0) {
      error(
          acId,
          path,
          methodTree,
          "local and anonymous Stage types are not supported",
          "Declare a top-level or static nested concrete Stage class.");
      valid = false;
    }
    List<ExecutableElement> constructors =
        stageType.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
            .map(ExecutableElement.class::cast)
            .toList();
    boolean hasAccessibleNoArgConstructor =
        constructors.isEmpty()
            ? !stageType.getModifiers().contains(Modifier.PRIVATE)
            : constructors.stream()
                .anyMatch(
                    constructor ->
                        constructor.getParameters().isEmpty()
                            && !constructor.getModifiers().contains(Modifier.PRIVATE));
    if (!hasAccessibleNoArgConstructor) {
      error(
          acId,
          path,
          methodTree,
          "a Scenario Stage requires an accessible no-argument constructor",
          "Add a non-private no-argument constructor to the concrete Stage.");
      valid = false;
    }
    return valid;
  }

  private StepTemplate parseNewScenarioStatement(
      String acId,
      ExecutableElement canonicalMethod,
      ScenarioParameters parameters,
      NewScenarioState state,
      TreePath statementPath,
      StatementTree statement) {
    if (!(statement instanceof ExpressionStatementTree expressionStatement)
        || !(expressionStatement.getExpression() instanceof MethodInvocationTree invocation)
        || !(invocation.getMethodSelect() instanceof MemberSelectTree stepSelect)
        || !(stepSelect.getExpression() instanceof MethodInvocationTree selectorInvocation)
        || !(selectorInvocation.getMethodSelect() instanceof MemberSelectTree selectorSelect)) {
      error(
          acId,
          statementPath,
          statement,
          "a new Scenario statement must be scenario.given|when|then|and(stage).step_method(...)",
          "Use one direct phase selector, one declared Stage parameter, and one void Step call.");
      return null;
    }
    TreePath invocationPath = new TreePath(statementPath, invocation);
    TreePath stepSelectPath = new TreePath(invocationPath, stepSelect);
    TreePath selectorInvocationPath = new TreePath(stepSelectPath, selectorInvocation);
    TreePath selectorSelectPath = new TreePath(selectorInvocationPath, selectorSelect);
    Element scenario =
        trees.getElement(new TreePath(selectorSelectPath, selectorSelect.getExpression()));
    if (!parameters.scenarioParameter().equals(scenario)) {
      error(
          acId,
          statementPath,
          selectorSelect.getExpression(),
          "the phase selector must be invoked on this method's ToppleScenario parameter",
          "Use the declared ToppleScenario parameter directly; do not use an alias, field, or"
              + " helper.");
      return null;
    }
    StepPhase phase = phaseSelector(selectorSelect.getIdentifier().toString());
    if (phase == null) {
      error(
          acId,
          statementPath,
          selectorSelect,
          "a Scenario selector must be given, when, then, or and",
          "Use scenario.given(stage), scenario.when(stage), scenario.then(stage), or"
              + " scenario.and(stage).");
      return null;
    }
    if (selectorInvocation.getArguments().size() != 1
        || !(trees.getElement(
                new TreePath(selectorInvocationPath, selectorInvocation.getArguments().getFirst()))
            instanceof VariableElement stageParameter)) {
      error(
          acId,
          statementPath,
          selectorInvocation,
          "a Scenario selector requires one declared concrete Stage parameter",
          "Pass one of this acceptance method's trailing Stage parameters directly.");
      return null;
    }
    StageParameter stage = parameters.stage(stageParameter);
    if (stage == null) {
      error(
          acId,
          statementPath,
          selectorInvocation,
          "the selected Stage must be declared by this acceptance method",
          "Pass the exact trailing Stage parameter, not a field, local, new instance, or helper"
              + " result.");
      return null;
    }
    if (!state.accept(acId, statementPath, selectorInvocation, phase)) {
      return null;
    }
    Element resolved = trees.getElement(stepSelectPath);
    if (!(resolved instanceof ExecutableElement stepMethod)) {
      return null;
    }
    if (!stepMethod.getEnclosingElement().equals(stage.type())) {
      error(
          acId,
          statementPath,
          stepSelect,
          "a visible Step must be declared directly on the concrete Stage parameter type",
          "Declare the Step on the concrete Stage or override it there before selecting it.");
      return null;
    }
    if (!validateNewStepDefinition(acId, statementPath, stepMethod)) {
      return null;
    }
    if (stepMethod.getParameters().size() != invocation.getArguments().size()) {
      return null;
    }
    NewArguments arguments =
        newArguments(acId, invocationPath, invocation, canonicalMethod, stepMethod);
    if (arguments == null) {
      return null;
    }
    List<StepToken> tokens =
        newTokens(acId, statementPath, invocation, stepMethod, phase, arguments);
    if (tokens == null) {
      return null;
    }
    String stepId =
        stage.binaryName() + "#" + stepMethod.getSimpleName() + methodDescriptor(stepMethod);
    return new StepTemplate(
        stepId,
        phase,
        tokens,
        arguments.bindings(),
        sourceRef(statementPath, invocation),
        stage.binaryName());
  }

  private boolean validateNewStepDefinition(
      String acId, TreePath statementPath, ExecutableElement stepMethod) {
    if (stepMethod.getReturnType().getKind() != TypeKind.VOID
        || stepMethod.getModifiers().contains(Modifier.PRIVATE)
        || stepMethod.getModifiers().contains(Modifier.STATIC)
        || stepMethod.getModifiers().contains(Modifier.FINAL)
        || stepMethod.getModifiers().contains(Modifier.ABSTRACT)) {
      error(
          acId,
          statementPath,
          statementPath.getLeaf(),
          "a new Scenario Step must be a non-private, non-static, non-final void method",
          "Use an overridable void method declared directly on the concrete Stage.");
      return false;
    }
    return true;
  }

  private NewArguments newArguments(
      String acId,
      TreePath invocationPath,
      MethodInvocationTree invocation,
      ExecutableElement canonicalMethod,
      ExecutableElement stepMethod) {
    Map<String, ArgumentPath> incoming = new LinkedHashMap<>();
    Map<String, TypeMirror> parameterTypes = new LinkedHashMap<>();
    for (int index = 0; index < stepMethod.getParameters().size(); index++) {
      VariableElement parameter = stepMethod.getParameters().get(index);
      ExpressionTree argument = invocation.getArguments().get(index);
      if (sameErasure(parameter.asType(), toppleCase)) {
        if (!(trees.getElement(new TreePath(invocationPath, argument))
                instanceof VariableElement supplied)
            || !canonicalMethod.getParameters().contains(supplied)
            || !sameErasure(supplied.asType(), toppleCase)) {
          error(
              acId,
              invocationPath,
              argument,
              "a ToppleCase Step parameter must receive this acceptance method's ToppleCase",
              "Pass the first ToppleCase parameter directly.");
          return null;
        }
        continue;
      }
      ArgumentPath path = canonicalArgument(acId, invocationPath, argument, canonicalMethod);
      if (path == null) {
        return null;
      }
      incoming.put(parameter.getSimpleName().toString(), path);
      parameterTypes.put(parameter.getSimpleName().toString(), parameter.asType());
    }
    return new NewArguments(incoming, parameterTypes, new ArrayList<>());
  }

  private List<StepToken> newTokens(
      String acId,
      TreePath statementPath,
      MethodInvocationTree invocation,
      ExecutableElement method,
      StepPhase phase,
      NewArguments arguments) {
    String template = annotationValue(method, As.class.getCanonicalName(), "value");
    if (template == null || template.isBlank()) {
      arguments
          .incoming()
          .forEach(
              (name, path) -> arguments.bindings().add(binding(arguments.bindings().size(), path)));
      List<StepToken> result = new ArrayList<>();
      result.add(new StepToken(StepTokenKind.PHASE, phase.name()));
      result.add(new StepToken(StepTokenKind.LITERAL, words(method.getSimpleName().toString())));
      for (int index = 0; index < arguments.bindings().size(); index++) {
        result.add(new StepToken(StepTokenKind.ARGUMENT, Integer.toString(index)));
      }
      return List.copyOf(result);
    }
    if (template.matches(".*\\{\\d+}.*")) {
      error(
          acId,
          statementPath,
          invocation,
          "positional @As placeholders are not supported in new Scenario authoring",
          "Use a named method parameter or property path such as {cart.customerId}.");
      return null;
    }
    List<StepToken> result = new ArrayList<>();
    result.add(new StepToken(StepTokenKind.PHASE, phase.name()));
    int cursor = 0;
    while (cursor < template.length()) {
      int opening = template.indexOf('{', cursor);
      if (opening < 0) {
        addLiteral(result, template.substring(cursor));
        break;
      }
      addLiteral(result, template.substring(cursor, opening));
      int closing = template.indexOf('}', opening + 1);
      if (closing < 0) {
        error(
            acId,
            statementPath,
            invocation,
            "a named @As placeholder must end with }",
            "Use a method parameter or safe property path enclosed in braces.");
        return null;
      }
      String path = template.substring(opening + 1, closing);
      String[] parts = path.split("\\.");
      if (!path.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
        error(
            acId,
            statementPath,
            invocation,
            "a named @As placeholder must be a method parameter or property path",
            "Use a name such as {cart} or {cart.customerId}; expressions and indexes are not"
                + " allowed.");
        return null;
      }
      ArgumentPath root = arguments.incoming().get(parts[0]);
      if (root == null) {
        error(
            acId,
            statementPath,
            invocation,
            "a named @As placeholder must be rooted at a non-framework Step parameter",
            "Use one of this Step method's business parameter names.");
        return null;
      }
      ArgumentPath resolved = root;
      TypeMirror propertyType = arguments.parameterTypes().get(parts[0]);
      for (int index = 1; index < parts.length; index++) {
        propertyType = safePropertyType(propertyType, parts[index]);
        if (propertyType == null) {
          error(
              acId,
              statementPath,
              invocation,
              "a named @As property path must resolve to a record, bean, or approved field",
              "Use a property declared on the Step parameter type; arbitrary methods are not"
                  + " allowed.");
          return null;
        }
        resolved = resolved.child(parts[index]);
      }
      int bindingIndex = arguments.bindings().size();
      arguments.bindings().add(binding(bindingIndex, resolved));
      result.add(new StepToken(StepTokenKind.ARGUMENT, Integer.toString(bindingIndex)));
      cursor = closing + 1;
    }
    return List.copyOf(result);
  }

  private static ArgumentBinding binding(int index, ArgumentPath path) {
    return new ArgumentBinding(index, path.displayName(), path.pointer());
  }

  private static StepPhase phaseSelector(String name) {
    return switch (name) {
      case "given" -> StepPhase.GIVEN;
      case "when" -> StepPhase.WHEN;
      case "then" -> StepPhase.THEN;
      case "and" -> StepPhase.AND;
      default -> null;
    };
  }

  private ArgumentPath canonicalArgument(
      String acId, TreePath parent, ExpressionTree expression, ExecutableElement canonicalMethod) {
    if (expression instanceof LiteralTree literal) {
      return new ArgumentPath("", String.valueOf(literal.getValue()));
    }
    if (expression instanceof IdentifierTree identifier) {
      Element element = trees.getElement(new TreePath(parent, identifier));
      if (element instanceof VariableElement parameter
          && parameter.getKind() == ElementKind.PARAMETER
          && canonicalMethod.getParameters().contains(parameter)
          && isAssignable(parameter.asType(), toppleCase)) {
        return new ArgumentPath("", "case");
      }
      error(
          acId,
          parent,
          expression,
          "local variables and arbitrary identifiers are not allowed as Stage arguments",
          "Pass a literal, ToppleCase input/expected value, or a property rooted at one of them.");
      return null;
    }
    if (expression instanceof MethodInvocationTree invocation) {
      TreePath invocationPath = new TreePath(parent, invocation);
      Element target = trees.getElement(new TreePath(invocationPath, invocation.getMethodSelect()));
      if (target instanceof ExecutableElement method
          && isToppleCaseAccessor(method, invocation, invocationPath)) {
        String side = "input".contentEquals(method.getSimpleName()) ? "inputs" : "expected";
        Object key = ((LiteralTree) invocation.getArguments().getFirst()).getValue();
        return new ArgumentPath(
            "/" + side + "/" + pointerEscape(String.valueOf(key)), String.valueOf(key));
      }
      ArgumentPath property =
          propertyInvocation(
              invocation,
              invocationPath,
              (rootPath, propertyRoot) ->
                  canonicalArgument(acId, rootPath, propertyRoot, canonicalMethod));
      if (property != null) {
        return property;
      }
      error(
          acId,
          parent,
          expression,
          "helper, SUT, constructor, and unrelated method calls are not allowed in Stage arguments",
          "Move that execution into a ToppleStage step; pass only literals or values rooted at"
              + " ToppleCase input/expected.");
      return null;
    }
    if (expression instanceof MemberSelectTree select) {
      TreePath selectPath = new TreePath(parent, select);
      Element member = trees.getElement(selectPath);
      if (member instanceof VariableElement field
          && field.getKind() == ElementKind.FIELD
          && !field.getModifiers().contains(Modifier.STATIC)) {
        ArgumentPath root =
            canonicalArgument(acId, selectPath, select.getExpression(), canonicalMethod);
        return root == null ? null : root.child(field.getSimpleName().toString());
      }
    }
    error(
        acId,
        parent,
        expression,
        "this argument expression is not part of the allowed case-data grammar",
        "Use a literal, ToppleCase input/expected accessor, or an allowed property path.");
    return null;
  }

  private ArgumentPath propertyInvocation(
      MethodInvocationTree invocation,
      TreePath parent,
      java.util.function.BiFunction<TreePath, ExpressionTree, ArgumentPath> rootResolver) {
    if (!(invocation.getMethodSelect() instanceof MemberSelectTree select)
        || !invocation.getArguments().isEmpty()) {
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

  private boolean isToppleCaseAccessor(
      ExecutableElement method, MethodInvocationTree invocation, TreePath parent) {
    if (!("input".contentEquals(method.getSimpleName())
            || "expected".contentEquals(method.getSimpleName()))
        || !isAssignable(method.getEnclosingElement().asType(), toppleCase)
        || invocation.getArguments().isEmpty()
        || !(invocation.getArguments().getFirst() instanceof LiteralTree literal)
        || !(literal.getValue() instanceof String)) {
      return false;
    }
    if (!(invocation.getMethodSelect() instanceof MemberSelectTree select)) {
      return false;
    }
    TreePath selectPath = new TreePath(parent, select);
    Element receiver = trees.getElement(new TreePath(selectPath, select.getExpression()));
    return receiver instanceof VariableElement variable
        && variable.getKind() == ElementKind.PARAMETER
        && isAssignable(variable.asType(), toppleCase);
  }

  private boolean isPropertyMethod(ExecutableElement method) {
    if (!method.getParameters().isEmpty() || method.getModifiers().contains(Modifier.STATIC)) {
      return false;
    }
    TypeElement owner = (TypeElement) method.getEnclosingElement();
    if (owner.getKind() == ElementKind.RECORD) {
      return owner.getRecordComponents().stream()
          .map(RecordComponentElement::getAccessor)
          .anyMatch(accessor -> accessor.equals(method));
    }
    String name = method.getSimpleName().toString();
    return (name.startsWith("get") && name.length() > 3)
        || (name.startsWith("is") && name.length() > 2);
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

  private TypeMirror safePropertyType(TypeMirror root, String name) {
    if (!(types.erasure(root) instanceof DeclaredType declared)
        || !(declared.asElement() instanceof TypeElement owner)) {
      return null;
    }
    for (Element member : elements.getAllMembers(owner)) {
      if (member instanceof VariableElement field
          && field.getKind() == ElementKind.FIELD
          && !field.getModifiers().contains(Modifier.STATIC)
          && field.getSimpleName().contentEquals(name)) {
        return field.asType();
      }
      if (member instanceof ExecutableElement method
          && !method.getModifiers().contains(Modifier.STATIC)
          && method.getParameters().isEmpty()
          && isPropertyMethod(method)
          && propertyName(method).equals(name)) {
        return method.getReturnType();
      }
    }
    return null;
  }

  private static void addLiteral(List<StepToken> tokens, String value) {
    if (!value.isEmpty()) {
      tokens.add(new StepToken(StepTokenKind.LITERAL, value));
    }
  }

  private boolean isAssignable(TypeMirror candidate, TypeMirror target) {
    return candidate != null
        && target != null
        && types.isAssignable(types.erasure(candidate), types.erasure(target));
  }

  private boolean sameErasure(TypeMirror left, TypeMirror right) {
    return left != null
        && right != null
        && types.isSameType(types.erasure(left), types.erasure(right));
  }

  private TypeMirror typeOf(String name) {
    TypeElement type = elements.getTypeElement(name);
    return type == null ? null : type.asType();
  }

  private String annotationValue(Element element, String annotationType, String member) {
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      if (!annotation.getAnnotationType().toString().equals(annotationType)) {
        continue;
      }
      for (Map.Entry<
              ? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue>
          entry : elements.getElementValuesWithDefaults(annotation).entrySet()) {
        if (member.contentEquals(entry.getKey().getSimpleName())) {
          Object value = entry.getValue().getValue();
          return value instanceof String string ? string : String.valueOf(value);
        }
      }
      return "";
    }
    return null;
  }

  private int annotationInt(ExecutableElement method, String member) {
    String value = annotationValue(method, ToppleProperty.class.getCanonicalName(), member);
    try {
      return Integer.parseInt(value);
    } catch (RuntimeException exception) {
      return Integer.MIN_VALUE;
    }
  }

  private boolean literalPropertyAcId(MethodTree method) {
    for (AnnotationTree annotation : method.getModifiers().getAnnotations()) {
      if (!annotation.getAnnotationType().toString().endsWith("ToppleProperty")) {
        continue;
      }
      if (annotation.getArguments().isEmpty()) {
        return false;
      }
      ExpressionTree value =
          annotation.getArguments().getFirst()
                  instanceof com.sun.source.tree.AssignmentTree assignment
              ? assignment.getExpression()
              : annotation.getArguments().getFirst();
      return value instanceof LiteralTree literal && literal.getValue() instanceof String;
    }
    return false;
  }

  private String title(ExecutableElement method) {
    String displayName = annotationValue(method, "org.junit.jupiter.api.DisplayName", "value");
    return displayName == null || displayName.isBlank()
        ? words(method.getSimpleName().toString())
        : displayName;
  }

  private static String words(String methodName) {
    return methodName.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').trim();
  }

  private SourceRef sourceRef(TreePath path, Tree tree) {
    long position = trees.getSourcePositions().getStartPosition(path.getCompilationUnit(), tree);
    long line = position < 0 ? 1 : path.getCompilationUnit().getLineMap().getLineNumber(position);
    long column =
        position < 0 ? 1 : path.getCompilationUnit().getLineMap().getColumnNumber(position);
    return new SourceRef(sourceName(path), Math.max(1, line), Math.max(1, column));
  }

  private String sourceDigest(TreePath path, Tree tree) {
    long start = trees.getSourcePositions().getStartPosition(path.getCompilationUnit(), tree);
    long end = trees.getSourcePositions().getEndPosition(path.getCompilationUnit(), tree);
    if (start < 0 || end < start) {
      return sha256("");
    }
    try {
      CharSequence source = path.getCompilationUnit().getSourceFile().getCharContent(true);
      if (end > source.length()) {
        return sha256("");
      }
      return sha256(source.subSequence((int) start, (int) end).toString());
    } catch (IOException | RuntimeException exception) {
      return sha256("");
    }
  }

  private void error(String acId, TreePath path, Tree tree, String rule, String repair) {
    SourceRef source = sourceRef(path, tree);
    String message =
        "AC "
            + acId
            + " at "
            + source.file()
            + ":"
            + source.line()
            + ":"
            + source.column()
            + " violates the @ToppleAcceptanceTest Stage DSL:\n"
            + rule
            + ". "
            + repair;
    trees.printMessage(Diagnostic.Kind.ERROR, message, tree, path.getCompilationUnit());
  }

  private void propertyError(String acId, TreePath path, Tree tree, String rule, String repair) {
    SourceRef source = sourceRef(path, tree);
    String message =
        "AC "
            + acId
            + " at "
            + source.file()
            + ":"
            + source.line()
            + ":"
            + source.column()
            + " violates the @ToppleProperty declaration:\n"
            + rule
            + ". "
            + repair;
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

  private String methodKey(ExecutableElement method) {
    return ((TypeElement) method.getEnclosingElement()).getQualifiedName()
        + "#"
        + method.getSimpleName()
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
      case DECLARED ->
          "L"
              + elements
                  .getBinaryName((TypeElement) ((DeclaredType) type).asElement())
                  .toString()
                  .replace('.', '/')
              + ";";
      default -> "L" + types.erasure(type).toString().replace('.', '/') + ";";
    };
  }

  private void writeDescriptor(CompilerScenarioDescriptor descriptor) {
    String name = sha256(descriptor.scenarioId()) + ".json";
    try {
      FileObject file =
          processingEnv
              .getFiler()
              .createResource(StandardLocation.CLASS_OUTPUT, "", DESCRIPTOR_DIRECTORY + name);
      try (Writer writer = file.openWriter()) {
        writer.write(json(descriptor));
      }
      descriptorFiles.add(name);
    } catch (IOException exception) {
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "ToppleCat could not write compiler descriptor: " + exception.getMessage());
    }
  }

  private void writePropertyDescriptor(CompilerPropertyDescriptor descriptor) {
    String name = sha256(descriptor.methodIdentity()) + ".json";
    try {
      FileObject file =
          processingEnv
              .getFiler()
              .createResource(
                  StandardLocation.CLASS_OUTPUT, "", PROPERTY_DESCRIPTOR_DIRECTORY + name);
      try (Writer writer = file.openWriter()) {
        writer.write(propertyJson(descriptor));
      }
      propertyDescriptorFiles.add(name);
    } catch (IOException exception) {
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "ToppleCat could not write Property compiler descriptor: " + exception.getMessage());
    }
  }

  private void writeIndex() {
    wroteIndex = true;
    writeIndex(DESCRIPTOR_DIRECTORY, INDEX, descriptorFiles, "compiler descriptor");
    writeIndex(
        PROPERTY_DESCRIPTOR_DIRECTORY,
        PROPERTY_INDEX,
        propertyDescriptorFiles,
        "Property compiler descriptor");
  }

  private void writeIndex(String directory, String indexName, Set<String> files, String kind) {
    if (files.isEmpty()) {
      return;
    }
    try {
      FileObject file =
          processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", indexName);
      try (Writer writer = file.openWriter()) {
        files.stream()
            .sorted()
            .forEach(
                name -> {
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
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              "ToppleCat could not write " + kind + " index: " + exception.getMessage());
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
    out.append("  \"scenarioParameterIndex\": ")
        .append(descriptor.scenarioParameterIndex())
        .append(",\n");
    out.append("  \"stageParameters\": [");
    for (int stageIndex = 0; stageIndex < descriptor.stageParameters().size(); stageIndex++) {
      ScenarioStage stage = descriptor.stageParameters().get(stageIndex);
      out.append("{\"parameterIndex\":")
          .append(stage.parameterIndex())
          .append(",\"stageBinaryName\":\"")
          .append(jsonEscape(stage.stageBinaryName()))
          .append("\"}");
      if (stageIndex + 1 < descriptor.stageParameters().size()) {
        out.append(',');
      }
    }
    out.append("],\n");
    out.append("  \"steps\": [\n");
    for (int stepIndex = 0; stepIndex < descriptor.steps().size(); stepIndex++) {
      StepTemplate step = descriptor.steps().get(stepIndex);
      out.append("    {\n");
      property(out, "stepId", step.stepId(), true, "      ");
      property(out, "phase", step.phase().name(), true, "      ");
      property(out, "stageBinaryName", step.stageBinaryName(), true, "      ");
      out.append("      \"tokens\": [");
      for (int tokenIndex = 0; tokenIndex < step.tokens().size(); tokenIndex++) {
        StepToken token = step.tokens().get(tokenIndex);
        out.append("{\"kind\":\"")
            .append(jsonEscape(token.kind().name()))
            .append("\",\"value\":\"")
            .append(jsonEscape(token.value()))
            .append("\"}");
        if (tokenIndex + 1 < step.tokens().size()) {
          out.append(',');
        }
      }
      out.append("],\n      \"argumentBindings\": [");
      for (int bindingIndex = 0; bindingIndex < step.argumentBindings().size(); bindingIndex++) {
        ArgumentBinding binding = step.argumentBindings().get(bindingIndex);
        out.append("{\"index\":")
            .append(binding.index())
            .append(",\"displayName\":\"")
            .append(jsonEscape(binding.displayName()))
            .append("\",\"jsonPointer\":\"")
            .append(jsonEscape(binding.jsonPointer()))
            .append("\"}");
        if (bindingIndex + 1 < step.argumentBindings().size()) {
          out.append(',');
        }
      }
      out.append("],\n      \"sourceRef\": ")
          .append(sourceJson(step.sourceRef()))
          .append("\n    }");
      if (stepIndex + 1 < descriptor.steps().size()) {
        out.append(',');
      }
      out.append('\n');
    }
    return out.append("  ]\n}\n").toString();
  }

  private static String propertyJson(CompilerPropertyDescriptor descriptor) {
    StringBuilder out = new StringBuilder("{\n");
    property(out, "schemaVersion", descriptor.schemaVersion(), true);
    property(out, "acId", descriptor.acId(), true);
    property(out, "title", descriptor.title(), true);
    property(out, "declaringBinaryName", descriptor.declaringBinaryName(), true);
    property(out, "methodName", descriptor.methodName(), true);
    property(out, "methodDescriptor", descriptor.methodDescriptor(), true);
    out.append("  \"sourceRef\": ").append(sourceJson(descriptor.sourceRef())).append(",\n");
    out.append("  \"tries\": ").append(descriptor.tries()).append(",\n");
    out.append("  \"maxDiscards\": ").append(descriptor.maxDiscards()).append(",\n");
    out.append("  \"maxShrinks\": ").append(descriptor.maxShrinks()).append(",\n");
    property(out, "sourceDigest", descriptor.sourceDigest(), false);
    return out.append("}\n").toString();
  }

  private static String sourceJson(SourceRef source) {
    return "{\"file\":\""
        + jsonEscape(source.file())
        + "\",\"line\":"
        + source.line()
        + ",\"column\":"
        + source.column()
        + "}";
  }

  private static void property(StringBuilder output, String key, String value, boolean comma) {
    property(output, key, value, comma, "  ");
  }

  private static void property(
      StringBuilder output, String key, String value, boolean comma, String prefix) {
    output
        .append(prefix)
        .append('"')
        .append(key)
        .append("\": \"")
        .append(jsonEscape(value))
        .append('"');
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
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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

  private record StageParameter(
      VariableElement parameter, TypeElement type, int index, String binaryName) {}

  private record ScenarioParameters(
      VariableElement scenarioParameter,
      List<StageParameter> stages,
      int scenarioParameterIndex,
      List<ScenarioStage> stageParameters) {
    private StageParameter stage(VariableElement parameter) {
      return stages.stream()
          .filter(candidate -> candidate.parameter().equals(parameter))
          .findFirst()
          .orElse(null);
    }
  }

  private record NewArguments(
      Map<String, ArgumentPath> incoming,
      Map<String, TypeMirror> parameterTypes,
      List<ArgumentBinding> bindings) {}

  private final class NewScenarioState {
    private StepPhase primary;

    private boolean accept(String acId, TreePath path, Tree tree, StepPhase phase) {
      if (phase == StepPhase.AND) {
        if (primary == null) {
          error(
              acId,
              path,
              tree,
              "scenario.and(...) requires a preceding Given, When, or Then Step",
              "Select given, when, or then before using and.");
          return false;
        }
        return true;
      }
      if (primary != null && phaseOrder(phase) < phaseOrder(primary)) {
        error(
            acId,
            path,
            tree,
            "Scenario primary phases cannot move backward",
            "Keep Given before When and Then; use and to continue the preceding phase.");
        return false;
      }
      primary = phase;
      return true;
    }

    private static int phaseOrder(StepPhase phase) {
      return switch (phase) {
        case GIVEN -> 0;
        case WHEN -> 1;
        case THEN -> 2;
        case AND -> throw new IllegalArgumentException("AND has no primary phase order");
      };
    }
  }

  private record ArgumentPath(String pointer, String displayName) {
    private ArgumentPath child(String name) {
      String next = pointer.isEmpty() ? "" : pointer + "/" + pointerEscape(name);
      return new ArgumentPath(next, name);
    }
  }

  private static final class IndexWriteException extends RuntimeException {
    private final IOException cause;

    private IndexWriteException(IOException cause) {
      this.cause = cause;
    }
  }
}
