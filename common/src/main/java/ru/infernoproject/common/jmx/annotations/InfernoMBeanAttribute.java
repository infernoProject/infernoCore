package ru.infernoproject.common.jmx.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InfernoMBeanAttribute {

    String description() default "";

    boolean isReadable() default true;
    boolean isWritable() default false;

}
