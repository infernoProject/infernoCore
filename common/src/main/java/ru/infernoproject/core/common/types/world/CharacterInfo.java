package ru.infernoproject.core.common.types.world;

import ru.infernoproject.core.common.db.sql.SQLField;
import ru.infernoproject.core.common.db.sql.SQLObject;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.core.common.types.auth.Account;
import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.common.utils.ByteWrapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@SQLObject(table = "characters", database = "characters")
public class CharacterInfo implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "account", type = Account.class)
    public Account account;

    @SQLField(column = "first_name", type = String.class)
    public String firstName;

    @SQLField(column = "last_name", type = String.class)
    public String lastName;

    @SQLField(column = "race", type = Integer.class)
    public int raceId;

    @SQLField(column = "gender", type = String.class)
    public String gender;

    @SQLField(column = "class", type = Integer.class)
    public int classId;

    @SQLField(column = "level", type = Integer.class)
    public int level = 0;

    @SQLField(column = "exp", type = Long.class)
    public long exp = 0;

    @SQLField(column = "currency", type = Long.class)
    public long currency = 0;

    public CharacterInfo() {

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
