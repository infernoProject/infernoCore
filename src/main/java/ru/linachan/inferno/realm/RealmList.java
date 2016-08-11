package ru.linachan.inferno.realm;

import org.bson.Document;
import ru.linachan.inferno.InfernoServer;

import java.util.ArrayList;
import java.util.List;

public class RealmList {

    public static List<RealmServer> getRealmList() {
        List<RealmServer> realmList = new ArrayList<>();

        for (Document realm: InfernoServer.DB.getCollection("realms").find(new Document("active", true))) {
            RealmServer realmServer = RealmServer.fromBSON(realm);
            realmList.add(realmServer);
        }

        return realmList;
    }
}
