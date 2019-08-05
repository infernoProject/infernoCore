package ru.infernoproject.worldd.world.guild.sql;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;

@SQLObject(database = "characters", table = "guild_members")
public class GuildMember implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "guild_id")
    public Guild guild;

    @SQLField(column = "character_id")
    public CharacterInfo character;

    @SQLField(column = "level")
    public int level;
}
