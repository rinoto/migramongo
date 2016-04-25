package com.rinoto.migramongo;

//@InitialMongoMigrationScript(version = "1")
public class InitialMigScript implements InitialMongoMigrScript {

	@Override
	public InitialMigrationInfo getMigrationInfo() {
		return new InitialMigrationInfo("1");
	}

	public void migrate() {
		System.out.println("migrating");
	}

}
