package ru.linachan.inferno.realm;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.inferno.InfernoServer;
import ru.linachan.inferno.common.auth.User;
import ru.linachan.inferno.common.codec.Message;
import ru.linachan.inferno.common.session.Session;

import java.nio.ByteBuffer;
import java.util.Base64;

@ChannelHandler.Sharable
public class RealmHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(RealmHandler.class);
    private static Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message request = (Message) msg;

        byte[] response = new byte[0];
        if (request.data().remaining() >= 2) {
            short opCode = request.data().getShort();
            switch (opCode) {
                case 0x01:
                    response = logIn(request.data());
                    break;
                case 0x02:
                    response = logOut(request.session());
                    break;
                case 0x03:
                    response = getWorldPort();
                    break;
            }
        }

        ctx.write(response);
    }

    private byte[] logIn(ByteBuffer logInData) {
        int loginLength = logInData.getInt();
        int passwordLength = logInData.getInt();

        byte[] loginBytes = new byte[loginLength];
        byte[] passwordBytes = new byte[passwordLength];

        logInData.get(loginBytes);
        logInData.get(passwordBytes);

        String login = new String(decoder.decode(loginBytes));
        String password = new String(decoder.decode(passwordBytes));

        ByteBuffer outputBuffer;

        User user = InfernoServer.INSTANCE
            .getAuthManager()
            .login(login, password);

        if (user != null) {
            Session session = InfernoServer.INSTANCE
                .getSessionManager()
                .createSession(user);

            outputBuffer = ByteBuffer.allocate(130);
            outputBuffer.putShort((short) 1);
            outputBuffer.put(session.getToken().getBytes());
        } else {
            outputBuffer = ByteBuffer.allocate(4);
            outputBuffer.putShort((short) 2);
        }

        return outputBuffer.array();
    }

    private byte[] logOut(Session session) {
        InfernoServer.INSTANCE
            .getSessionManager()
            .closeSession(session.getToken());

        return new byte[0];
    }

    private byte[] getWorldPort() {
        ByteBuffer worldPort = ByteBuffer.allocate(4);
        worldPort.putInt(41597);
        return worldPort.array();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unable to process request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }
}
