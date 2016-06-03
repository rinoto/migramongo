package com.rinoto.migramongo.spring;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.InitialMigrationInfo;
import com.rinoto.migramongo.InitialMongoMigrationScript;

/**
 * Used to produce an error - there cannot be more than 1
 * InitialMigrationScript!
 * 
 * @author ruben
 *
 */
public class SecondInitialMigrationScript implements InitialMongoMigrationScript {

	@Override
	public InitialMigrationInfo getMigrationInfo() {
		return new InitialMigrationInfo("1");
	}

	@Override
	public void migrate(MongoDatabase db) {
		System.out.println("migrating initial");
	}

}
