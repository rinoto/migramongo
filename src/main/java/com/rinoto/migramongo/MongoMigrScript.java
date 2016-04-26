package com.rinoto.migramongo;

import com.mongodb.client.MongoDatabase;

public interface MongoMigrScript {

    MigrationInfo getMigrationInfo();

    void migrate(MongoDatabase database);

}
