package ru.infernoproject.core.realmd;

import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.core.common.codec.xor.XORDecoder;
import ru.infernoproject.core.common.codec.xor.XOREncoder;
import ru.infernoproject.core.common.net.Listener;
import ru.infernoproject.core.common.net.Server;
import ru.infernoproject.core.realmd.session.SessionKiller;
import ru.infernoproject.core.realmd.srp.SRP6Engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RealmServer extends Server {

    private static final ExecutorService threadPool = Executors.newWorkStealingPool(
        Runtime.getRuntime().availableProcessors() * 10
    );
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors() * 10
    );

    private Listener listener;

    protected void run() {
        String listenHost = config.getString("realmd.listenHost", "0.0.0.0");
        Integer listenPort = config.getInt("realmd.listenPort", 3274);

        SRP6Engine srp6Engine = new SRP6Engine(config);

        try {
            dataSourceManager.initDataSources("realmd");
        } catch (FlywayException e) {
            logger.error("Unable to initialize database: {}", e.getMessage());
            System.exit(1);
        }

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XOREncoder.class)
            .addHandler(XORDecoder.class)
            .addHandler(new RealmHandler(dataSourceManager, srp6Engine))
            .build();

        SessionKiller sessionKiller = new SessionKiller(
            dataSourceManager, config.getInt("session.ttl", 180)
        );

        threadPool.submit(listener);

        scheduler.scheduleAtFixedRate(sessionKiller, 0 ,60, TimeUnit.SECONDS);

        awaitShutdown();
    }

    protected void onShutdown() {
        listener.stop();

        scheduler.shutdown();
        threadPool.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        RealmServer server = new RealmServer();

        server.main();
    }
}
