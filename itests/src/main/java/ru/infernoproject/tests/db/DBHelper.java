package ru.infernoproject.tests.db;

import ru.infernoproject.common.auth.sql.Account;

import ru.infernoproject.common.auth.sql.AccountBan;
import ru.infernoproject.common.auth.sql.AccountLevel;
import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.tests.crypto.CryptoHelper;
import ru.infernoproject.worldd.script.sql.*;
import ru.infernoproject.worldd.world.guild.sql.Guild;
import ru.infernoproject.worldd.world.guild.sql.GuildMember;

import java.net.SocketAddress;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

public class DBHelper {

    private final DataSourceManager dataSourceManager;
    private final CryptoHelper cryptoHelper;

    public DBHelper(DataSourceManager dataSourceManager, CryptoHelper cryptoHelper) {
        this.dataSourceManager = dataSourceManager;
        this.cryptoHelper = cryptoHelper;
    }

    public <T extends SQLObjectWrapper> void cleanUpTable(Class<T> model, String filter) {
        try {
            dataSourceManager.query(model).delete(filter);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Account createUser(String login, String password) {
        try {
            byte[] clientSalt = cryptoHelper.generateSalt();
            byte[] clientVerifier = cryptoHelper.calculateVerifier(login, password, clientSalt);

            dataSourceManager.query(Account.class).insert(new Account(
                login, AccountLevel.USER, String.format("%s@testCase", login), clientSalt, clientVerifier
            ));

            return dataSourceManager.query(Account.class).select()
                .filter(new SQLFilter("login").eq(login))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUserAccessLevel(Account account, AccountLevel level) {
        try {
            account.accessLevel = level;

            dataSourceManager.query(Account.class).update(account);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Session createSession(Account account, SocketAddress address) {
        try {
            Session session = new Session(account, cryptoHelper.generateSalt(), address);

            dataSourceManager.query(Session.class).insert(session);

            return dataSourceManager.query(Session.class).select()
                .filter(new SQLFilter("session_key").eq(session.getKeyHex()))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RealmListEntry createRealmIfNotExists(String name, String host, int port) {
        try {
            RealmListEntry realmListEntry = dataSourceManager.query(RealmListEntry.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();

            if (realmListEntry != null)
                return realmListEntry;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return createRealm(name, host, port);
    }

    public RealmListEntry createRealm(String name, String host, int port) {
        try {
            RealmListEntry realmServer = new RealmListEntry();

            realmServer.name = name;
            realmServer.type = 1;
            realmServer.online = 1;
            realmServer.lastSeen = LocalDateTime.now();

            realmServer.serverHost = host;
            realmServer.serverPort = port;

            dataSourceManager.query(RealmListEntry.class).insert(realmServer);

            return dataSourceManager.query(RealmListEntry.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ClassInfo createClass(String name, String resource) {
        try {
            ClassInfo classInfo = new ClassInfo();

            classInfo.name = name;
            classInfo.resource = resource;

            dataSourceManager.query(ClassInfo.class).insert(classInfo);

            return dataSourceManager.query(ClassInfo.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public RaceInfo createRace(String name, String resource) {
        try {
            RaceInfo raceInfo = new RaceInfo();

            raceInfo.name = name;
            raceInfo.resource = resource;

            dataSourceManager.query(RaceInfo.class).insert(raceInfo);

            return dataSourceManager.query(RaceInfo.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CharacterInfo createCharacter(CharacterInfo characterInfo) {
        try {
            dataSourceManager.query(CharacterInfo.class).insert(characterInfo);

            return dataSourceManager.query(CharacterInfo.class).select()
                .filter(new SQLFilter().and(
                    new SQLFilter("realm").eq(characterInfo.realm.id),
                    new SQLFilter("first_name").eq(characterInfo.firstName),
                    new SQLFilter("last_name").eq(characterInfo.lastName),
                    new SQLFilter("delete_flag").eq(0)
                )).fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CharacterInfo createCharacter(Account account, RealmListEntry realm, String firstName, String lastName, GenderInfo genderInfo, RaceInfo raceInfo, ClassInfo classInfo, byte[] body) {
        CharacterInfo characterInfo = new CharacterInfo();

        characterInfo.account = account;
        characterInfo.realm = realm;
        characterInfo.firstName = firstName;
        characterInfo.lastName = lastName;
        characterInfo.gender = genderInfo;
        characterInfo.raceInfo = raceInfo;
        characterInfo.classInfo = classInfo;
        characterInfo.body = body;

        return createCharacter(characterInfo);
    }

    public void setCharacterPosition(CharacterInfo characterInfo, float x, float y, float z, float orientation) {
        characterInfo.positionX = x;
        characterInfo.positionY = y;
        characterInfo.positionZ = z;

        characterInfo.orientation = orientation;

        try {
            dataSourceManager.query(CharacterInfo.class).update(characterInfo);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteCharacter(CharacterInfo characterInfo) {
        deleteCharacter(characterInfo, false);
    }

    public void deleteCharacter(CharacterInfo characterInfo, boolean force) {
        try {
            if (force) {
                dataSourceManager.query(CharacterInfo.class).delete(characterInfo);
            } else {
                dataSourceManager.query(CharacterInfo.class).update(
                    "SET `delete_flag` = 1, `delete_after` = DATE_ADD(NOW(), INTERVAL 1 MINUTE) WHERE `id` = " + characterInfo.id
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void selectCharacter(Session session, CharacterInfo characterInfo) {
        try {
            session.characterInfo = characterInfo;

            dataSourceManager.query(Session.class).update(session);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Script createScript(String name, String lang, String script) {
        try {
            Script scriptData = new Script();

            scriptData.name = name;
            scriptData.script = script;
            scriptData.language = lang;

            dataSourceManager.query(Script.class).insert(scriptData);

            return dataSourceManager.query(Script.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Command createCommand(String name, AccountLevel accessLevel, String lang, String script) {
        try {
            Script scriptData = createScript(name, lang, script);

            Command commandData = new Command();

            commandData.name = name;
            commandData.level = accessLevel;
            commandData.script = scriptData;

            dataSourceManager.query(Command.class).insert(commandData);

            return dataSourceManager.query(Command.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public AccountBan banAccount(Account account, long expiresIn, String reason) {
        try {
            AccountBan ban = new AccountBan();

            ban.account = account;
            ban.expires = LocalDateTime.now().plusSeconds(expiresIn);
            ban.reason = reason;

            dataSourceManager.query(AccountBan.class).insert(ban);

            return dataSourceManager.query(AccountBan.class).select()
                .filter(new SQLFilter("account").eq(account.id))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Spell createSpell(String name, SpellType type, int minLevel, ClassInfo requiredClass, long coolDown, float distance, float radius, long basicPotential, Script script) {
        return createSpell(name, type, minLevel, requiredClass, coolDown, distance, radius, basicPotential, script, null, null);
    }

    public Spell createSpell(String name, SpellType type, int minLevel, ClassInfo requiredClass, long coolDown, float distance, float radius, long basicPotential, Script script, Effect effect) {
        return createSpell(name, type, minLevel, requiredClass, coolDown, distance, radius, basicPotential, script, effect, null);
    }

    public Spell createSpell(String name, SpellType type, int minLevel, ClassInfo requiredClass, long coolDown, float distance, float radius, long basicPotential, Script script, DamageOverTime dot) {
        return createSpell(name, type, minLevel, requiredClass, coolDown, distance, radius, basicPotential, script, null, dot);
    }

    public Spell createSpell(String name, SpellType type, int minLevel, ClassInfo requiredClass, long coolDown, float distance, float radius, long basicPotential, Script script, Effect effect, DamageOverTime dot) {
        try {
            Spell spell = new Spell();

            spell.name = name;
            spell.type = type;

            spell.requiredLevel = minLevel;
            spell.requiredClass = requiredClass;

            spell.coolDown = coolDown;
            spell.distance = distance;
            spell.radius = radius;
            spell.basicPotential = basicPotential;

            spell.effect = effect;
            spell.damageOverTime = dot;

            spell.script = script;

            dataSourceManager.query(Spell.class).insert(spell);

            return dataSourceManager.query(Spell.class).select()
                .filter(new SQLFilter().and(
                    new SQLFilter("name").eq(name),
                    new SQLFilter("required_level").eq(minLevel),
                    new SQLFilter("required_class").eq(requiredClass.id)
                )).fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Guild createGuild(String title, String tag, String description, CharacterInfo owner) {
        try {
            Guild guild = new Guild();

            guild.realm = owner.realm.id;
            guild.title = title;
            guild.tag = tag;
            guild.description = description;

            dataSourceManager.query(Guild.class).insert(guild);

            guild = dataSourceManager.query(Guild.class).select()
                .filter(new SQLFilter().and(
                    new SQLFilter("realm").eq(owner.realm.id),
                    new SQLFilter("tag").eq(tag),
                    new SQLFilter("title").eq(title)
                )).fetchOne();

            addGuildMember(guild, owner, 1);

            return guild;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Guild getCharacterGuild(CharacterInfo characterInfo) {
        try {
            GuildMember guildMember = dataSourceManager.query(GuildMember.class).select()
                .filter(new SQLFilter("character_id").eq(characterInfo.id))
                .fetchOne();

            return Objects.nonNull(guildMember) ? guildMember.guild : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addGuildMember(Guild guild, CharacterInfo player, int level) {
        try {
            GuildMember guildMember = new GuildMember();

            guildMember.guild = guild;
            guildMember.character = player;
            guildMember.level = level;

            dataSourceManager.query(GuildMember.class).insert(guildMember);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getCharacterGuildLevel(Guild guild, CharacterInfo player) {
        try {
            GuildMember guildMember = dataSourceManager.query(GuildMember.class).select()
                .filter(new SQLFilter().and(
                    new SQLFilter("character_id").eq(player.id),
                    new SQLFilter("guild_id").eq(guild.id)
                )).fetchOne();

            return Objects.nonNull(guildMember) ? guildMember.level : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Effect createEffect(String name, EffectType type, long duration, Script script) {
        try {
            Effect effect = new Effect();

            effect.name = name;
            effect.duration = duration;
            effect.type = type;
            effect.script = script;

            dataSourceManager.query(Effect.class).insert(effect);

            return dataSourceManager.query(Effect.class).select()
                .filter(new SQLFilter("name").eq(name))
                .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public DamageOverTime createDoT(String name, long duration, long tickInterval, long basicPotential, Script script) {
        try {
            DamageOverTime damageOverTime = new DamageOverTime();

            damageOverTime.name = name;
            damageOverTime.duration = duration;
            damageOverTime.tickInterval = tickInterval;
            damageOverTime.basicPotential = basicPotential;
            damageOverTime.script = script;

            dataSourceManager.query(DamageOverTime.class).insert(damageOverTime);

            return dataSourceManager.query(DamageOverTime.class).select()
                    .filter(new SQLFilter("name").eq(name))
                    .fetchOne();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
