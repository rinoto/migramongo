package com.rinoto.migramongo;

@InitialMongoMigrationScript(version = "1")
public class InitialMigScript {

    public void migrate() {
        System.out.println("migrating");
    }

}
