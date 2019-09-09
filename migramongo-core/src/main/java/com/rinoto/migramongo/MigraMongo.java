package com.rinoto.migramongo;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.dao.LockService;
import com.rinoto.migramongo.dao.MigrationHistoryService;
import com.rinoto.migramongo.lookup.ScriptLookupService;

/**
 * Performs the migration of the scripts
 * 
 * @author rinoto
 */
public class MigraMongo {

    private static final Logger logger = LoggerFactory.getLogger(MigraMongo.class);

    private final ScriptLookupService scriptLookupService;
    private final MigrationHistoryService migrationHistoryService;
    private final LockService lockService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private MongoDatabase database;

    public MigraMongo(
            MongoDatabase database,
            MigrationHistoryService migrationEntryService,
            LockService lockService,
            ScriptLookupService scriptLookupService) {
        this.database = database;
        this.migrationHistoryService = migrationEntryService;
        this.lockService = lockService;
        this.scriptLookupService = scriptLookupService;
    }

    /**
     * It returns the migrations that would be applied, if the migration would be performed, but it doesn't actually migrate anything
     * 
     * @return
     */
    public MigraMongoStatus dryRun() {
        try {
            final List<MongoMigrationScript> migrationScriptsToApply = findMigrationScriptsToApply();
            final MigraMongoStatus status = MigraMongoStatus.ok();
            migrationScriptsToApply.stream().forEach(ms -> {
                // dummy migration entries, emulating a migration that was not
                // executed
                final MigrationEntry migEntry = new MigrationEntry();
                migEntry.setFromVersion(ms.getMigrationInfo().getFromVersion());
                migEntry.setToVersion(ms.getMigrationInfo().getToVersion());
                migEntry.setInfo("Dummy migration entry - migration has not been performed");
                status.addEntry(migEntry);
            });
            return status;
        } catch (MongoMigrationException e) {
            return e.getStatus();
        }
    }

    /**
     * Performs the migration from the last entry in the DB, until the last available {@link com.rinoto.migramongo.MongoMigrationScript} found.
     * <p>
     * If there are no migrations in the source code, and there is no
     * 
     * @return the status
     */
    public MigraMongoStatus migrate() {
        final boolean lockAcquired = lockService.acquireLock();
        if ( !lockAcquired) {
            return MigraMongoStatus.lockNotAcquired();
        }
        try {
            final List<MongoMigrationScript> migrationScriptsToApply = findMigrationScriptsToApply();
            return migrate(migrationScriptsToApply);
        } catch (MongoMigrationException e) {
            logger.error("Exception caught while migrating", e);
            return e.getStatus();
        } finally {
            lockService.releaseLock();
        }
    }

    /**
     * Same as migrate, but asynchronous.
     * <ul>
     * <li>if there is nothing to migrate, a status with OK will be returned
     * <li>if there are items to migrate, a status with IN_PROGRESS and the items that will be migrated will be returned
     * </ul>
     * 
     * @return the status of the migration - will always be ok, as the migration runs asynchronously
     */
    public MigraMongoStatus migrateAsync() {
        final MigraMongoStatus dryRunStatus = dryRun();
        if (dryRunStatus.migrationsApplied.isEmpty()) {
            // return OK status - no thread will be started
            return dryRunStatus;
        }
        logger.debug("Migration will be performed asynchronously");
        executorService.submit(() -> {
            logger.debug("Running migration in a separate thread");
            final MigraMongoStatus status = migrate();
            logger.debug("Migration in a separate thread returned status {}", status);
        });
        // return IN_PROGRESS status
        dryRunStatus.status = MigrationStatus.IN_PROGRESS;
        return dryRunStatus;
    }

    /**
     * Gets the status of the applied migrations since the specified version
     * <p>
     * if fromVersion is null, it will get all applied migrations
     * 
     * @param fromVersion the version we want to get the status from
     * @return status of the applied migrations since the specified version
     */
    public MigraMongoStatus status(String fromVersion) {
        final Iterable<MigrationEntry> migrations = migrationHistoryService.findMigrations(fromVersion);
        for (MigrationRun migEntry : migrations) {
            if (migEntry.getStatus() == MigrationStatus.ERROR) {
                return MigraMongoStatus
                    .error("At least one migration script threw an error. Check individual entries")
                    .withEntries(toList(migrations));
            }
            if (migEntry.getStatus() == MigrationStatus.IN_PROGRESS) {
                return MigraMongoStatus
                    .inProgress("At least one migration script is in progress. Check individual entries")
                    .withEntries(toList(migrations));
            }
        }
        // if we are here, it means that everything went ok
        return MigraMongoStatus.ok().withEntries(toList(migrations));
    }

