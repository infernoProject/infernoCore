package ru.infernoproject.common.characters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.server.ServerSession;

import java.sql.SQLException;
import java.util.List;

public class CharacterManager {

    private final DataSourceManager dataSourceManager;
    private final ConfigFile configFile;

    private static final Logger logger = LoggerFactory.getLogger(CharacterManager.class);

    public CharacterManager(DataSourceManager dataSourceManager, ConfigFile configFile) {
        this.dataSourceManager = dataSourceManager;
        this.configFile = configFile;
    }

    public List<CharacterInfo> list(ServerSession session) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("account").eq(session.getAccount().id),
                new SQLFilter("delete_flag").eq(0)
            )).fetchAll();
    }

    public CharacterInfo create(CharacterInfo characterInfo, ServerSession session) throws SQLException {
        if (get(characterInfo.firstName, characterInfo.lastName) != null)
            return null;

        characterInfo.account = session.getAccount();

        dataSourceManager.query(CharacterInfo.class).insert(characterInfo);

        return get(characterInfo.firstName, characterInfo.lastName);
    }

    public void delete(int characterId, ServerSession session) throws SQLException {
        CharacterInfo character = dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("id").eq(characterId),
                new SQLFilter("account").eq(session.getAccount().id)
            )).fetchOne();

        if (character != null) {
            dataSourceManager.query(CharacterInfo.class).update(
                "SET `delete_flag` = 1, `delete_after` = DATE_ADD(NOW(), INTERVAL " + configFile.getInt("characters.expiration_time", 30) + " DAY) WHERE `id` = " + character.id
            );
        }
    }

    public CharacterInfo get(String firstName, String lastName) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("first_name").eq(firstName),
                new SQLFilter("last_name").eq(lastName)
            )).fetchOne();
    }

    public CharacterInfo get(int characterId) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("id").eq(characterId)
            )).fetchOne();
    }

    public void cleanup() throws SQLException {
        dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().raw(
                "`delete_flag` = 1 AND `delete_after` < NOW()"
            )).fetchAll().parallelStream().forEach(character -> {
                try {
                    dataSourceManager.query(CharacterInfo.class).delete(character);
                    logger.info("Character {} deleted", character);
                } catch (SQLException e) {
                    logger.error("SQLError[{}]: {}", e.getSQLState(), e.getMessage());
                }
            });
    }
}
