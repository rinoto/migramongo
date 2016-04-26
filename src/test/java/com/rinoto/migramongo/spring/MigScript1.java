package com.rinoto.migramongo.spring;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.MongoMigrationScript;

// @MongoMigrationScript(from = "1", to = "2")
public class MigScript1 implements MongoMigrationScript {

    @Override
    public void migrate(MongoDatabase db) {
        System.out.println("migrating script 1");
    }

    @Override
    public MigrationInfo getMigrationInfo() {
        return new MigrationInfo("1", "2");
    }

}
