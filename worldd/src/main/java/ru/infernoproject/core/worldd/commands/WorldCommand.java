package ru.infernoproject.core.worldd.commands;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.worldd.WorldSession;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface WorldCommand {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Info {
        String command();
        int accessLevel();
        String description();
    }

    WorldCommandResult execute(DataSourceManager dataSourceManager, WorldSession worldHandler, String... args);

}
