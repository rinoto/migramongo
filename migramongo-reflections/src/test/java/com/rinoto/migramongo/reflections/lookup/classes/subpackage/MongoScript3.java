package com.rinoto.migramongo.reflections.lookup.classes.subpackage;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.MongoMigrationScript;

public class MongoScript3 implements MongoMigrationScript {

    @Override
    public MigrationInfo getMigrationInfo() {
        return new MigrationInfo("3", "4");
    }

    @Override
    public void migrate(MongoDatabase database) throws Exception {
        //nothing
    }

}
