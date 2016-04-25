package com.rinoto.migramongo;

//@MongoMigrationScript(from = "1", to = "2")
public class MigScript1 implements MongoMigrScript {

	public void migrate() {
		System.out.println("migrating");
	}

	@Override
	public MigrationInfo getMigrationInfo() {
		return new MigrationInfo("1", "2");
	}

}
