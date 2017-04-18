package ru.infernoproject.common.data.sql;

public class GenderInfo {

    private String name;
    private String resource;

    public static final GenderInfo MALE = new GenderInfo("Male", "male");
    public static final GenderInfo FEMALE = new GenderInfo("Female", "female");

    public GenderInfo(String name, String resource) {
        this.name = name;
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public String toString() {
        return name;
    }
}
