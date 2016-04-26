package com.rinoto.migramongo;

import com.mongodb.client.MongoDatabase;

// @MongoMigrationScript(from = "1", to = "2")
public class MigScript1 implements MongoMigrScript {

    @Override
    public void migrate(MongoDatabase db) {
        System.out.println("migrating script 1");
    }

    @Override
    public MigrationInfo getMigrationInfo() {
        return new MigrationInfo("1", "2");
    }

}
