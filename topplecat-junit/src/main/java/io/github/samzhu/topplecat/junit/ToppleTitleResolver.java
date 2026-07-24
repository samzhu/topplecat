package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;

/** Resolves a human title without introducing a second specification language. */
public final class ToppleTitleResolver {
    private ToppleTitleResolver() {
    }

    public static String title(Method method) {
        DisplayName displayName = method.getAnnotation(DisplayName.class);
        if (displayName != null && !displayName.value().isBlank()) {
            return displayName.value();
        }
        ToppleAc ac = method.getAnnotation(ToppleAc.class);
        if (ac != null && !ac.title().isBlank()) {
            return ac.title();
        }
        return words(method.getName());
    }

    static String words(String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .trim();
    }
}
