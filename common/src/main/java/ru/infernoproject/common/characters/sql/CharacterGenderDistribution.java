package ru.infernoproject.common.characters.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLFunction;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;

@SQLObject(database = "characters", table = "characters")
public class CharacterGenderDistribution implements SQLObjectWrapper {

    @SQLFunction(column = "count", expression = "COUNT(*)")
    public int count;

    @SQLField(column = "gender")
    public String gender;

}
