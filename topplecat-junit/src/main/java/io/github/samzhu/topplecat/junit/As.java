package io.github.samzhu.topplecat.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Overrides the report sentence for one {@link ToppleStage} step method. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface As {
    /** Zero-based placeholders such as {@code {0}} insert recorded arguments. */
    String value();
}
