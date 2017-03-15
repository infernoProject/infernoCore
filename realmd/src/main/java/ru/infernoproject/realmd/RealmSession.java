package ru.infernoproject.realmd;

import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.server.ServerSession;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;

import java.net.SocketAddress;

public class RealmSession implements ServerSession {

    private final ChannelHandlerContext ctx;
    private final SocketAddress remoteAddress;

    private Account account;
    private boolean authorized = false;

    public RealmSession(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
        this.ctx = ctx;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    @Override
    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public void write(byte opCode, ByteConvertible data) {
        ctx.writeAndFlush(new ByteArray(opCode).put(data).toByteArray());
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
    public SocketAddress address() {
        return remoteAddress;
    }

    @Override
    public ChannelHandlerContext context() {
        return ctx;
    }
}