    private MigraMongoStatus migrate(List<MongoMigrationScript> migrationScriptsToApply) {
        logger.debug("Running migration on {} scripts", migrationScriptsToApply.size());
        final MigraMongoStatus status = MigraMongoStatus.ok();
        boolean isInitialMigration = migrationScriptsToApply
            .stream()
            .filter(ms -> InitialMongoMigrationScript.class.isInstance(ms))
            .findFirst()
            .isPresent();
        for (MongoMigrationScript migScriptToApply : migrationScriptsToApply) {
            final MigrationEntry migEntry = executeMigrationScript(migScriptToApply, isInitialMigration);
            status.addEntry(migEntry);
            if (migEntry.getStatus() == MigrationStatus.ERROR) {
                status.status = MigrationStatus.ERROR;
                status.message = "MigrationScript with fromVersion '" +
                    migEntry.getFromVersion() +
                    "' and toVersion '" +
                    migEntry.getToVersion() +
                    "' failed with message: " +
                    migEntry.getStatusMessage();
                return status;
            }
        }
        logger.debug("Migration performed with status {}", status);
        return status;
    }

    private MigrationEntry getLastMigrationApplied() throws MongoMigrationException {
        final MigrationEntry lastMigrationApplied = migrationHistoryService.getLastMigrationApplied();
        if (isInInconsistentState(lastMigrationApplied)) {
            throw new MongoMigrationException(
                new MigraMongoStatus(
                    MigrationStatus.ERROR,
                    "Last Migration is in status " +
                        lastMigrationApplied.getStatus() +
                        ": " +
                        lastMigrationApplied +
                        ". Cannot apply any migration until the entry gets fixed"));
        }
        return lastMigrationApplied;
    }

    private List<MongoMigrationScript> findMigrationScriptsToApply() throws MongoMigrationException {
        final MigrationEntry lastMigrationApplied = getLastMigrationApplied();

        final List<MongoMigrationScript> migrationScriptsToApply = new ArrayList<>();
        final String fromVersion;
        if (lastMigrationApplied == null) {
            final InitialMongoMigrationScript initialMigrationScript = scriptLookupService.findInitialScript();
            if (initialMigrationScript == null) {
                //if no initial found, we do not migrate anything
                return Collections.emptyList();
                //                throw new MongoMigrationException(
                //                    new MigraMongoStatus(
                //                        MigrationStatus.ERROR,
                //                        "no last migration script found, and no initial migration script provided!"));
            }
            migrationScriptsToApply.add(initialMigrationScript);
            fromVersion = initialMigrationScript.getMigrationInfo().getToVersion();
        } else {
            fromVersion = lastMigrationApplied.getToVersion();
        }
        migrationScriptsToApply.addAll(getMigrationScriptsToApply(fromVersion));
        return migrationScriptsToApply;
    }

    /**
     * 'repairs' an entry in the <i>_migramongo</i> collection that has been marked as <code>ERROR</code> or has hanged in <code>IN_PROGRESS</code> status.
     * <p>
     * the entry gets defined by the <code>fromVersion</code> and <code>toVersion</code> parameters
     * <p>
     * if the entry does not exist, or it was not in one of the allowed states for repairing, an error status will be thrown
     * 
     * @param fromVersion fromVersion
     * @param toVersion toVersion
     * @return status
     */
    public MigraMongoStatus repair(String fromVersion, String toVersion) {
        final MigrationEntry migrationEntry = migrationHistoryService.findMigration(fromVersion, toVersion);
        if (migrationEntry == null) {
            return MigraMongoStatus
                .error(
                    "No migration entry found for fromVersion '" + fromVersion + "' and toVersion '" + toVersion + "'");
        }
        if (migrationEntry.getStatus() == MigrationStatus.OK) {
            return MigraMongoStatus
                .error(
                    "Migration entry with fromVersion '" +
                        fromVersion +
                        "' and toVersion '" +
                        toVersion +
                        "' has already status '" +
                        migrationEntry.getStatus() +
                        "'. Nothing will be done");
        }
        final MigrationStatus previousStatus = migrationEntry.getStatus();
        final MigrationEntry correctedMigrationEntry = migrationHistoryService
            .setMigrationStatusToFinished(migrationEntry);
        final MigraMongoStatus status = MigraMongoStatus
            .ok(
                "Status of migrationEntry " +
                    migrationEntry +
                    " changed from '" +
                    previousStatus +
                    "' to '" +
                    MigrationStatus.OK +
                    "'");
        status.addEntry(correctedMigrationEntry);
        return status;
    }

