package ru.infernoproject.core.worldd;

import org.flywaydb.core.api.FlywayException;
import ru.infernoproject.core.common.codec.xor.XORDecoder;
import ru.infernoproject.core.common.codec.xor.XOREncoder;
import ru.infernoproject.core.common.net.Listener;
import ru.infernoproject.core.common.net.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldServer extends Server {

    private static final ExecutorService threadPool = Executors.newWorkStealingPool(
        Runtime.getRuntime().availableProcessors() * 10
    );

    private Listener listener;

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

        listener = new Listener.Builder(listenHost, listenPort)
            .addHandler(XOREncoder.class)
            .addHandler(XORDecoder.class)
            .addHandler(new WorldHandler(dataSourceManager))
            .build();

        threadPool.submit(listener);

        awaitShutdown();
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
