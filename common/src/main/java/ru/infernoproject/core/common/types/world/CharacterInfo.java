package ru.infernoproject.core.common.types.world;

import ru.infernoproject.core.common.utils.ByteArray;
import ru.infernoproject.core.common.utils.ByteConvertible;
import ru.infernoproject.core.common.utils.ByteWrapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CharacterInfo implements ByteConvertible {

    private int id;

    private String firstName;
    private String lastName;

    private int raceId;
    private String gender;
    private int classId;

    private int level = 0;
    private long exp = 0;
    private long currency = 0;

    public CharacterInfo(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt("id");

        firstName = resultSet.getString("firstName");
        lastName = resultSet.getString("lastName");

        raceId = resultSet.getInt("race");
        gender = resultSet.getString("gender");
        classId = resultSet.getInt("class");

        level = resultSet.getInt("level");
        exp = resultSet.getLong("exp");
        currency = resultSet.getLong("currency");
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
