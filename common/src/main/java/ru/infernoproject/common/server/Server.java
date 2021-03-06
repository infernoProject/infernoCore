package ru.infernoproject.common.server;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.jmx.InfernoMBean;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanAttribute;
import ru.infernoproject.common.jmx.annotations.InfernoMBeanOperation;
import ru.infernoproject.common.utils.ErrorUtils;

import javax.management.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.concurrent.*;

public abstract class Server implements InfernoMBean {

    protected static Logger logger;
    protected ConfigFile config = null;

    protected final DataSourceManager dataSourceManager;
    private final MBeanServer mBeanServer;

    protected static final ExecutorService threadPool = Executors.newWorkStealingPool(
        Runtime.getRuntime().availableProcessors() * 10
    );

    protected static final ScheduledExecutorService scheduledThreadPool = Executors.newSingleThreadScheduledExecutor();

    private boolean running = true;
    private ScheduledFuture shutdownFuture = null;

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

        config.getKeys("logging.levels.").stream()
            .map(loggingGroup -> loggingGroup.replace("logging.levels.", ""))
            .forEach(loggerName -> LogManager.getLogger(loggerName).setLevel(
                Level.toLevel(config.getString("logging.levels." + loggerName, "INFO"))
            ));

        dataSourceManager = new DataSourceManager(config);
        mBeanServer = ManagementFactory.getPlatformMBeanServer();

        registerMBean(this);
    }

    protected void registerMBean(InfernoMBean mBean) {
        try {
            mBeanServer.registerMBean(
                mBean, new ObjectName(String.format("ru.inferno-project.common.server:type=%s", mBean.getClass().getSimpleName()))
            );
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            ErrorUtils.logger(logger).error(String.format("Unable to setup JMX MBean '%s'", mBean.getClass().getSimpleName()), e);
        }
    }

    private String getVersion() {
        Package packageInfo = getClass().getPackage();
        String implementationVersion = packageInfo.getImplementationVersion();

        if (implementationVersion != null) {
            return implementationVersion;
        } else {
            return "LATEST";
        }
    }

    private void printBanner() throws IOException {
        URL bannerResource = getClass().getClassLoader().getResource(getClass().getSimpleName() + ".banner");
        if (bannerResource != null) {
            InputStream banner = bannerResource.openStream();

            StringWriter bannerWriter = new StringWriter();
            IOUtils.copy(banner, bannerWriter, "UTF-8");
            String bannerString = bannerWriter.toString()
                .replace("%VERSION%", getVersion());

            logger.info(bannerString);
        } else {
            logger.warn("Banner '{}' not found", getClass().getSimpleName() + ".banner");
        }
    }

    protected boolean isRunning() {
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

    @InfernoMBeanOperation(description = "Shutdown server immediately")
    public void shutdown() {
        onShutdown();
        threadPool.shutdown();
        scheduledThreadPool.shutdown();

        running = false;
    }

    @InfernoMBeanOperation(description = "Schedule server shutdown (seconds)")
    public void scheduleShutdown(long shutdownDelay) {
        shutdownFuture = scheduledThreadPool.schedule(
            this::shutdown, shutdownDelay, TimeUnit.SECONDS
        );

        logger.warn("Scheduled server shutdown in {} second(s)", shutdownDelay);
    }

    @InfernoMBeanOperation(description = "Cancel scheduled server shutdown (if possible)")
    public void cancelShutdown() {
        if (shutdownFuture != null) {
            if (shutdownFuture.cancel(false)) {
                logger.info("Scheduled server shutdown cancelled");
            }

            shutdownFuture = null;
        }
    }

    protected abstract void run();

    protected abstract void onShutdown();

    public void main() {
        logger.info("Starting {}", getClass().getSimpleName());

        run();
    }
}
