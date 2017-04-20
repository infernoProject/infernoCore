package ru.infernoproject.worldd;

import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.player.WorldPlayer;

import java.net.SocketAddress;

import static ru.infernoproject.worldd.constants.WorldOperations.EVENT;

public class WorldSession implements ServerSession, WorldNotificationListener {

    private Account account;

    private boolean authorized = false;

    private final ChannelHandlerContext ctx;
    private final SocketAddress remoteAddress;

    private WorldPlayer player;

    public WorldSession(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
        this.ctx = ctx;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    @Override
    public void write(byte opCode, ByteConvertible data) {
        ctx.writeAndFlush(new ByteArray(opCode).put(data));
    }

    @Override
    public SocketAddress address() {
        return remoteAddress;
    }

    @Override
    public ChannelHandlerContext context() {
        return ctx;
    }

    public WorldPlayer getPlayer() {
        return player;
    }

    public void setPlayer(WorldPlayer player) {
        this.player = player;
    }

    @Override
    public void onEvent(byte type, ByteConvertible data) {
        write(EVENT, new ByteArray(type).put(data));
    }
}
