package com.rinoto.migramongo;

import java.util.Collection;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MigraMongo {

    private static final String MIGRAMONGO_COLLECTION = "_migramongo";
    private final MongoDatabase database;

    @Autowired
    private ApplicationContext appContext;

    public MigraMongo(MongoDatabase database) {
        this.database = database;
    }

    public MigraMongoStatus migrate() {
        final Document lastMigrationApplied = getLastMigrationApplied();
        if (lastMigrationApplied == null) {
            return new MigraMongoStatus("ERROR", "no last migration script found");
        }
        return MigraMongoStatus.OK;
    }

    public MongoCollection<Document> getMigramongoCollection() {
        MongoCollection<Document> collection = this.database.getCollection(MIGRAMONGO_COLLECTION);
        if (collection == null) {
            this.database.createCollection(MIGRAMONGO_COLLECTION);
            collection = this.database.getCollection(MIGRAMONGO_COLLECTION);
        }
        return collection;
    }

    public Document getLastMigrationApplied() {
        return getMigramongoCollection().find().sort(new Document("createdAt", 1)).first();
    }

    public Collection<Object> getMigrationScripts() {
        final Map<String, Object> beansWithAnnotation = appContext.getBeansWithAnnotation(MongoMigrationScript.class);
        return beansWithAnnotation.values();
    }

}
