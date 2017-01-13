package ru.infernoproject.core.common.net.client;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ClientCallBack {
    byte opCode();
}
