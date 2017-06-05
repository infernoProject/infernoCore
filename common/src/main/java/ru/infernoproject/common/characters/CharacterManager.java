package ru.infernoproject.common.characters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.characters.sql.CharacterClassDistribution;
import ru.infernoproject.common.characters.sql.CharacterGenderDistribution;
import ru.infernoproject.common.characters.sql.CharacterRaceDistribution;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.SQLFilter;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.realmlist.RealmListEntry;

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

    public List<CharacterInfo> list(Account account) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("account").eq(account.id),
                new SQLFilter("delete_flag").eq(0)
            )).fetchAll();
    }

    public List<CharacterInfo> list_deleted(Account account) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("account").eq(account.id),
                new SQLFilter("delete_flag").eq(1)
            )).fetchAll();
    }

    public int create(CharacterInfo characterInfo) throws SQLException {
        if (exists(characterInfo))
            return -1;

        dataSourceManager.query(CharacterInfo.class).insert(characterInfo);

        return get(characterInfo.realm, characterInfo.firstName, characterInfo.lastName).id;
    }

    public boolean delete(CharacterInfo characterInfo) throws SQLException {
        if (exists(characterInfo)) {
            dataSourceManager.query(CharacterInfo.class).update(
                "SET `delete_flag` = 1, `delete_after` = DATE_ADD(NOW(), INTERVAL " + configFile.getInt("characters.expiration_time", 30) + " DAY) WHERE `id` = " + characterInfo.id
            );

            return true;
        }

        return false;
    }

    public boolean restore(CharacterInfo characterInfo) throws SQLException {
        if (!exists(characterInfo)) {
            dataSourceManager.query(CharacterInfo.class).update(
                "SET `delete_flag` = 0, `delete_after` = NULL WHERE `id` = " + characterInfo.id
            );

            return true;
        }

        return false;
    }

    public boolean exists(CharacterInfo characterInfo) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("realm").eq(characterInfo.realm.id),
                new SQLFilter("first_name").eq(characterInfo.firstName),
                new SQLFilter("last_name").eq(characterInfo.lastName),
                new SQLFilter("delete_flag").eq(0)
            )).fetchOne() != null;
    }

    public CharacterInfo get(RealmListEntry realm, String firstName, String lastName) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("realm").eq(realm.id),
                new SQLFilter("first_name").eq(firstName),
                new SQLFilter("last_name").eq(lastName),
                new SQLFilter("delete_flag").eq(0)
            )).fetchOne();
    }

    public CharacterInfo get(int characterId) throws SQLException {
        return dataSourceManager.query(CharacterInfo.class).select()
            .filter(new SQLFilter("id").eq(characterId))
            .fetchOne();
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

    public List<CharacterRaceDistribution> getRaceDistribution(RealmListEntry realm) throws SQLException {
        return dataSourceManager.query(CharacterRaceDistribution.class).select()
            .filter(new SQLFilter("realm").eq(realm.id))
            .group("race").fetchAll();
    }

    public List<CharacterClassDistribution> getClassDistribution(RealmListEntry realm) throws SQLException {
        return dataSourceManager.query(CharacterClassDistribution.class).select()
            .filter(new SQLFilter("realm").eq(realm.id))
            .group("class").fetchAll();
    }

    public List<CharacterGenderDistribution> getGenderDistribution(RealmListEntry realm) throws SQLException {
        return dataSourceManager.query(CharacterGenderDistribution.class).select()
            .filter(new SQLFilter("realm").eq(realm.id))
            .group("gender").fetchAll();
    }
}
