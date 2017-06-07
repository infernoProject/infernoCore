package ru.infernoproject.common.auth.sql;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;

import java.time.LocalDateTime;

@SQLObject(database = "realmd", table = "account_ban")
public class AccountBan implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "account")
    public Account account;

    @SQLField(column = "expires")
    public LocalDateTime expires;

    @SQLField(column = "reason")
    public String reason;
}