    public MigraMongoStatus rerun(String fromVersion, String toVersion) {
        final MigrationEntry migrationEntry = migrationHistoryService.findMigration(fromVersion, toVersion);
        if (migrationEntry == null) {
            return MigraMongoStatus
                .error(
                    "No migration entry found for fromVersion '" +
                        fromVersion +
                        "' and toVersion '" +
                        toVersion +
                        "' in the migration history");
        }
        final Optional<MongoMigrationScript> migScriptOpt = scriptLookupService
            .findMongoScripts()
            .stream()
            .filter(
                script -> fromVersion.equals(script.getMigrationInfo().getFromVersion()) &&
                    toVersion.equals(script.getMigrationInfo().getToVersion()))
            .findFirst();
        if ( !migScriptOpt.isPresent()) {
            return MigraMongoStatus
                .error(
                    "No migration script found for fromVersion '" +
                        fromVersion +
                        "' and toVersion '" +
                        toVersion +
                        "'");
        }

        final MongoMigrationScript mongoMigrationScript = migScriptOpt.get();
        final MigrationRun migrationRun = new MigrationRun();
        try {
            mongoMigrationScript.migrate(database);
            final MigrationEntry migrationEntryWithRun = migrationHistoryService
                .addRunToMigrationEntry(
                    migrationEntry,
                    migrationRun.complete(MigrationStatus.OK, "Migration completed correctly"));
            return MigraMongoStatus
                .ok("Re-run of Migration fromVersion " + fromVersion + " toVersion " + toVersion + " run successfully")
                .addEntry(migrationEntryWithRun);

        } catch (Exception e) {
            logger
                .error(
                    "Error when re-running migration fromVersion " +
                        fromVersion +
                        " toVersion " +
                        toVersion +
                        ": " +
                        e.getMessage());
            final MigrationEntry migrationEntryWithRun = migrationHistoryService
                .addRunToMigrationEntry(migrationEntry, migrationRun.complete(MigrationStatus.ERROR, e.getMessage()));
            return MigraMongoStatus
                .error(
                    "Error when re-running migration fromVersion " +
                        fromVersion +
                        " toVersion " +
                        toVersion +
                        ": " +
                        e.getMessage())
                .addEntry(migrationEntryWithRun);
        }
    }

    private boolean isInInconsistentState(MigrationRun mig) {
        return mig != null && mig.getStatus() != MigrationStatus.OK;
    }

    private MigrationEntry executeMigrationScript(MongoMigrationScript migrationScript, boolean isInitialMigration) {
        if ( !InitialMongoMigrationScript.class.isInstance(migrationScript) &&
            isInitialMigration &&
            migrationScript.includedInInitialMigrationScript()) {
            //special case when we are in the initial migration, and this script is already included in it
            return migrationHistoryService.insertMigrationStatusSkipped(migrationScript.getMigrationInfo());
        }

        final MigrationEntry migrationEntry = migrationHistoryService
            .insertMigrationStatusInProgress(migrationScript.getMigrationInfo());
        try {
            migrationScript.migrate(database);
            return migrationHistoryService.setMigrationStatusToFinished(migrationEntry);
        } catch (Exception e) {
            return migrationHistoryService.setMigrationStatusToFailed(migrationEntry, e);
        }
    }

    private List<MongoMigrationScript> getMigrationScriptsToApply(String version) {
        final Collection<MongoMigrationScript> migScripts = scriptLookupService.findMongoScripts();
        final List<MongoMigrationScript> migScriptsToApply = findMigScriptsToApply(version, migScripts);
        return migScriptsToApply;
    }

    private List<MongoMigrationScript> findMigScriptsToApply(
            String version,
            Collection<MongoMigrationScript> allMigrationScripts) {
        if (allMigrationScripts.isEmpty()) {
            return new ArrayList<>();
        }
        final List<MongoMigrationScript> candidates = new ArrayList<>();
        final List<MongoMigrationScript> rest = new ArrayList<>();
        for (MongoMigrationScript ms : allMigrationScripts) {
            if (ms.getMigrationInfo().getFromVersion().equals(version)) {
                candidates.add(ms);
            } else {
                rest.add(ms);
            }
        }
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        if (candidates.size() > 1) {
            throw new IllegalStateException(
                "There is more than one script with fromVersion " + version + ": " + allMigrationScripts);
        }
        final MongoMigrationScript nextMigrationScript = candidates.get(0);
        final List<MongoMigrationScript> nextMigScriptsRec = findMigScriptsToApply(
            nextMigrationScript.getMigrationInfo().getToVersion(),
            rest);
        candidates.addAll(nextMigScriptsRec);
        return candidates;
    }

    /**
     * Returns the migration entries that have been applied
     * 
     * @return the list of migration entries
     */
    public List<MigrationEntry> getMigrationEntries() {
        return toList(migrationHistoryService.getAllMigrationsApplied());
    }

    /**
     * In case that the locks are corrupted, we can always re-init (destroy) them
     */
    public void destroyLocks() {
        lockService.destroyLock();
    }

    private <T extends Object> List<T> toList(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

}
