package io.github.samzhu.topplecat.junit;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;

/** Resolves a human title without introducing a second specification language. */
public final class ToppleTitleResolver {
  private ToppleTitleResolver() {}

  public static String title(Method method) {
    DisplayName displayName = method.getAnnotation(DisplayName.class);
    if (displayName != null && !displayName.value().isBlank()) {
      return displayName.value();
    }
    return words(method.getName());
  }

  static String words(String methodName) {
    return methodName.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').trim();
  }
}
