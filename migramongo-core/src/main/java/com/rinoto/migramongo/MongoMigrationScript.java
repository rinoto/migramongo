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

}
