package ru.linachan.inferno.common.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.yggdrasil.YggdrasilCore;
import ru.linachan.yggdrasil.storage.YggdrasilStorageFile;

import java.io.File;
import java.io.IOException;

public class AuthManager {

    private YggdrasilStorageFile userStorage;

    private static Logger logger = LoggerFactory.getLogger(AuthManager.class);

    public AuthManager() {
        try {
            userStorage = YggdrasilCore.INSTANCE.getStorage()
                    .createStorage("mmoServerStorage", new File("mmoServer.dat"), "MMO".getBytes(), false);
        } catch (IOException e) {
            logger.error("Unable to initialize MMOServer Credentials Storage: {}", e.getMessage());
        }
    }

    public User register(String login, String password) throws IOException {
        User user = new User();

        user.setLogin(login);
        user.setPassword(password);
        user.setAttribute("level", "user");

        userStorage.putObject(login, user);
        return user;
    }

    public User login(String login, String password) throws IOException, ClassNotFoundException {
        if (userStorage.hasKey(login)) {
            User user = userStorage.getObject(login, User.class);
            if (user.checkPassword(password)) {
                return user;
            }
        }

        return null;
    }

    public void update(User user) throws IOException {
        userStorage.putObject(user.getLogin(), user);
    }

    public void shutdown() {
        try {
            userStorage.writeStorage();
        } catch (InterruptedException | IOException e) {
            logger.error("Unable to save MMOServer Credential Data: {}", e.getMessage());
        }
    }
}
