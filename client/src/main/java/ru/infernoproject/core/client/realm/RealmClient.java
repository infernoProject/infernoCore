package ru.infernoproject.core.client.realm;

import com.nimbusds.srp6.*;
import io.netty.channel.ChannelHandlerContext;
import ru.infernoproject.core.client.common.BasicClient;
import ru.infernoproject.core.client.world.WorldClient;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.utils.*;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.RealmOperations.*;

public class RealmClient extends BasicClient {

    private SRP6CryptoParams cryptoParams = null;
    private SRP6ClientSession session;

    private RealmServerInfo server;

    private final String LOG_IN_CALLBACK = "logIn";
    private final String SIGN_UP_CALLBACK = "signUp";
    private final String SESSION_CALLBACK = "session";
    private final String REALM_LIST_CALLBACK = "realmList";

    public RealmClient(String host, int port) {
        super(host, port);
    }

    public void srp6ConfigGet() throws InterruptedException {
        send(SRP6_CONFIG);

        while (cryptoParams == null) {
            Thread.sleep(100);
        }
    }

    private void setSRP6Config(ByteWrapper config) {
        BigInteger N = config.getBigInteger();
        BigInteger g = config.getBigInteger();
        String H = config.getString();

        cryptoParams = new SRP6CryptoParams(N, g, H);
    }

    public void signUp(String login, String password, Callback callBack) throws InterruptedException {
        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(cryptoParams);

        BigInteger salt = new BigInteger(SRP6VerifierGenerator.generateRandomSalt(16));
        BigInteger verifier = verifierGenerator.generateVerifier(salt, login, password);

        registerCallBack(SIGN_UP_CALLBACK, callBack);
        send(new ByteArray()
            .put(SIGN_UP).put(login)
            .put(salt).put(verifier)
        );
    }

    private void signUpCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                getCallBack(SIGN_UP_CALLBACK).callBack(
                    Result.success()
                );
                break;
            case AUTH_ERROR:
                getCallBack(SIGN_UP_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Already registered")
                );
                break;
            case SQL_ERROR:
                getCallBack(SIGN_UP_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Server failure")
                );
                break;
        }
    }

    public void logIn(String login, String password, Callback callBack) {
        session = new SRP6ClientSession();
        session.step1(login, password);

        registerCallBack(LOG_IN_CALLBACK, callBack);
        send(new ByteArray()
            .put(LOG_IN_STEP1).put(login)
        );
    }

    private void logInStep2(ByteWrapper challenge) {
        switch (challenge.getByte()) {
            case SUCCESS:
                BigInteger B = challenge.getBigInteger();
                BigInteger salt = challenge.getBigInteger();

                try {
                    SRP6ClientCredentials credentials = session.step2(cryptoParams, B, salt);

                    send(new ByteArray()
                        .put(LOG_IN_STEP2).put(credentials.A).put(credentials.M1)
                    );
                } catch (SRP6Exception e) {
                    getCallBack(LOG_IN_CALLBACK).callBack(
                        Result.failed()
                            .attr("message", "Invalid auth token")
                    );
                }
                break;
            case AUTH_ERROR:
                getCallBack(LOG_IN_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Invalid login")
                );
                break;
            case AUTH_INVALID:
                getCallBack(LOG_IN_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Invalid authorization order")
                );
                break;
            case SQL_ERROR:
                getCallBack(LOG_IN_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Server failure")
                );
                break;
        }
    }

    private void logInStep3(ByteWrapper proof) {
        switch (proof.getByte()) {
            case SUCCESS:
                BigInteger M2 = proof.getBigInteger();

                try {
                    session.step3(M2);

                    getCallBack(LOG_IN_CALLBACK).callBack(Result.success());
                } catch (SRP6Exception e) {
                    getCallBack(LOG_IN_CALLBACK).callBack(
                        Result.failed()
                            .attr("message", "Server verification failed")
                    );
                }
                break;
            case AUTH_ERROR:
                getCallBack(LOG_IN_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Invalid password")
                );
                break;
            case AUTH_REQUIRED:
                getCallBack(LOG_IN_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Invalid authorization order")
                );
                break;
            case SQL_ERROR:
                getCallBack(LOG_IN_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Server failure")
                );
                break;
        }
    }

    public void sessionTokenGet(Callback callback) {
        registerCallBack(SESSION_CALLBACK, callback);
        send(SESSION_TOKEN);
    }

    private void setSessionToken(ByteWrapper sessionData) {
        switch (sessionData.getByte()) {
            case SUCCESS:
                byte[] sessionToken = sessionData.getBytes();

                getCallBack(SESSION_CALLBACK).callBack(
                    Result.success().attr("sessionToken", sessionToken)
                );
                break;
            case AUTH_REQUIRED:
                getCallBack(SESSION_CALLBACK).callBack(
                    Result.failed().attr("message", "Authorization required")
                );
                break;
        }
    }

    public void realmListGet(Callback callback) {
        registerCallBack(REALM_LIST_CALLBACK, callback);
        send(REALM_LIST);
    }

    private void realmListSet(ByteWrapper realmListData) {
        switch (realmListData.getByte()) {
            case SUCCESS:
                List<RealmServerInfo> realmList = realmListData.getList().stream()
                    .map(RealmServerInfo::new)
                    .collect(Collectors.toList());

                getCallBack(REALM_LIST_CALLBACK).callBack(
                    Result.success().attr("realmList", realmList)
                );
                break;
            case AUTH_REQUIRED:
                getCallBack(REALM_LIST_CALLBACK).callBack(
                    Result.failed().attr("message", "Authorization required")
                );
                break;
            case SQL_ERROR:
                getCallBack(REALM_LIST_CALLBACK).callBack(
                    Result.failed().attr("message", "Server failure")
                );
                break;
        }
    }

    public void serverSelect(RealmServerInfo server) {
        this.server = server;
    }

    public WorldClient serverConnect() {
        return (server != null) ? new WorldClient(server) : null;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteWrapper in) throws Exception {
        logger.debug("IN: {}", in);
        switch (in.getByte()) {
            case SRP6_CONFIG:
                setSRP6Config(in.getWrapper());
                break;
            case SIGN_UP:
                signUpCallBack(in.getWrapper());
                break;
            case LOG_IN_STEP1:
                logInStep2(in.getWrapper());
                break;
            case LOG_IN_STEP2:
                logInStep3(in.getWrapper());
                break;
            case REALM_LIST:
                realmListSet(in.getWrapper());
                break;
            case SESSION_TOKEN:
                setSessionToken(in.getWrapper());
                break;
        }
    }
}
