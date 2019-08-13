package ru.infernoproject.worldd.world;

import ru.infernoproject.common.auth.sql.AccountLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InternalCommand {

    String command();
    AccountLevel level() default AccountLevel.USER;

}
