package ru.infernoproject.core.worldd.characters;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.worldd.WorldSession;
import ru.infernoproject.core.worldd.scripts.ScriptManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CharacterManager {

    private final DataSourceManager dataSourceManager;
    private final ScriptManager scriptManager;

    public CharacterManager(DataSourceManager dataSourceManager, ScriptManager scriptManager) {
        this.dataSourceManager = dataSourceManager;
        this.scriptManager = scriptManager;
    }
    
    public List<CharacterInfo> characterList(WorldSession session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            List<CharacterInfo> characters = new ArrayList<>();

            try (PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT * FROM characters WHERE account = ?"
            )) {
                characterQuery.setInt(1, session.getAccount().getAccountId());

                try (ResultSet resultSet = characterQuery.executeQuery()) {
                    while (resultSet.next()) {
                        characters.add(new CharacterInfo(resultSet));
                    }
                }
            }

            return characters;
        }
    }

    public CharacterInfo characterCreate(CharacterInfo characterInfo, WorldSession session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            try (PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT id FROM characters WHERE firstName = ? AND lastName = ?"
            )) {
                characterQuery.setString(1, characterInfo.getFirstName());
                characterQuery.setString(2, characterInfo.getLastName());

                try (ResultSet resultSet = characterQuery.executeQuery()) {
                    if (resultSet.next()) {
                        return null;
                    }
                }

                try (PreparedStatement characterCreator = connection.prepareStatement(
                    "INSERT INTO characters (firstName, lastName, account, race, gender, class, level, currency)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                )) {

                    characterCreator.setString(1, characterInfo.getFirstName());
                    characterCreator.setString(2, characterInfo.getLastName());
                    characterCreator.setInt(3, session.getAccount().getAccountId());
                    characterCreator.setInt(4, characterInfo.getRaceId());
                    characterCreator.setString(5, characterInfo.getGender());
                    characterCreator.setInt(6, characterInfo.getClassId());
                    characterCreator.setInt(7, 1);
                    characterCreator.setInt(8, 100);

                    characterCreator.execute();
                }
            }

            return characterGet(characterInfo.getFirstName(), characterInfo.getLastName());
        }
    }

    public CharacterInfo characterGet(String firstName, String lastName) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            try (PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT * FROM characters WHERE firstName = ? AND lastName = ?"
            )) {

                characterQuery.setString(1, firstName);
                characterQuery.setString(2, lastName);

                try (ResultSet resultSet = characterQuery.executeQuery()) {
                    if (resultSet.next()) {
                        return new CharacterInfo(resultSet);
                    }
                }
            }

            return null;
        }
    }

    public CharacterInfo characterGet(int characterId, WorldSession session) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            try (PreparedStatement characterQuery = connection.prepareStatement(
                "SELECT * FROM characters WHERE account = ? AND id = ?"
            )) {

                characterQuery.setInt(1, session.getAccount().getAccountId());
                characterQuery.setInt(2, characterId);

                try (ResultSet resultSet = characterQuery.executeQuery()) {
                    if (resultSet.next()) {
                        return new CharacterInfo(resultSet);
                    }
                }
            }

            return null;
        }
    }

    public void update(Long diff) {

    }

    public boolean spellLearned(CharacterInfo characterInfo, int spellId) throws SQLException {
        try (Connection connection = dataSourceManager.getConnection("characters")) {
            try (PreparedStatement spellQuery = connection.prepareStatement(
                "SELECT id FROM character_spells WHERE character_id = ? AND spell_id = ?"
            )) {

                spellQuery.setInt(1, characterInfo.getId());
                spellQuery.setInt(2, spellId);

                try (ResultSet resultSet = spellQuery.executeQuery()) {
                    return resultSet.next();
                }
            }
        }
    }

    public boolean spellLearn(CharacterInfo characterInfo, int spellId) throws SQLException {
        if (spellLearned(characterInfo, spellId))
            return false;

        try (Connection connection = dataSourceManager.getConnection("characters")) {
            try (PreparedStatement spellLearnQuery = connection.prepareStatement(
                "INSERT INTO character_spells (character_id, spell_id, cooldown) VALUES (?, ?, 0)"
            )) {

                spellLearnQuery.setInt(1, characterInfo.getId());
                spellLearnQuery.setInt(2, spellId);

                if (scriptManager.spellExists(spellId)) {
                    spellLearnQuery.execute();
                    return true;
                }
            }
        }

        return false;
    }
}
