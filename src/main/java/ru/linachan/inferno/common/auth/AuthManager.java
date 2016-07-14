package ru.linachan.inferno.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AuthManager {

    private static Logger logger = LoggerFactory.getLogger(AuthManager.class);

    public AuthManager() {
    }

    public User register(String login, String password) throws IOException {
        User user = new User();

        user.setLogin(login);
        user.setPassword(password);
        user.setAttribute("level", "user");

        return user;
    }

    public User login(String login, String password) {
        return null;
    }

    public void update(User user) throws IOException {

    }

    public void shutdown() {

    }
}
