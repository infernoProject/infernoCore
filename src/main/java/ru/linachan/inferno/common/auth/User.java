package ru.linachan.inferno.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class User implements Serializable {

    private byte[] password;
    private String login;
    private Map<String, Object> attributes = new HashMap<>();
    private UUID id = UUID.randomUUID();

    public static final long serialVersionUID = 1L;

    private static MessageDigest sha256;
    private static Logger logger = LoggerFactory.getLogger(User.class);

    static {
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to initialize HashGenerator: {}", e.getMessage());
        }
    }

    public void setPassword(String userPassword) {
        password = sha256.digest(userPassword.getBytes());
    }

    public void setLogin(String userLogin) {
        login = userLogin;
    }

    public String getLogin() {
        return login;
    }

    public void setAttribute(String attribute, Object value) {
        attributes.put(attribute, value);
    }

    public Object getAttribute(String attribute, Object defaultValue) {
        return attributes.getOrDefault(attribute, defaultValue);
    }

    public UUID getId() {
        return id;
    }

    public boolean checkPassword(String userPassword) {
        return Arrays.equals(password, sha256.digest(userPassword.getBytes()));
    }
}
