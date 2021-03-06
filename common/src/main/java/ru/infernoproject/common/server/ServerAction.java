package ru.infernoproject.common.server;

import ru.infernoproject.common.auth.sql.AccountLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServerAction {

    byte[] opCode();
    boolean authRequired() default false;
    AccountLevel minLevel() default AccountLevel.USER;
}
