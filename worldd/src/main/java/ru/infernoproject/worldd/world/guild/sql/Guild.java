package ru.infernoproject.worldd.world.guild.sql;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;

@SQLObject(database = "characters", table = "guilds")
public class Guild implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "realm")
    public int realm;

    @SQLField(column = "tag")
    public String tag;

    @SQLField(column = "title")
    public String title;

    @SQLField(column = "description")
    public String description;
}
