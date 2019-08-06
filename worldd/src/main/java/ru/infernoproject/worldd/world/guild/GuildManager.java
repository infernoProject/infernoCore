package ru.infernoproject.worldd.world.guild;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.worldd.world.guild.sql.Guild;
import ru.infernoproject.worldd.world.guild.sql.GuildMember;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuildManager {

    private final DataSourceManager dataSourceManager;

    public GuildManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public Guild getPlayerGuild(int id) throws SQLException {
        GuildMember guildMember = dataSourceManager.query(GuildMember.class).select()
            .filter(new SQLFilter("character_id").eq(id))
            .fetchOne();

        return Objects.nonNull(guildMember) ? guildMember.guild : null;
    }

    public Guild getGuild(int id) throws SQLException {
        return dataSourceManager.query(Guild.class).select()
            .filter(new SQLFilter("id").eq(id))
            .fetchOne();
    }

    public Guild getGuildByTitle(int realm, String title) throws SQLException {
        return dataSourceManager.query(Guild.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("title").eq(title),
                new SQLFilter("realm").eq(realm)
            )).fetchOne();
    }

    public List<CharacterInfo> getGuildPlayers(int id) throws SQLException {
        return dataSourceManager.query(GuildMember.class).select()
            .filter(new SQLFilter("guild_id").eq(id))
            .fetchAll().stream()
            .map(member -> member.character)
            .collect(Collectors.toList());
    }

    public CharacterInfo getGuildMaster(int id) throws SQLException {
        GuildMember guildMember = dataSourceManager.query(GuildMember.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("guild_id").eq(id),
                new SQLFilter("level").eq(1)
            )).fetchOne();

        return Objects.nonNull(guildMember) ? guildMember.character : null;
    }

    public Guild createGuild(String title, String tag, String description, CharacterInfo owner) throws SQLException {
        Guild guildInfo = dataSourceManager.query(Guild.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("realm").eq(owner.realm.id),
                new SQLFilter().or(
                    new SQLFilter("tag").eq(tag),
                    new SQLFilter("title").eq(title)
                )
            )).fetchOne();

        if (Objects.nonNull(guildInfo)) {
            return null;
        }

        guildInfo = new Guild();

        guildInfo.tag = tag;
        guildInfo.title = title;
        guildInfo.description = description;
        guildInfo.realm = owner.realm.id;

        dataSourceManager.query(Guild.class).insert(guildInfo);

        guildInfo = getGuildByTitle(owner.realm.id, title);
        addGuildMember(guildInfo, owner, 1);

        return guildInfo;
    }

    public void addGuildMember(Guild guild, CharacterInfo player, int level) throws SQLException {
        GuildMember guildMember = new GuildMember();

        guildMember.guild = guild;
        guildMember.character = player;
        guildMember.level = level;

        dataSourceManager.query(GuildMember.class).insert(guildMember);
    }

    public void removeGuildMember(CharacterInfo player) throws SQLException {
        dataSourceManager.query(GuildMember.class).delete("WHERE `character_id` = " + player.id);
    }

    public void removeGuild(int id) throws SQLException {
        dataSourceManager.query(GuildMember.class).delete("WHERE `guild_id` = " + id);
        dataSourceManager.query(Guild.class).delete("WHERE `id` = " + id);
    }

    public int getPlayerLevel(Guild guild, CharacterInfo characterInfo) throws SQLException {
        GuildMember guildMember = dataSourceManager.query(GuildMember.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("guild_id").eq(guild.id),
                new SQLFilter("character_id").eq(characterInfo.id)
            )).fetchOne();

        return Objects.nonNull(guildMember) ? guildMember.level : 0;
    }

    public void setPlayerLevel(Guild guild, CharacterInfo player, int level) throws SQLException {
        GuildMember guildMember = dataSourceManager.query(GuildMember.class).select()
            .filter(new SQLFilter().and(
                new SQLFilter("guild_id").eq(guild.id),
                new SQLFilter("character_id").eq(player.id)
            )).fetchOne();

        guildMember.level = level;

        dataSourceManager.query(GuildMember.class).update(guildMember);
    }
}
