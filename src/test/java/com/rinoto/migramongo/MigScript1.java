package com.rinoto.migramongo;

@MongoMigrationScript(from = "1", to = "2")
public class MigScript1 {

    public void migrate() {
        System.out.println("migrating");
    }

}
