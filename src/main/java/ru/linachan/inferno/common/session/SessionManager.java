package ru.linachan.inferno.common.session;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.inferno.InfernoServer;
import ru.linachan.inferno.common.auth.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SessionManager {

    private static Random randomGenerator = new Random();
    private static Logger logger = LoggerFactory.getLogger(SessionManager.class);

    private MongoCollection<Document> sessions = InfernoServer.DB.getCollection("sessions");

    public Session createSession(User user) {
        for (Document sessionData: sessions.find(new Document("user", user.getLogin()))) {
            Session session = Session.fromBSON(sessionData);
            logger.info("SESSION({}): Session closed ({}s)", session.getUser(), session.getDuration() / 1000.0);
            session.onClose();
            sessions.deleteOne(sessionData);
        }

        byte[] token = new byte[128];
        randomGenerator.nextBytes(token);

        Session userSession = new Session(new SessionToken(token), user.getLogin());
        logger.info("SESSION({}): Session initialized", user.getLogin());

        sessions.insertOne(userSession.toBSON());

        return userSession;
    }

    public void closeSession(SessionToken sessionToken) {
        Document sessionData = sessions.find(new Document("token", sessionToken.getBytes())).first();
        if (sessionData != null) {
            Session session = Session.fromBSON(sessionData);
            logger.info("SESSION({}): Session closed ({}s)", session.getUser(), session.getDuration() / 1000.0);
            session.onClose();

            sessions.deleteOne(session.toBSON());
        }
    }

    public void killSession(Session session) {
        logger.info("SESSION({}): Session expired ({}s)", session.getUser(), session.getDuration() / 1000.0);
        session.onClose();

        sessions.deleteOne(session.toBSON());
    }

    public Session getSession(SessionToken token) {
        Document sessionData = sessions.find(new Document("token", token.getBytes())).first();
        if (sessionData != null) {
            Session session = Session.fromBSON(sessionData);
            return session;
        }
        return null;
    }

    public void updateSession(Session session) {
        session.update();
        sessions.updateOne(
            new Document("token", session.getToken().getBytes()),
            new Document("$set", session.toBSON())
        );
    }

    public List<Session> listSessions() {
        List<Session> sessionList = new ArrayList<>();
        for (Document sessionData: sessions.find()) {
            sessionList.add(Session.fromBSON(sessionData));
        }
        return sessionList;
    }
}
