package io.github.samzhu.topplecat.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a stage field populated from state provided by an earlier stage. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExpectedState {
    /** Makes a missing compatible provided value an authoring error. */
    boolean required() default false;
}
