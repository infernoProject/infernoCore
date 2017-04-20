package ru.infernoproject.worldd.world;

import ru.infernoproject.common.utils.ByteConvertible;

public interface WorldNotificationListener {

    void onEvent(byte type, ByteConvertible data);

}
