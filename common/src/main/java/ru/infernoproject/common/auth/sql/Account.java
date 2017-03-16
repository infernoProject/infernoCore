package ru.infernoproject.common.auth.sql;

import ru.infernoproject.common.utils.HexBin;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

import java.math.BigInteger;

@SQLObject(database = "realmd", table = "accounts")
public class Account implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "level")
    public String accessLevel;

    @SQLField(column = "login")
    public String login;

    @SQLField(column = "email")
    public String email;

    @SQLField(column = "salt")
    public String salt;

    @SQLField(column = "verifier")
    public String verifier;

    public Account() {
        // Default constructor for SQLObjectWrapper
    }

    public Account(String login, String accessLevel, String email, byte[] salt, byte[] verifier) {
        this.login = login;
        this.accessLevel = accessLevel;
        this.email = email;
        this.salt = HexBin.encode(salt);
        this.verifier = HexBin.encode(verifier);
    }

    public int getId() {
        return id;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public String getLogin() {
        return login;
    }

    public byte[] getSalt() {
        return HexBin.decode(salt);
    }

    public byte[] getVerifier() {
        return HexBin.decode(verifier);
    }
}
