package ru.linachan.inferno.common.auth;

import org.bson.Document;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class User {

    private byte[] password;
    private String login;
    private Map<String, Object> attributes = new HashMap<>();
    private UUID id = UUID.randomUUID();

    private static MessageDigest sha256;
    private static Logger logger = LoggerFactory.getLogger(User.class);

    static {
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to initialize HashGenerator: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static User fromBSON(Document userData) {
        User user = new User();

        user.id = UUID.fromString(userData.getString("uuid"));
        user.login = userData.getString("login");
        user.password = ((Binary) userData.get("password")).getData();
        user.attributes = (Map<String, Object>) userData.get("attriubutes");

        return user;
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

    public Document toBSON() {
        return new Document("login", login)
            .append("password", password)
            .append("uuid", id.toString())
            .append("attributes", attributes);
    }
}
