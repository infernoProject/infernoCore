package ru.linachan.inferno.world;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.inferno.InfernoServer;
import ru.linachan.inferno.common.Utils;
import ru.linachan.inferno.common.session.Session;
import ru.linachan.inferno.world.interest.InterestManager;
import ru.linachan.inferno.common.vector.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class World {

    private final Vector2<Double, Double> dimensions;
    private final Vector2<Integer, Integer> regionCount;

    private final Vector2<Double, Double> regionSize;

    private Map<Vector2<Integer, Integer>, Region> regionMap = new ConcurrentHashMap<>();
    private InterestManager interestManager;

    private static Logger logger = LoggerFactory.getLogger(World.class);
    private MongoCollection<Document> players;

    public World(Vector2<Double, Double> size, Vector2<Integer, Integer> regions) {
        dimensions = size;
        regionCount = regions;

        regionSize = new Vector2<>(
            size.getX() / regionCount.getX(),
            size.getY() / regionCount.getY()
        );

        interestManager = new InterestManager(this);

        for (int x = 0; x <= regionCount.getX(); x++) {
            for (int y = 0; y <= regionCount.getY(); y++) {
                Vector2<Integer, Integer> regionPosition = new Vector2<>(x, y);
                regionMap.put(regionPosition, new Region(regionPosition));
            }
        }

        players = InfernoServer.DB.getCollection("players");
    }

    public void calculatePlayerAreaOfInterest(Player player) {
        Vector2<Double, Double> pos = securePosition(player.getPosition());
        Vector2<Double, Double> aoi = player.getInterestAreaSize();

        Vector2<Integer, Integer> topLeft = getRegionByPosition(new Vector2<>(pos.getX() - aoi.getX(), pos.getY() - aoi.getY()));
        Vector2<Integer, Integer> bottomRight = getRegionByPosition(new Vector2<>(pos.getX() + aoi.getX(), pos.getY() + aoi.getY()));

        player.getInterestArea().setArea(topLeft, bottomRight);
    }

    public void updatePlayer(Player player) {
        Vector2<Double, Double> pos = securePosition(player.getPosition());
        player.setPosition(pos);

        calculatePlayerAreaOfInterest(player);

        Vector2<Integer, Integer> regionPosition = getRegionByPosition(pos);
        Region region = regionMap.get(regionPosition);

        interestManager.onUpdate(player, region);

        players.updateOne(
            new Document("uuid", player.getID().toString()),
            new Document("$set", player.toBSON())
        );
    }

    private Vector2<Double, Double> securePosition(Vector2<Double, Double> position) {
        return new Vector2<>(
            Math.min(Math.max(0, position.getX()), dimensions.getX()),
            Math.min(Math.max(0, position.getY()), dimensions.getY())
        );
    }

    private Vector2<Integer, Integer> getRegionByPosition(Vector2<Double, Double> position) {
        position = securePosition(position);

        int regionX = (int) (position.getX() / regionSize.getX());
        int regionY = (int) (position.getY() / regionSize.getY());

        return new Vector2<>(regionX, regionY);
    }

    public Player getPlayer(Session session) {
        Document playerData = players.find(new Document("user", session.getUser())).first();
        Player player;

        if (playerData != null) {
            player = Player.fromBSON(playerData);
        } else {
            Document newPlayerData = new Document("user", session.getUser())
                .append("name", session.getUser())
                .append("position_x", 0.0)
                .append("position_y", 0.0)
                .append("area_x", 10.0)
                .append("area_y", 10.0)
                .append("uuid", UUID.randomUUID().toString());
            players.insertOne(newPlayerData);
            player = Player.fromBSON(newPlayerData);
        }

        calculatePlayerAreaOfInterest(player);

        return player;
    }

    public List<Player> getPlayerData() {
        List<Player> playerList = new ArrayList<>();

        for (Document playerData: players.find()) {
            Player player = Player.fromBSON(playerData);
            calculatePlayerAreaOfInterest(player);
            playerList.add(player);
        }

        return playerList;
    }
}
