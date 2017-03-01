package ru.infernoproject.common.server;

import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.utils.ByteConvertible;

import java.net.SocketAddress;

public interface ServerSession {

    void setAccount(Account account);
    Account getAccount();

    SocketAddress address();
    ChannelHandlerContext context();

    void setAuthorized(boolean authorized);
    boolean isAuthorized();

    void write(byte opCode, ByteConvertible data);
}
