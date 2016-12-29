package ru.infernoproject.core.realmd;

import com.nimbusds.srp6.SRP6ServerSession;

public class RealmSession {

    private SRP6ServerSession srp6Session;
    private boolean authorized = false;
    private Integer accountID;

    public void setSRP6Session(SRP6ServerSession srp6Session) {
        this.srp6Session = srp6Session;
    }

    public SRP6ServerSession getSRP6Session() {
        return srp6Session;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAccountID(Integer accountID) {
        this.accountID = accountID;
    }

    public Integer getAccountID() {
        return accountID;
    }
}
