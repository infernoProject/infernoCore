package ru.infernoproject.common.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.config.ConfigFile;

import java.io.File;
import java.io.IOException;

public class DataSourceUtils {

    private ConfigFile config = null;
    private final DataSourceManager dataSourceManager;

    private static final Logger logger = LoggerFactory.getLogger(DataSourceUtils.class);

    public DataSourceUtils() {

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

        dataSourceManager = new DataSourceManager(config);
    }

    private void migrate(String dataSource) {
        dataSourceManager.getMigrationManager(dataSource).migrate();
    }

    private void clean(String dataSource) {
        dataSourceManager.getMigrationManager(dataSource).clean();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            logger.error("Usage: DataSourceUtils <data source> <action>");
            System.exit(1);
        }

        DataSourceUtils dsu = new DataSourceUtils();

        switch (args[1]) {
            case "clean":
                dsu.clean(args[0]);
                break;
            case "migrate":
                dsu.migrate(args[0]);
                break;
            default:
                logger.error("Unsupported action '{}'", args[1]);
                System.exit(1);
        }
    }
}
