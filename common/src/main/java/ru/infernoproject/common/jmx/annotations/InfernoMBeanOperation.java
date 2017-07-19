package ru.infernoproject.common.jmx.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InfernoMBeanOperation {

    String description() default "";

    int impact() default 1;
}
