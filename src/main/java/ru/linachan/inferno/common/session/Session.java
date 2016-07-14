package ru.linachan.inferno.common.session;

import ru.linachan.mmo.auth.User;
import ru.linachan.mmo.world.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private SessionToken token;
    private User user;

    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    private Long startTime;
    private Long updateTime;
    private Player player;

    public Session(SessionToken sessionToken, User sessionUser) {
        token = sessionToken;
        user = sessionUser;

        startTime = updateTime = System.currentTimeMillis();
    }

    public void update() {
        updateTime = System.currentTimeMillis();
    }

    public SessionToken getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public Object getAttribute(String attribute, Object defautlValue) {
        return attributes.getOrDefault(attribute, defautlValue);
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

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
