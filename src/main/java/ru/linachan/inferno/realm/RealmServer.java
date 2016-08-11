package ru.linachan.inferno.realm;

import org.bson.Document;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class RealmServer {

    private String host;
    private Integer port;
    private String name;
    private String type;
    private Boolean isActive;

    private RealmServer(String realmHost, Integer realmPort, String realmName, String realmType, Boolean realmIsActive) {
        host = realmHost;
        port = realmPort;
        name = realmName;
        type = realmType;
        isActive = realmIsActive;
    }

    public static RealmServer fromBSON(Document realm) {
        return new RealmServer(
            realm.getString("host"),
            (int) Math.round(realm.getDouble("port")),
            realm.getString("name"),
            realm.getString("type"),
            realm.getBoolean("active")
        );
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Boolean isActive() {
        return isActive;
    }

    public byte[] getBytes() {
        ByteBuffer realmBuffer = ByteBuffer.allocate(17 + name.getBytes().length + type.getBytes().length);

        try {
            realmBuffer.put(Inet4Address.getByName(host).getAddress());
        } catch (UnknownHostException e) {
            realmBuffer.put(new byte[4]);
        }

        realmBuffer.putInt(port);
        realmBuffer.put((byte) ((isActive) ? 0x01 : 0x00));
        realmBuffer.putInt(name.getBytes().length);
        realmBuffer.put(name.getBytes());
        realmBuffer.putInt(type.getBytes().length);
        realmBuffer.put(type.getBytes());

        return realmBuffer.array();
    }

    @Override
    public String toString() {
        return String.format("Realm[%s](%s:%d)", name, host, port);
    }
}
