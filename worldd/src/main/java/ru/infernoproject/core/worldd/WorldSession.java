package ru.infernoproject.core.worldd;

import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.core.common.net.server.ServerSession;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;

import java.net.SocketAddress;

public class WorldSession implements ServerSession {

    private Account account;

    private boolean authorized = false;

    private final ChannelHandlerContext ctx;
    private final SocketAddress remoteAddress;

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
    public void write(ByteConvertible data) {
        ctx.writeAndFlush(data.toByteArray());
    }

    @Override
    public SocketAddress address() {
        return remoteAddress;
    }

    @Override
    public ChannelHandlerContext context() {
        return ctx;
    }
}
