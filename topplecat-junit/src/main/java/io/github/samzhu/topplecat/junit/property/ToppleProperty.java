package io.github.samzhu.topplecat.junit.property;

import io.github.samzhu.topplecat.junit.ToppleJunit;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Supplementary deterministic invariant check bound literally to one existing acceptance condition.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Test
@ExtendWith(TopplePropertyExtension.class)
@Tag(ToppleJunit.PROPERTY_TAG)
public @interface ToppleProperty {
  String value();

  int tries() default 200;

  int maxDiscards() default 1_000;

  int maxShrinks() default 500;
}
