package ru.infernoproject.common.characters.sql;

import ru.infernoproject.common.data.sql.ClassInfo;
import ru.infernoproject.common.data.sql.GenderInfo;
import ru.infernoproject.common.data.sql.RaceInfo;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.common.realmlist.RealmListEntry;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;

import java.time.LocalDateTime;

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
    public RaceInfo raceInfo;

    @SQLField(column = "gender")
    public GenderInfo gender;

    @SQLField(column = "class")
    public ClassInfo classInfo;


    @SQLField(column = "level")
    public int level = 0;

    @SQLField(column = "exp")
    public long exp = 0;

    @SQLField(column = "currency")
    public long currency = 0;


    @SQLField(column = "location")
    public int location;

    @SQLField(column = "position_x")
    public float positionX;

    @SQLField(column = "position_y")
    public float positionY;

    @SQLField(column = "position_z")
    public float positionZ;

    @SQLField(column = "orientation")
    public float orientation;


    @SQLField(column = "body")
    public byte[] body;


    @SQLField(column = "delete_flag")
    public int deleteFlag;

    @SQLField(column = "delete_after")
    public LocalDateTime deleteAfter;

    public CharacterInfo() {
        // Default constructor for SQLObjectWrapper
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id).put(realm.id)
            .put(firstName).put(lastName)
            .put(raceInfo.id).put(gender.toString().toLowerCase()).put(classInfo.id)
            .put(level).put(exp).put(currency).put(body)
            .toByteArray();
    }

    @Override
    public String toString() {
        return String.format(
            "CharacterInfo(ID=%d):LVL(%d):R(%s):C(%s): %s : %s %s",
            id, level, raceInfo, classInfo, gender, firstName, lastName
        );
    }
}
