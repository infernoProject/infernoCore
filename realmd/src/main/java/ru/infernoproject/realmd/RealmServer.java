package ru.infernoproject.realmd;

import com.zaxxer.hikari.pool.HikariPool;
import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.common.xor.XORCodec;
import ru.infernoproject.common.server.Listener;
import ru.infernoproject.common.server.Server;

public class RealmServer extends Server {

    private Listener listener;

    @Override
    protected void run() {
        String listenHost = config.getString("realmd.listenHost", "0.0.0.0");
        Integer listenPort = config.getInt("realmd.listenPort", 3274);

        try {
            dataSourceManager.initDataSources("realmd", "characters");
        } catch (FlywayException | HikariPool.PoolInitializationException e) {
            logger.error("Unable to initialize database: {}", e.getMessage());
            System.exit(1);
        }

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XORCodec.class)
            .addHandler(new RealmHandler(dataSourceManager, config))
            .build();

        threadPool.submit(listener);

        awaitShutdown();
    }

    @Override
    protected void onShutdown() {
        listener.stop();

        threadPool.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        RealmServer server = new RealmServer();

        server.main();
    }
}
