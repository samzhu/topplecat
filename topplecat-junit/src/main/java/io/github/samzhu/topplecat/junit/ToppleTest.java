package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Parameterized JUnit test that executes configured ToppleCat cases for one acceptance condition. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterizedTest(name = "{0}")
@ArgumentsSource(ToppleCaseSourceProvider.class)
@ExtendWith(ToppleAcExtension.class)
public @interface ToppleTest {
    /** Stable acceptance-condition identifier. */
    String value();
}
