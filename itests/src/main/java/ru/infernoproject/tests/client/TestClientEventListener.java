package ru.infernoproject.tests.client;

import ru.infernoproject.common.utils.ByteWrapper;

public interface TestClientEventListener {

    void onEvent(ByteWrapper event);
}
