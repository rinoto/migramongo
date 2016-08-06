package com.rinoto.migramongo;

import com.mongodb.client.MongoDatabase;

/**
 * Interface that the migration scripts must implement.
 * <p>
 * <code>MigraMongo</code> will search for Spring Services implementing this interface, and will execute them when needed.
 * 
 * @author rinoto
 */
public interface MongoMigrationScript {

    /**
     * Returns the <code>MigrationInfo</code> object with the information needed on how to migrate the script (<code>from</code> and <code>to</code> versions)
     * 
     * @return the migrationInfo
     */
    MigrationInfo getMigrationInfo();

    /**
     * It performs the actual migration. This method will be called by <code>MigraMongo</code>, and the status (ok, failed) will be written to the <code>_migramongo</code> collection in the DB
     * 
     * @param database mongodb to use
     */
    void migrate(MongoDatabase database) throws Exception;

    /**
     * If true, this MigrationScript has been included in the initial one.
     * <ul>
     * <li>If <code>true</code> in a fresh new installation with already existing MigrationScripts <b>in code</b>, this Script will be added to the History, but <b>will not be executed</b>, i.e. it will be marked as <i>skipped</i>, because its code is already contained in the initial one.
     * <li>If <code>false</code>, the migration script will be executed normally in a new installation.
     * </ul>
     * Default is <code>true</code>
     * 
     * @return true if the code of this script has already been included in the initial script
     */
    default boolean includedInInitialMigrationScript() {
        return true;
    }

}
