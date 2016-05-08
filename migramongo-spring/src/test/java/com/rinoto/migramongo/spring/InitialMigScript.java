package com.rinoto.migramongo.spring;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.InitialMigrationInfo;
import com.rinoto.migramongo.InitialMongoMigrationScript;

// @InitialMongoMigrationScript(version = "1")
public class InitialMigScript implements InitialMongoMigrationScript {

    @Override
    public InitialMigrationInfo getMigrationInfo() {
        return new InitialMigrationInfo("1");
    }

    @Override
    public void migrate(MongoDatabase db) {
        System.out.println("migrating initial");
    }

}
