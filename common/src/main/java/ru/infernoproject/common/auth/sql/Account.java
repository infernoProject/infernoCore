package ru.infernoproject.common.auth.sql;

import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

@SQLObject(database = "realmd", table = "accounts")
public class Account implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "level")
    public AccountLevel accessLevel;

    @SQLField(column = "login")
    public String login;

    @SQLField(column = "email")
    public String email;

    @SQLField(column = "salt")
    public byte[] salt;

    @SQLField(column = "verifier")
    public byte[] verifier;

    public Account() {
        // Default constructor for SQLObjectWrapper
    }

    public Account(String login, AccountLevel accessLevel, String email, byte[] salt, byte[] verifier) {
        this.login = login;
        this.accessLevel = accessLevel;
        this.email = email;
        this.salt = salt;
        this.verifier = verifier;
    }

    public int getId() {
        return id;
    }

    public AccountLevel getAccessLevel() {
        return accessLevel;
    }

    public String getLogin() {
        return login;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getVerifier() {
        return verifier;
    }
}
