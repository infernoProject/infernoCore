package ru.linachan.inferno.common.codec;

import ru.linachan.inferno.common.session.Session;

import java.nio.ByteBuffer;

public class Message {

    private Session session;
    private ByteBuffer data;

    public Message(Session userSession, ByteBuffer userData) {
        session = userSession;
        data = userData;
    }

    public ByteBuffer data() {
        return data;
    }

    public Session session() {
        return session;
    }

    @Override
    public String toString() {
        return String.format(
            "Message(Session(%s), Data(%s))",
            (session != null) ? session.getUser() : "NONE",
            (data != null) ? data.capacity() : "NONE"
        );
    }
}
