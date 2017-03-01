package ru.infernoproject.worldd.characters;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;
import ru.infernoproject.worldd.characters.sql.CharacterInfo;
import ru.infernoproject.worldd.WorldSession;
import ru.infernoproject.worldd.characters.sql.CharacterSpell;
import ru.infernoproject.worldd.scripts.ScriptManager;
import ru.infernoproject.worldd.scripts.sql.SpellInfo;

import javax.script.ScriptException;
import java.sql.SQLException;
import java.util.List;

public class CharacterManager {

    private final DataSourceManager dataSourceManager;
    private final ScriptManager scriptManager;

    public CharacterManager(DataSourceManager dataSourceManager, ScriptManager scriptManager) {
        this.dataSourceManager = dataSourceManager;
        this.scriptManager = scriptManager;
    }

    public List<CharacterInfo> characterList(WorldSession session) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter("account").eq(session.getAccount().getAccountId()))
            .fetchAll();
    }

    public CharacterInfo characterCreate(CharacterInfo characterInfo, WorldSession session) throws SQLException {
        if (characterGet(characterInfo.getFirstName(), characterInfo.getLastName()) != null)
            return null;

        characterInfo.account = session.getAccount();

        dataSourceManager.query(CharacterInfo.class).insert(characterInfo);

        return characterGet(characterInfo.getFirstName(), characterInfo.getLastName());
    }

    public CharacterInfo characterGet(String firstName, String lastName) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("first_name").eq(firstName),
                new SQLFilter("last_name").eq(lastName)
            )).fetchOne();
    }

    public CharacterInfo characterGet(int characterId, WorldSession session) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("account").eq(session.getAccount().getAccountId()),
                new SQLFilter("id").eq(characterId)
            )).fetchOne();
    }

    public void update(Long diff) {
        // TODO(aderyugin): Implement delayed character deletion
    }

    public Boolean spellLearned(CharacterInfo characterInfo, int spellId) throws SQLException {
        return dataSourceManager.query(CharacterSpell.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("character_id").eq(characterInfo.getId()),
                new SQLFilter("spell_id").eq(spellId)
            )).fetchOne() != null;
    }

    public Boolean spellLearn(CharacterInfo characterInfo, int spellId) throws SQLException {
        try {
            SpellInfo spellInfo = scriptManager.spellGet(spellId);

            return !spellLearned(characterInfo, spellId) &&
                (spellInfo != null) &&
                dataSourceManager.query(CharacterSpell.class).insert(new CharacterSpell(
                    characterInfo, spellInfo
                )) > 0;
        } catch (ScriptException e) {
            return false;
        }
    }
}
