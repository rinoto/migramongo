package com.rinoto.migramongo.dao;

import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;

public interface MigrationHistoryService {

    /**
     * Gets the last migration that was applied, by checking the <code>createdAt</code> field of the entries
     */
    MigrationEntry getLastMigrationApplied();

    /**
     * It changes the status of an existing <code>MigrationEntry</code> to {@link com.rinoto.migramongo.MigraMongoStatus.MigrationStatus.OK}
     */
    MigrationEntry setMigrationStatusToFinished(MigrationEntry migrationEntry);

    /**
     * It changes the status of an existing <code>MigrationEntry</code> to {@link com.rinoto.migramongo.MigraMongoStatus.MigrationStatus.OK} and adds the <code>repaired=true</code> to the entry.
     */
    MigrationEntry setMigrationStatusToManuallyRepaired(MigrationEntry migrationEntry);

    /**
     * It changes the status of an existing <code>MigrationEntry</code> to {@link com.rinoto.migramongo.MigraMongoStatus.MigrationStatus.ERROR}
     */
    MigrationEntry setMigrationStatusToFailed(MigrationEntry migrationEntry, Exception e);

    /**
     * Inserts a new <code>MigrationEntry</code> with the information of the <code>MigrationInfo</code> parameter
     */
    MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo);

    /**
     * It finds a <code>MigrationEntry</code> that matches the <code>fromVersion</code> and <code>toVersion</code> parameters
     */
    MigrationEntry findMigration(String fromVersion, String toVersion);

}
