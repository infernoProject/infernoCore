package ru.infernoproject.worldd.characters.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.auth.sql.Account;
import ru.infernoproject.worldd.data.sql.ClassInfo;
import ru.infernoproject.worldd.data.sql.GenderInfo;
import ru.infernoproject.worldd.data.sql.RaceInfo;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.common.utils.ByteWrapper;

@SQLObject(table = "characters", database = "characters")
public class CharacterInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "account")
    public Account account;

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
    }

    public CharacterInfo(String firstName, String lastName, RaceInfo raceInfo, GenderInfo genderInfo, ClassInfo classInfo) {
        this.firstName = firstName;
        this.lastName = lastName;

        this.raceId = raceInfo.getId();
        this.gender = genderInfo.getResource();
        this.classId = classInfo.getId();
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id)
            .put(firstName).put(lastName)
            .put(raceId).put(gender).put(classId)
            .put(level).put(exp).put(currency)
            .toByteArray();
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getRaceId() {
        return raceId;
    }

    public String getGender() {
        return gender;
    }

    public int getClassId() {
        return classId;
    }

    @Override
    public String toString() {
        return String.format(
            "CharacterInfo(ID=%d):LVL(%d):R(%d):C(%d): %s : %s %s",
            id, level, raceId, classId, gender, firstName, lastName
        );
    }
}
