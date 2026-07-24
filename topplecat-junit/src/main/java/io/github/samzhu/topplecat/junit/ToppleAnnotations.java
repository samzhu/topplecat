package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ToppleCatException;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

final class ToppleAnnotations {
    private ToppleAnnotations() {
    }

    static Optional<ToppleAcBinding> find(ExtensionContext context) {
        return context.getTestMethod().flatMap(method -> find(method, context.getRequiredTestClass()))
                .or(() -> context.getTestClass().flatMap(ToppleAnnotations::find));
    }

    private static Optional<ToppleAcBinding> find(Method method, Class<?> testClass) {
        return find(method).or(() -> find(testClass));
    }

    private static Optional<ToppleAcBinding> find(AnnotatedElement element) {
        ToppleAc ac = element.getAnnotation(ToppleAc.class);
        ToppleTest test = element.getAnnotation(ToppleTest.class);
        if (ac != null && test != null) {
            throw new ToppleCatException("A test cannot declare both @ToppleAc and @ToppleTest. Use one binding annotation.");
        }
        if (test != null) {
            return Optional.of(new ToppleAcBinding(test.value(), ""));
        }
        if (ac != null) {
            return Optional.of(new ToppleAcBinding(ac.value(), ac.title()));
        }
        return Optional.empty();
    }
}
