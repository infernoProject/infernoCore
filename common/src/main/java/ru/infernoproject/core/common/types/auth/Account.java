package ru.infernoproject.core.common.types.auth;

import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;

import ru.infernoproject.core.common.db.sql.SQLField;
import ru.infernoproject.core.common.db.sql.SQLObject;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;

import java.math.BigInteger;

@SQLObject(database = "realmd", table = "accounts")
public class Account implements SQLObjectWrapper {

    @SQLField(column = "id", type = Integer.class)
    public int accountId;

    @SQLField(column = "level", type = String.class)
    public String accessLevel;

    @SQLField(column = "login", type = String.class)
    public String login;

    @SQLField(column = "email", type = String.class)
    public String email;

    @SQLField(column = "salt", type = String.class)
    public String salt;

    @SQLField(column = "verifier", type = String.class)
    public String verifier;

    public Account() {

    }

    public Account(String login, String accessLevel, String email, BigInteger salt, BigInteger verifier) {
        this.login = login;
        this.accessLevel = accessLevel;
        this.email = email;
        this.salt = HexBin.encode(salt.toByteArray());
        this.verifier = HexBin.encode(verifier.toByteArray());
    }

    public int getAccountId() {
        return accountId;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public String getLogin() {
        return login;
    }

    public BigInteger getSalt() {
        return new BigInteger(HexBin.decode(salt));
    }

    public BigInteger getVerifier() {
        return new BigInteger(HexBin.decode(verifier));
    }
}
