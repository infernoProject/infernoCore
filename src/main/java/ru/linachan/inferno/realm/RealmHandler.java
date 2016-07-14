package ru.linachan.inferno.realm;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.mmo.MMOPlugin;
import ru.linachan.mmo.auth.User;
import ru.linachan.mmo.codec.Message;
import ru.linachan.mmo.session.Session;
import ru.linachan.yggdrasil.YggdrasilCore;
import ru.linachan.yggdrasil.plugin.YggdrasilPluginManager;

import java.io.IOException;
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

        try {
            User user = YggdrasilCore.INSTANCE
                .getManager(YggdrasilPluginManager.class)
                .get(MMOPlugin.class)
                .getAuthManager()
                .login(login, password);

            if (user != null) {
                Session session = YggdrasilCore.INSTANCE
                    .getManager(YggdrasilPluginManager.class)
                    .get(MMOPlugin.class)
                    .getSessionManager()
                    .createSession(user);

                outputBuffer = ByteBuffer.allocate(130);
                outputBuffer.putShort((short) 1);
                outputBuffer.put(session.getToken().getBytes());
            } else {
                outputBuffer = ByteBuffer.allocate(4);
                outputBuffer.putShort((short) 2);
            }
        } catch (ClassNotFoundException | IOException e) {
            logger.error("Unable to authenticate user: {}", e.getMessage());

            outputBuffer = ByteBuffer.allocate(4);
            outputBuffer.putShort((short) 3);
        }

        return outputBuffer.array();
    }

    private byte[] logOut(Session session) {
        YggdrasilCore.INSTANCE
            .getManager(YggdrasilPluginManager.class)
            .get(MMOPlugin.class)
            .getSessionManager()
            .closeSession(session.getToken());

        return new byte[0];
    }

    private byte[] getWorldPort() {
        ByteBuffer worldPort = ByteBuffer.allocate(4);
        worldPort.putInt(YggdrasilCore.INSTANCE.getConfig().getInt("mmo.world.port", 41597));
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
