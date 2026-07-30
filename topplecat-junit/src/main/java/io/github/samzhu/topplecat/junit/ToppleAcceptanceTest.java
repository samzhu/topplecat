package io.github.samzhu.topplecat.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Parameterized JUnit test that executes configured ToppleCat cases for one acceptance condition.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterizedTest(name = "{0}")
@ArgumentsSource(ToppleCaseSourceProvider.class)
@ExtendWith(ToppleAcceptanceExtension.class)
@Tag(ToppleJunit.CONTRACT_TAG)
public @interface ToppleAcceptanceTest {
  /** Stable acceptance-condition identifier. */
  String value();
}
