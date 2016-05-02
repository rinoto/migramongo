package com.rinoto.migramongo;

/**
 * Extension of {@link com.rinoto.migramongo.MongoMigrationScript} to identify the first migration script.
 * <p>
 * The method {@link #getMigrationInfo()} returns an {@link com.rinoto.migramongo.InitialMigrationInfo} instead of a {@link com.rinoto.migramongo.MigrationInfo}
 * 
 * @author rinoto
 */
public interface InitialMongoMigrationScript extends MongoMigrationScript {

    /**
     * returns the information needed from <code>MigraMongo</code> to know when to execute the script.
     * <p>
     * see: {@link com.rinoto.migramongo.MongoMigrationScript#getMigrationInfo()}
     */
    InitialMigrationInfo getMigrationInfo();

}
