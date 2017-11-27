package ru.infernoproject.realmd;

import com.zaxxer.hikari.pool.HikariPool;
import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.common.xor.XORCodec;
import ru.infernoproject.common.server.Listener;
import ru.infernoproject.common.server.Server;

public class RealmServer extends Server {

    private Listener listener;
    private RealmHandler handler;

    @Override
    protected void run() {
        String listenHost = config.getString("realmd.listenHost", "0.0.0.0");
        Integer listenPort = config.getInt("realmd.listenPort", 3274);

        try {
            dataSourceManager.initDataSources("realmd", "characters", "objects");
        } catch (FlywayException | HikariPool.PoolInitializationException e) {
            logger.error("Unable to initialize database: {}", e.getMessage());
            System.exit(1);
        }

        handler = new RealmHandler(dataSourceManager, config);

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XORCodec.class)
            .addHandler(handler)
            .build();

        threadPool.submit(listener);
        registerMBean(handler);

        awaitShutdown();
    }

    @Override
    protected void onShutdown() {
        handler.shutdown();
        listener.stop();
    }

    public static void main(String[] args) throws InterruptedException {
        RealmServer server = new RealmServer();

        server.main();
    }
}
