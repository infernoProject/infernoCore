package ru.linachan.inferno.common.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.mmo.auth.User;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SessionManager {

    private static Random randomGenerator = new Random();
    private static Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private List<Session> sessions = new CopyOnWriteArrayList<>();

    public Session createSession(User user) {
        byte[] token = new byte[128];
        randomGenerator.nextBytes(token);

        Session userSession = new Session(new SessionToken(token), user);
        logger.info("SESSION({}): Session initialized", user.getLogin());

        sessions.stream()
            .filter(session -> session.getUser().getLogin().equals(user.getLogin()))
            .collect(Collectors.toList()).stream()
            .forEach(session -> closeSession(session.getToken()));

        sessions.add(userSession);

        return userSession;
    }

    public void closeSession(SessionToken sessionToken) {
        sessions.stream()
            .filter(session -> session.getToken().equals(sessionToken))
            .collect(Collectors.toList()).stream()
            .forEach(session -> {
                logger.info("SESSION({}): Session closed ({}s)", session.getUser().getLogin(), session.getDuration() / 1000.0);
                session.onClose();
                sessions.remove(session);
            });
    }

    public void killSession(Session session) {
        logger.info("SESSION({}): Session expired ({}s)", session.getUser().getLogin(), session.getDuration() / 1000.0);
        sessions.remove(session);
    }

    public Session getSession(SessionToken token) {
        final Session[] userSession = { null };

        sessions.stream()
            .filter(session -> session.getToken().equals(token))
            .forEach(session -> userSession[0] = session);

        return userSession[0];
    }

    public List<Session> listSessions() {
        return sessions;
    }
}
