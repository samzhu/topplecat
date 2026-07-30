package io.github.samzhu.topplecat.junit;

import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;

final class ToppleAnnotations {
  private ToppleAnnotations() {}

  static Optional<ToppleAcceptanceBinding> find(ExtensionContext context) {
    return context
        .getTestMethod()
        .flatMap(method -> find(method, context.getRequiredTestClass()))
        .or(() -> context.getTestClass().flatMap(ToppleAnnotations::find));
  }

  private static Optional<ToppleAcceptanceBinding> find(Method method, Class<?> testClass) {
    return find(method).or(() -> find(testClass));
  }

  private static Optional<ToppleAcceptanceBinding> find(
      java.lang.reflect.AnnotatedElement element) {
    ToppleAcceptanceTest test = element.getAnnotation(ToppleAcceptanceTest.class);
    if (test != null) {
      return Optional.of(new ToppleAcceptanceBinding(test.value()));
    }
    return Optional.empty();
  }
}
