package ru.infernoproject.core.common.net.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ServerAction {

    byte opCode();
}
