package ru.infernoproject.common.characters.sql;

import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.db.sql.annotations.SQLFunction;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

@SQLObject(database = "characters", table = "characters")
public class CharacterClassDistribution implements SQLObjectWrapper {

    @SQLFunction(column = "count", expression = "COUNT(*)")
    public int count;

    @SQLField(column = "class")
    public ClassInfo classInfo;

}
