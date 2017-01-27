package ru.infernoproject.core.worldd;

import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.core.common.codec.xor.XORDecoder;
import ru.infernoproject.core.common.codec.xor.XOREncoder;
import ru.infernoproject.core.common.net.server.Listener;
import ru.infernoproject.core.common.net.server.Server;
import ru.infernoproject.core.worldd.world.WorldTimer;

public class WorldServer extends Server {

    private Listener listener;

    private WorldTimer timer;
    private WorldHandler handler;

    @Override
    protected void run() {
        String listenHost = config.getString("world.listenHost", "0.0.0.0");
        Integer listenPort = config.getInt("world.listenPort", 8085);

        try {
            dataSourceManager.initDataSources("realmd", "world", "characters");
        } catch (FlywayException e) {
            logger.error("Unable to initialize database: {}", e.getMessage());
            System.exit(1);
        }

        timer = new WorldTimer();
        handler = new WorldHandler(dataSourceManager, accountManager);

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XOREncoder.class)
            .addHandler(XORDecoder.class)
            .addHandler(handler)
            .build();

        threadPool.submit(listener);

        while (isRunning()) {
            Long timeDiff = timer.tick();

            handler.update(timeDiff);
        }
    }

    @Override
    protected void onShutdown() {
        listener.stop();

        threadPool.shutdown();
    }

    public static void main(String[] args) {
        WorldServer server = new WorldServer();

        server.main();
    }
}