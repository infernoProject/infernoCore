package ru.infernoproject.core.worldd;

import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.core.common.net.server.ServerSession;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.worldd.world.WorldEvent;
import ru.infernoproject.core.worldd.world.WorldNotificationListener;
import ru.infernoproject.core.worldd.world.player.WorldPlayer;

import java.net.SocketAddress;

import static ru.infernoproject.core.common.constants.WorldOperations.EVENT;

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
        ctx.writeAndFlush(
            new ByteArray().put(opCode).put(data)
                .toByteArray()
        );
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
    public void onEvent(byte type, int quantifier, int duration, int healthCurrent, int healthMax) {
        write(EVENT, new ByteArray()
            .put(type).put(quantifier).put(duration)
            .put(healthCurrent).put(healthMax)
        );
    }
}
