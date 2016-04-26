package com.rinoto.migramongo;

public interface InitialMongoMigrationScript extends MongoMigrationScript {

	InitialMigrationInfo getMigrationInfo();

}
