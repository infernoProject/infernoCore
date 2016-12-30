package ru.infernoproject.core.worldd.characters;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.worldd.WorldSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CharacterManager {

    private final DataSourceManager dataSourceManager;

    public CharacterManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }
    
    public List<CharacterInfo> characterList(WorldSession session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            List<CharacterInfo> characters = new ArrayList<>();

            PreparedStatement characterQuery = connection.prepareStatement(
                    "SELECT * FROM characters WHERE account = ?"
            );

            characterQuery.setInt(1, session.getAccountID());

            try (ResultSet resultSet = characterQuery.executeQuery()) {
                while (resultSet.next()) {
                    characters.add(new CharacterInfo(resultSet));
                }
            }

            return characters;
        }
    }

    public CharacterInfo characterCreate(CharacterInfo characterInfo, WorldSession session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT id FROM characters WHERE firstName = ? AND lastName = ?"
            );

            characterQuery.setString(1, characterInfo.getFirstName());
            characterQuery.setString(2, characterInfo.getLastName());

            try (ResultSet resultSet = characterQuery.executeQuery()) {
                if (resultSet.next()) {
                    return null;
                }
            }

            PreparedStatement characterCreator = connection.prepareStatement(
                "INSERT INTO characters (firstName, lastName, account, race, gender, class, currency) VALUES " +
                    "(?, ?, ?, ?, ?, ?, ?)"
            );

            characterCreator.setString(1, characterInfo.getFirstName());
            characterCreator.setString(2, characterInfo.getLastName());
            characterCreator.setInt(3, session.getAccountID());
            characterCreator.setInt(4, characterInfo.getRaceId());
            characterCreator.setString(5, characterInfo.getGender());
            characterCreator.setInt(6, characterInfo.getClassId());
            characterCreator.setInt(7, 100);

            characterCreator.execute();

            return characterGet(characterInfo.getFirstName(), characterInfo.getLastName());
        }
    }

    public CharacterInfo characterGet(String firstName, String lastName) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT * FROM characters WHERE firstName = ? AND lastName = ?"
            );

            characterQuery.setString(1, firstName);
            characterQuery.setString(2, lastName);

            try (ResultSet resultSet = characterQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new CharacterInfo(resultSet);
                }
            }

            return null;
        }
    }

    public CharacterInfo characterGet(int characterId, WorldSession session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT * FROM characters WHERE account = ? AND id = ?"
            );

            characterQuery.setInt(1, session.getAccountID());
            characterQuery.setInt(2, characterId);

            try (ResultSet resultSet = characterQuery.executeQuery()) {
                if (resultSet.next()) {
                    return new CharacterInfo(resultSet);
                }
            }

            return null;
        }
    }
}
