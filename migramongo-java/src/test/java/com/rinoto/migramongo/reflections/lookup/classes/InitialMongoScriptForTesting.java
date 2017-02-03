package com.rinoto.migramongo.reflections.lookup.classes;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.InitialMigrationInfo;
import com.rinoto.migramongo.InitialMongoMigrationScript;

public class InitialMongoScriptForTesting implements InitialMongoMigrationScript {

    @Override
    public void migrate(MongoDatabase database) throws Exception {
        //nothing
    }

    @Override
    public InitialMigrationInfo getMigrationInfo() {
        return new InitialMigrationInfo("1");
    }

}
