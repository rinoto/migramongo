package com.rinoto.migramongo;

import com.mongodb.client.MongoDatabase;

// @InitialMongoMigrationScript(version = "1")
public class InitialMigScript implements InitialMongoMigrScript {

    @Override
    public InitialMigrationInfo getMigrationInfo() {
        return new InitialMigrationInfo("1");
    }

    @Override
    public void migrate(MongoDatabase db) {
        System.out.println("migrating initial");
    }

}
