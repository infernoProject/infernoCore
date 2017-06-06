package ru.infernoproject.common.characters.sql;

import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.sql.annotations.SQLFunction;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

@SQLObject(database = "characters", table = "characters")
public class CharacterRaceDistribution implements SQLObjectWrapper {

    @SQLFunction(column = "count", expression = "COUNT(*)")
    public int count;

    @SQLField(column = "race")
    public RaceInfo raceInfo;

}
