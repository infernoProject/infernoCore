package ru.infernoproject.common.characters.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.HexBin;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;

@SQLObject(table = "characters", database = "characters")
public class CharacterInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "account")
    public Account account;

    @SQLField(column = "realm")
    public RealmListEntry realm;

    @SQLField(column = "first_name")
    public String firstName;

    @SQLField(column = "last_name")
    public String lastName;

    @SQLField(column = "race")
    public int raceId;

    @SQLField(column = "gender")
    public String gender;

    @SQLField(column = "class")
    public int classId;

    @SQLField(column = "level")
    public int level = 0;

    @SQLField(column = "exp")
    public long exp = 0;

    @SQLField(column = "currency")
    public long currency = 0;

    @SQLField(column = "body")
    public String body;

    public CharacterInfo() {
        // Default constructor for SQLObjectWrapper
    }

    public CharacterInfo(ByteWrapper wrapper) {
        id = wrapper.getInt();

        firstName = wrapper.getString();
        lastName = wrapper.getString();

        raceId = wrapper.getInt();
        gender = wrapper.getString();
        classId = wrapper.getInt();

        body = HexBin.encode(wrapper.getBytes());
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id)
            .put(firstName).put(lastName)
            .put(raceId).put(gender).put(classId)
            .put(level).put(exp).put(currency).put(HexBin.decode(body))
            .toByteArray();
    }

    @Override
    public String toString() {
        return String.format(
            "CharacterInfo(ID=%d):LVL(%d):R(%d):C(%d): %s : %s %s",
            id, level, raceId, classId, gender, firstName, lastName
        );
    }
}