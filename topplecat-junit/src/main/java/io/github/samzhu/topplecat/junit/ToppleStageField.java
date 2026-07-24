package io.github.samzhu.topplecat.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Requests a fresh {@link ToppleStage} instance for each ToppleCat case invocation. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ToppleStageField {
}
