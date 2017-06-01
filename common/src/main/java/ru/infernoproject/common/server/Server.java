package ru.infernoproject.common.server;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Server {

    protected static Logger logger;
    protected ConfigFile config = null;

    protected final DataSourceManager dataSourceManager;

    protected static final ExecutorService threadPool = Executors.newWorkStealingPool(
        Runtime.getRuntime().availableProcessors() * 10
    );

    private boolean running = true;

    public Server() {
        logger = LoggerFactory.getLogger(getClass());

        try {
            printBanner();
        } catch (IOException e) {
            logger.error("Unable to print banner: {}", e.getMessage());
        }

        String configName = System.getProperty(
            "configFile",
            getClass().getSimpleName() + ".conf"
        );
        File configFile = new File(configName);

        if (configFile.exists()) {
            try {
                config = ConfigFile.readConfig(configFile);
            } catch (IOException e) {
                logger.error("Unable to read config: {}", e.getMessage());
            }
        } else {
            logger.error("Config file '{}' not found!", configFile);
        }

        if (config == null) {
            System.exit(1);
        }

        LogManager.getRootLogger().setLevel(
            Level.toLevel(config.getString("logging.level", "INFO"))
        );

        for (String loggingGroup: config.getKeys("logging.levels.")) {
            String loggerName = loggingGroup.replace("logging.levels.", "");

            LogManager.getLogger(loggerName).setLevel(
                Level.toLevel(config.getString(loggingGroup, "INFO"))
            );
        }

        dataSourceManager = new DataSourceManager(config);
    }

    private void printBanner() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream banner = classLoader.getResource(getClass().getSimpleName() + ".banner").openStream();

        StringWriter bannerWriter = new StringWriter();
        IOUtils.copy(banner, bannerWriter, "UTF-8");
        String bannerString = bannerWriter.toString()
            .replace("%VERSION%", getClass().getPackage().getImplementationVersion());

        logger.info(bannerString);
    }

    public boolean isRunning() {
        return running;
    }

    protected void awaitShutdown() {
        try {
            while (running) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        onShutdown();

        running = false;
    }

    protected abstract void run();

    protected abstract void onShutdown();

    public void main() {
        logger.info("Starting {}", getClass().getSimpleName());

        run();
    }
}
