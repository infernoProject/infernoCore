package ru.linachan.inferno.world;

import org.bson.Document;
import ru.linachan.inferno.world.interest.InterestArea;
import ru.linachan.inferno.world.interest.InterestObject;
import ru.linachan.inferno.common.vector.Vector2;

import java.util.UUID;

public class Player implements InterestObject {
    private Vector2<Double, Double> position;
    private Vector2<Double, Double> interestAreaSize;

    private UUID id;
    private String name;
    private String user;

    private InterestArea interestArea;

    private Player(UUID playerID, String playerName) {
        id = playerID;
        name = playerName;

        interestArea = new InterestArea();
    }

    public static Player fromBSON(Document playerData) {
        Player player = new Player(
            UUID.fromString(playerData.getString("uuid")),
            playerData.getString("name")
        );

        player.user = playerData.getString("user");
        player.position = new Vector2<>(
            playerData.getDouble("position_x"),
            playerData.getDouble("position_y")
        );

        player.interestAreaSize = new Vector2<>(
            playerData.getDouble("area_x"),
            playerData.getDouble("area_y")
        );

        return player;
    }

    @Override
    public Vector2<Double, Double> getPosition() {
        return position;
    }

    public Vector2<Double, Double> getInterestAreaSize() {
        return interestAreaSize;
    }

    public void setPosition(Vector2<Double, Double> position) {
        this.position = position;
    }

    public void setInterestAreaSize(Vector2<Double, Double> interestAreaSize) {
        this.interestAreaSize = interestAreaSize;
    }

    @Override
    public UUID getID() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object target) {
        if (target == null) return false;
        if (target == this) return true;
        if (!(target instanceof Player)) return false;

        UUID targetId = ((Player) target).getID();
        return id.equals(targetId);
    }

    public InterestArea getInterestArea() {
        return interestArea;
    }

    @Override
    public String getName() {
        return name;
    }

    public Document toBSON() {
        return new Document("user", user)
            .append("uuid", id.toString())
            .append("name", name)
            .append("position_x", position.getX())
            .append("position_y", position.getY())
            .append("area_x", interestAreaSize.getX())
            .append("area_y", interestAreaSize.getY());
    }
}
