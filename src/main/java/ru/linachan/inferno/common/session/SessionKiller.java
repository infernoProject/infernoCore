package ru.linachan.inferno.common.session;

import java.util.stream.Collectors;

public class SessionKiller implements Runnable {

    private SessionManager sessionManager;
    private boolean isRunning = true;

    private static final long EXPIRATION_TIME = 60000L;

    public SessionKiller(SessionManager manager) {
        sessionManager = manager;
    }

    @Override
    public void run() {
        while (isRunning) {
            sessionManager.listSessions().stream()
                .filter(session -> System.currentTimeMillis() - session.getUpdateTime() > EXPIRATION_TIME)
                .collect(Collectors.toList()).stream()
                .forEach(session -> sessionManager.killSession(session));

            try { Thread.sleep(1000); } catch(InterruptedException ignored) {}
        }
    }

    public void stop() {
        isRunning = false;
    }
}
