package ru.linachan.inferno.common.db;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import ru.linachan.inferno.InfernoServer;

public class InfernoDataBase {

    private MongoClient dbClient;
    private MongoDatabase dbInstance;

    public InfernoDataBase() {
        dbClient = new MongoClient(
            new MongoClientURI(InfernoServer.CONFIG.getString("db.uri", "mongodb://127.0.0.1:27017/"))
        );

        dbInstance = dbClient.getDatabase(InfernoServer.CONFIG.getString("db.name", "inferno"));
    }

    public MongoDatabase getDataBase() {
        return dbInstance;
    }

    public MongoCollection<Document> getCollection(String collection) {
        return dbInstance.getCollection(collection);
    }
}
