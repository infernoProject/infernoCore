package ru.infernoproject.core.worldd.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WorldCommander {

    private final Map<WorldCommand.Info, Class<? extends WorldCommand>> commands = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(WorldCommander.class);

    public boolean register(Class<? extends WorldCommand> command) {
        if (command.isAnnotationPresent(WorldCommand.Info.class)) {
            WorldCommand.Info commandInfo = command.getAnnotation(WorldCommand.Info.class);
            commands.put(commandInfo, command);
        }

        return false;
    }

    public WorldCommand getCommand(String name, int accessLevel) {
        Optional<WorldCommand.Info> commandInfoOptional = commands.keySet().stream()
            .filter(info -> info.command().equals(name) && info.accessLevel() <= accessLevel)
            .findFirst();

        if (commandInfoOptional.isPresent()) {
            try {
                return commands.get(commandInfoOptional.get()).newInstance();
            } catch (IllegalAccessException e) {
                logger.error("IllegalAccessException: {}", e.getMessage());
            } catch (InstantiationException e) {
                logger.error("Unable to instantiate command: {}", e.getMessage());
            }
        }

        return null;
    }

    public Map<WorldCommand.Info, Class<? extends WorldCommand>> getCommands() {
        return commands;
    }
}
