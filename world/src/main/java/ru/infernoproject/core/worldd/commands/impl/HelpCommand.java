package ru.infernoproject.core.worldd.commands.impl;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.worldd.WorldSession;
import ru.infernoproject.core.worldd.commands.WorldCommand;
import ru.infernoproject.core.worldd.commands.WorldCommandResult;

import java.util.stream.Collectors;

@WorldCommand.Info(command = ".help", accessLevel = 2, description = "Show help")
public class HelpCommand implements WorldCommand {

    @Override
    public WorldCommandResult execute(DataSourceManager dataSourceManager, WorldSession session, String... args) {
        String[] commandInfo = session.getWorldHandler().getCommander().getCommands().keySet().stream()
            .filter(info -> info.accessLevel() <= session.getAccessLevel())
            .map(info -> String.format(
                "%s(%d) - %s", info.command(), info.accessLevel(), info.description())
            )
            .collect(Collectors.toList()).toArray(new String[] {});

        return WorldCommandResult.success(commandInfo);
    }
}
