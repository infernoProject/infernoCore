package ru.infernoproject.core.worldd.characters;

import ru.infernoproject.core.common.db.DataSourceManager;
import ru.infernoproject.core.common.error.CoreException;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.worldd.WorldSession;
import ru.infernoproject.core.worldd.scripts.ScriptManager;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CharacterManager {

    private final DataSourceManager dataSourceManager;
    private final ScriptManager scriptManager;

    public CharacterManager(DataSourceManager dataSourceManager, ScriptManager scriptManager) {
        this.dataSourceManager = dataSourceManager;
        this.scriptManager = scriptManager;
    }

    @SuppressWarnings("unchecked")
    public List<CharacterInfo> characterList(WorldSession session) throws CoreException {
        return (List<CharacterInfo>) dataSourceManager.query(
            "characters", "SELECT * FROM characters WHERE account = ?"
        ).configure((query) -> {
            query.setInt(1, session.getAccount().getAccountId());
        }).executeSelect((resultSet) -> {
            List<CharacterInfo> characters = new ArrayList<>();

            while (resultSet.next()) {
                characters.add(new CharacterInfo(resultSet));
            }

            return characters;
        });
    }

    public CharacterInfo characterCreate(CharacterInfo characterInfo, WorldSession session) throws CoreException {
        Boolean characterExists = (Boolean) dataSourceManager.query(
            "characters", "SELECT id FROM characters WHERE firstName = ? AND lastName = ?"
        ).configure((query) -> {
            query.setString(1, characterInfo.getFirstName());
            query.setString(2, characterInfo.getLastName());
        }).executeSelect(ResultSet::next);

        if (!characterExists) {
            dataSourceManager.query(
                "characters",
                "INSERT INTO characters (firstName, lastName, account, race, gender, class, level, currency)" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            ).configure((query) -> {
                query.setString(1, characterInfo.getFirstName());
                query.setString(2, characterInfo.getLastName());
                query.setInt(3, session.getAccount().getAccountId());
                query.setInt(4, characterInfo.getRaceId());
                query.setString(5, characterInfo.getGender());
                query.setInt(6, characterInfo.getClassId());
                query.setInt(7, 1);
                query.setInt(8, 100);
            }).executeUpdate();

            return characterGet(characterInfo.getFirstName(), characterInfo.getLastName());
        }

        return null;
    }

    public CharacterInfo characterGet(String firstName, String lastName) throws CoreException {
        return (CharacterInfo) dataSourceManager.query(
            "characters", "SELECT * FROM characters WHERE firstName = ? AND lastName = ?"
        ).configure((query) -> {
            query.setString(1, firstName);
            query.setString(2, lastName);
        }).executeSelect((resultSet) -> {
            if (resultSet.next()) {
                return new CharacterInfo(resultSet);
            }

            return null;
        });
    }

    public CharacterInfo characterGet(int characterId, WorldSession session) throws CoreException {
        return (CharacterInfo) dataSourceManager.query(
            "characters", "SELECT * FROM characters WHERE account = ? AND id = ?"
        ).configure((query) -> {
            query.setInt(1, session.getAccount().getAccountId());
            query.setInt(2, characterId);
        }).executeSelect((resultSet) -> {
            if (resultSet.next()) {
                return new CharacterInfo(resultSet);
            }

            return null;
        });
    }

    public void update(Long diff) {

    }

    public Boolean spellLearned(CharacterInfo characterInfo, int spellId) throws CoreException {
        return (Boolean) dataSourceManager.query(
            "characters", "SELECT id FROM character_spells WHERE character_id = ? AND spell_id = ?"
        ).configure((query) -> {
            query.setInt(1, characterInfo.getId());
            query.setInt(2, spellId);
        }).executeSelect(ResultSet::next);

    }

    public Boolean spellLearn(CharacterInfo characterInfo, int spellId) throws CoreException {
        return !spellLearned(characterInfo, spellId) && scriptManager.spellExists(spellId) && dataSourceManager.query(
            "characters", "INSERT INTO character_spells (character_id, spell_id, cooldown) VALUES (?, ?, 0)"
        ).configure((query) -> {
            query.setInt(1, characterInfo.getId());
            query.setInt(2, spellId);
        }).executeUpdate() > 0;
    }
}
