package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.ArgumentBinding;
import io.github.samzhu.topplecat.core.ScenarioTemplateRenderer;
import io.github.samzhu.topplecat.core.StepTemplate;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** Resolves compiler-owned argument bindings against one typed case without reading Java source. */
final class ReviewScenarioResolver {
  private ReviewScenarioResolver() {}

  static List<ReviewScenarioStep> resolve(
      List<StepTemplate> templates, JsonNode inputs, JsonNode expected) {
    ObjectNode root = new ObjectNode(new JsonNodeFactory());
    root.set("inputs", inputs);
    root.set("expected", expected);
    return templates.stream()
        .map(
            template ->
                new ReviewScenarioStep(
                    template.phase(),
                    ScenarioTemplateRenderer.renderSentence(
                        template,
                        template.argumentBindings().stream()
                            .map(binding -> display(root, binding))
                            .toList())))
        .toList();
  }

  private static String display(JsonNode root, ArgumentBinding binding) {
    if (binding.jsonPointer().isBlank()) {
      return placeholder(binding);
    }
    JsonNode value = root.at(binding.jsonPointer());
    if (value.isMissingNode()) {
      return placeholder(binding);
    }
    if (value.isString()) {
      return value.stringValue();
    }
    if (value.isNumber() || value.isBoolean() || value.isNull()) {
      return value.toString();
    }
    return value.toString();
  }

  private static String placeholder(ArgumentBinding binding) {
    return "<" + binding.displayName() + ">";
  }
}
