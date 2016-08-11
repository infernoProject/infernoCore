package ru.linachan.inferno.common.session;

import org.bson.Document;
import org.bson.types.Binary;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private SessionToken token;
    private String user;
    private InetSocketAddress address;

    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    private Long startTime;
    private Long updateTime;

    public Session(SessionToken sessionToken, String sessionUser) {
        token = sessionToken;
        user = sessionUser;

        startTime = updateTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    public static Session fromBSON(Document sessionData) {
        Session session = new Session(
            new SessionToken(((Binary) sessionData.get("token")).getData()),
            (String) sessionData.get("user")
        );

        session.attributes = (Map<String, Object>) sessionData.get("attributes");
        session.startTime = sessionData.getLong("start_time");
        session.updateTime = sessionData.getLong("update_time");

        String address[] = sessionData.getString("update_time").split(":");
        session.address = new InetSocketAddress(address[0], Integer.parseInt(address[1]));

        return session;
    }

    public void update() {
        updateTime = System.currentTimeMillis();
    }

    public SessionToken getToken() {
        return token;
    }

    public String getUser() {
        return user;
    }

    public Object getAttribute(String attribute, Object defaultValue) {
        return attributes.getOrDefault(attribute, defaultValue);
    }

    public void setAttribute(String attribute, Object value) {
        attributes.put(attribute, value);
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public Long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    public void onClose() {}

    public Document toBSON() {
        return new Document("user", user)
            .append("token", token.getBytes())
            .append("attributes", attributes)
            .append("start_time", startTime)
            .append("update_time", updateTime)
            .append("address", String.format("%s:%d", address.getHostName(), address.getPort()));
    }

    public void setRemoteAddress(SocketAddress remoteAddress) {
        address = (InetSocketAddress) remoteAddress;
    }
}
