package ru.infernoproject.core.worldd.commands.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.worldd.WorldSession;
import ru.infernoproject.core.worldd.commands.WorldCommand;
import ru.infernoproject.core.worldd.commands.WorldCommandResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WorldCommand.Info(command = ".auth", accessLevel = 3, description = "Account manipulations")
public class AccountCommand implements WorldCommand {

    private final static Logger logger = LoggerFactory.getLogger(AccountCommand.class);

    @Override
    public WorldCommandResult execute(DataSourceManager dataSourceManager, WorldSession session, String... args) {
        try (Connection connection = dataSourceManager.getConnection("realmd")) {
            if (args.length > 0) {
                switch (args[0]) {
                    case "list":
                        PreparedStatement accountQuery = connection.prepareStatement(
                                "SELECT id, login, level FROM accounts"
                        );

                        List<String> accounts = new ArrayList<>();
                        try (ResultSet resultSet = accountQuery.executeQuery()) {
                            while (resultSet.next()) {
                                accounts.add(String.format(
                                        "ID(%d): Level(%s): %s",
                                        resultSet.getInt("id"),
                                        resultSet.getString("level").toUpperCase(),
                                        resultSet.getString("login")
                                ));
                            }
                        }

                        return WorldCommandResult.success(accounts.toArray(new String[]{}));
                    default:
                        return WorldCommandResult.failure(
                            String.format("Unknown action: %s", args[0])
                        );
                }
            }

            return WorldCommandResult.failure("No arguments specified");
        } catch (SQLException e) {
            logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
            return WorldCommandResult.failure("Server failure");
        }
    }
}
