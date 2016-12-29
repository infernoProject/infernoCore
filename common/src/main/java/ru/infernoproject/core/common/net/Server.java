package ru.infernoproject.core.common.net;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.config.ConfigFile;
import ru.infernoproject.core.common.db.DataSourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public abstract class Server {

    protected static Logger logger;
    protected ConfigFile config = null;

    protected final DataSourceManager dataSourceManager;

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
            logger.error(String.format("Config file '%s' not found!", configFile));
        }

        if (config == null) {
            System.exit(1);
        }

        if (config.getBoolean("logging.debug", false)) {
            LogManager.getRootLogger().setLevel(Level.DEBUG);
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

        System.out.println(bannerString);
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
