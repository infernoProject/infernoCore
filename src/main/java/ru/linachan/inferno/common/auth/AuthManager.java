package ru.linachan.inferno.common.auth;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.inferno.InfernoServer;

import java.io.IOException;

public class AuthManager {

    private static Logger logger = LoggerFactory.getLogger(AuthManager.class);
    private MongoCollection<Document> users;

    public AuthManager() {
        users = InfernoServer.DB.getCollection("users");
    }

    private Document getUser(String login) {
        return users.find(new Document("login", login)).first();
    }

    public User register(String login, String password) throws IOException {
        Document userData = getUser(login);
        if (userData == null) {
            User user = new User();

            user.setLogin(login);
            user.setPassword(password);
            user.setAttribute("level", "user");

            users.insertOne(user.toBSON());

            return user;
        }
        return null;
    }

    public User login(String login, String password) {
        Document userData = getUser(login);
        if (userData != null) {
            User user = User.fromBSON(userData);
            if (user.checkPassword(password)) {
                return user;
            }
        }

        return null;
    }

    public void update(User user) throws IOException {
        users.updateOne(
            new Document("login", user.getLogin()),
            new Document("$set", user.toBSON())
        );
    }
}
