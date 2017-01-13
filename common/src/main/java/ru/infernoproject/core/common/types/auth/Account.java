package ru.infernoproject.core.common.types.auth;

public class Account {

    private final int accountId;
    private final int accessLevel;
    private final String login;

    public Account(int accountId, int accessLevel, String login) {
        this.accountId = accountId;
        this.accessLevel = accessLevel;
        this.login = login;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public String getLogin() {
        return login;
    }
}
