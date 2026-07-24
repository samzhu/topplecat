package io.github.samzhu.topplecat.junit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Binds a non-parameterized JUnit test to one acceptance condition. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(ToppleAcExtension.class)
public @interface ToppleAc {
    /** Stable acceptance-condition identifier. */
    String value();

    /** Human title used only when the test has no {@code @DisplayName}. */
    String title() default "";
}
