package com.rinoto.migramongo.dao;

import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.MigrationRun;

public interface MigrationHistoryService {

    /**
     * Gets the last migration that was applied, by checking the <code>createdAt</code> field of the entries
     *
     * @return last mig entry
     */
    MigrationEntry getLastMigrationApplied();

    /**
     * It changes the status of an existing <code>MigrationEntry</code> to {@link com.rinoto.migramongo.MigraMongoStatus.MigrationStatus#OK}
     *
     * @param migrationEntry entry to change
     * @return migEntry just changed
     */
    MigrationEntry setMigrationStatusToFinished(MigrationEntry migrationEntry);

    /**
     * It changes the status of an existing <code>MigrationEntry</code> to {@link com.rinoto.migramongo.MigraMongoStatus.MigrationStatus#OK} and adds the <code>repaired=true</code> to the entry.
     *
     * @param migrationEntry entry to change
     * @return migEntry just changed
     */
    MigrationEntry setMigrationStatusToManuallyRepaired(MigrationEntry migrationEntry);

    /**
     * It changes the status of an existing <code>MigrationEntry</code> to {@link com.rinoto.migramongo.MigraMongoStatus.MigrationStatus#ERROR}
     *
     * @param migrationEntry entry to change
     * @param e the exception
     * @return migEntry just changed
     */
    MigrationEntry setMigrationStatusToFailed(MigrationEntry migrationEntry, Exception e);

    /**
     * Inserts a new <code>MigrationEntry</code> with the information of the <code>MigrationInfo</code> parameter
     *
     * @param migrationInfo info to insert
     * @return migEntry just updated
     */
    MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo);

    /**
     * Inserts a new <code>MigrationEntry</code> with the information of the <code>MigrationInfo</code> parameter, and sets it to <code>skipped</code>, i.e. the migration was not executed.
     *
     * @param migrationInfo info to insert
     * @return migEntry just updated
     */
    MigrationEntry insertMigrationStatusSkipped(MigrationInfo migrationInfo);

    /**
     * It finds a <code>MigrationEntry</code> that matches the <code>fromVersion</code> and <code>toVersion</code> parameters
     *
     * @param fromVersion fromVersion param
     * @param toVersion toVersion param
     * @return migEntry found
     */
    MigrationEntry findMigration(String fromVersion, String toVersion);

    /**
     * Finds all migrations applied since the specified version, order asc
     *
     * @param fromVersion the version we want to get the entries from
     * @return all migrations applied since the specified version
     */
    Iterable<MigrationEntry> findMigrations(String fromVersion);

    /**
     * Returns all the migration entries that have been applied, ordered by date asc.
     *
     * @return all migrations applied
     */
    Iterable<MigrationEntry> getAllMigrationsApplied();

    MigrationEntry addRunToMigrationEntry(MigrationEntry entry, MigrationRun run);

    MigrationEntry setLastReRunToFinished(MigrationEntry migrationEntry);

    MigrationEntry setLastReRunToFailed(MigrationEntry migrationEntry, Exception e);
}
