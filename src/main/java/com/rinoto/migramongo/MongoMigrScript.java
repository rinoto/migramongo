package com.rinoto.migramongo;

public interface MongoMigrScript {

	MigrationInfo getMigrationInfo();

	void migrate();

}
