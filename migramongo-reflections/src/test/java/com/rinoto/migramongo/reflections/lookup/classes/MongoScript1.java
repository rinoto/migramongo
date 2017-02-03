package com.rinoto.migramongo.reflections.lookup.classes;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.MongoMigrationScript;

public class MongoScript1 implements MongoMigrationScript {

    @Override
    public MigrationInfo getMigrationInfo() {
        return new MigrationInfo("1", "2");
    }

    @Override
    public void migrate(MongoDatabase database) throws Exception {
        //nothing
    }

}
