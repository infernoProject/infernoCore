package ru.linachan.inferno.common.codec.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import ru.linachan.inferno.InfernoServer;
import ru.linachan.inferno.common.codec.Message;
import ru.linachan.inferno.common.session.Session;
import ru.linachan.inferno.common.session.SessionToken;

import java.nio.ByteBuffer;
import java.util.List;

public class SessionDecoder extends MessageToMessageDecoder<byte[]> {

    @Override
    protected void decode(ChannelHandlerContext ctx, byte[] in, List<Object> out) throws Exception {
        if (in.length > 128) {
            ByteBuffer data = ByteBuffer.wrap(in);

            byte[] sessionToken = new byte[128];
            data.get(sessionToken);

            byte[] dataBytes = new byte[data.remaining()];
            data.get(dataBytes);

            Session session = InfernoServer.INSTANCE
                .getSessionManager()
                .getSession(new SessionToken(sessionToken));

            if (session != null)
                session.update();

            out.add(new Message(session, ByteBuffer.wrap(dataBytes)));
        }
    }
}
