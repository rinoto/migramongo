package com.rinoto.migramongo;

import com.mongodb.client.MongoDatabase;

public interface MongoMigrationScript {

    MigrationInfo getMigrationInfo();

    void migrate(MongoDatabase database);

}
