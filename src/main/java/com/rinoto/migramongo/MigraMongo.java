package com.rinoto.migramongo;

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

    public void init() {
        System.out.println("appContex: " + appContext);
        final MongoCollection<Document> collection = this.database.getCollection(MIGRAMONGO_COLLECTION);
        if (collection == null) {
            this.database.createCollection(MIGRAMONGO_COLLECTION);
        }
    }

}
