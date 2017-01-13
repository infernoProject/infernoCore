package ru.infernoproject.core.common.net.server;

import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;

import java.net.SocketAddress;

public interface ServerSession {

    void setAccount(Account account);
    Account getAccount();

    SocketAddress address();
    ChannelHandlerContext context();

    void setAuthorized(boolean authorized);
    boolean isAuthorized();

    void write(ByteConvertible data);
}
