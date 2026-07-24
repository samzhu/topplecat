package io.github.samzhu.topplecat.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a stage field whose value is available to later stages. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ProvidedState {
}
