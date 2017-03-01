package ru.infernoproject.core.realmd;

import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.core.common.codec.xor.XORDecoder;
import ru.infernoproject.core.common.codec.xor.XOREncoder;
import ru.infernoproject.core.common.net.server.Listener;
import ru.infernoproject.core.common.net.server.Server;
import ru.infernoproject.core.common.srp.SRP6Engine;

public class RealmServer extends Server {

    private Listener listener;

    protected void run() {
        String listenHost = config.getString("realmd.listenHost", "0.0.0.0");
        Integer listenPort = config.getInt("realmd.listenPort", 3274);

        try {
            dataSourceManager.initDataSources("realmd");
        } catch (FlywayException e) {
            logger.error("Unable to initialize database: {}", e.getMessage());
            System.exit(1);
        }

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XOREncoder.class)
            .addHandler(XORDecoder.class)
            .addHandler(new RealmHandler(dataSourceManager, accountManager))
            .build();

        threadPool.submit(listener);

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
