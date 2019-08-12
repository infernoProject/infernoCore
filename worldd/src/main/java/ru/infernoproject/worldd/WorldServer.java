package ru.infernoproject.worldd;

import com.zaxxer.hikari.pool.HikariPool;
import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.common.xor.XORCodec;
import ru.infernoproject.common.server.Listener;
import ru.infernoproject.common.server.Server;
import ru.infernoproject.worldd.world.WorldTimer;

public class WorldServer extends Server {

    private Listener listener;

    private WorldTimer timer;
    private WorldHandler handler;

    @Override
    protected void run() {
        String listenHost = config.getString("world.listenHost", "0.0.0.0");
        Integer listenPort = config.getInt("world.listenPort", 8085);

        try {
            dataSourceManager.initDataSources("realmd", "world", "characters", "objects");
        } catch (FlywayException | HikariPool.PoolInitializationException e) {
            logger.error("Unable to initialize database: {}", e.getMessage());
            System.exit(1);
        }

        timer = new WorldTimer();
        handler = new WorldHandler(dataSourceManager, config, timer);

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XORCodec.class)
            .addHandler(handler)
            .build();

        threadPool.submit(listener);
        registerMBean(handler);

        while (isRunning()) {
            Long timeDiff = timer.tick();

            handler.update(timeDiff);
        }
    }

    @Override
    protected void onShutdown() {
        handler.shutdown();
        listener.stop();
    }

    public static void main(String[] args) {
        WorldServer server = new WorldServer();

        server.main();
    }
}
