package com.rinoto.migramongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
	private final MigrationHistoryService migrationEntryService;
	private final LockService lockService;
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private MongoDatabase database;

	public MigraMongo(MongoDatabase database, MigrationHistoryService migrationEntryService, LockService lockService,
			ScriptLookupService scriptLookupService) {
		this.database = database;
		this.migrationEntryService = migrationEntryService;
		this.lockService = lockService;
		this.scriptLookupService = scriptLookupService;
	}

	/**
	 * It returns the migrations that would be applied, if the migration would
	 * be performed, but it doesn't actually migrate anything
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
	 * Performs the migration from the last entry in the DB, until the last
	 * available {@link com.rinoto.migramongo.MongoMigrationScript} found.
	 * <p>
	 * If there are no migrations in the source code, and there is no
	 * 
	 * @return the status
	 */
	public MigraMongoStatus migrate() {
		final boolean lockAcquired = lockService.acquireLock();
		if (!lockAcquired) {
			return MigraMongoStatus.lockNotAcquired();
		}
		try {
			final List<MongoMigrationScript> migrationScriptsToApply = findMigrationScriptsToApply();
			return migrate(migrationScriptsToApply);
		} catch (MongoMigrationException e) {
			return e.getStatus();
		} finally {
			lockService.releaseLock();
		}
	}

	/**
	 * Same as migrate, but asynchronous.
	 * <p>
	 * <ul>
	 * <li>if there is nothing to migrate, a status with OK will be returned
	 * <li>if there are items to migrate, a status with IN_PROGRESS and the
	 * items that will be migrated will be returned
	 * </ul>
	 * 
	 * @return
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
	 * @param fromVersion
	 * @return status of the applied migrations since the specified version
	 */
	public MigraMongoStatus status(String fromVersion) {
		final Iterable<MigrationEntry> migrations = migrationEntryService.findMigrations(fromVersion);
		for (MigrationEntry migEntry : migrations) {
			if (migEntry.getStatus() == MigrationStatus.ERROR) {
				return MigraMongoStatus.error("At least one migration script threw an error. Check individual entries")
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
		for (MongoMigrationScript migScriptToApply : migrationScriptsToApply) {
			final MigrationEntry migEntry = executeMigrationScript(migScriptToApply);
			status.addEntry(migEntry);
			if (migEntry.getStatus() == MigrationStatus.ERROR) {
				status.status = MigrationStatus.ERROR;
				status.message = "MigrationScript with fromVersion '" + migEntry.getFromVersion() + "' and toVersion '"
						+ migEntry.getToVersion() + "' failed with message: " + migEntry.getStatusMessage();
				return status;
			}
		}
		logger.debug("Migration performed with status {}", status);
		return status;
	}

	private MigrationEntry getLastMigrationApplied() throws MongoMigrationException {
		final MigrationEntry lastMigrationApplied = migrationEntryService.getLastMigrationApplied();
		if (isInInconsistentState(lastMigrationApplied)) {
			throw new MongoMigrationException(new MigraMongoStatus(MigrationStatus.ERROR,
					"Last Migration is in status " + lastMigrationApplied.getStatus() + ": " + lastMigrationApplied
							+ ". Cannot apply any migration until the entry gets fixed"));
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
				throw new MongoMigrationException(new MigraMongoStatus(MigrationStatus.ERROR,
						"no last migration script found, and no initial migration script provided!"));
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
	 * 'repairs' an entry in the <i>_migramongo</i> collection that has been
	 * marked as <code>ERROR</code> or has hanged in <code>IN_PROGRESS</code>
	 * status.
	 * <p>
	 * the entry gets defined by the <code>fromVersion</code> and
	 * <code>toVersion</code> parameters
	 * <p>
	 * if the entry does not exist, or it was not in one of the allowed states
	 * for reparing, an error status will be thrown
	 * 
	 * @param fromVersion
	 *            fromVersion
	 * @param toVersion
	 *            toVersion
	 * @return status
	 */
	public MigraMongoStatus repair(String fromVersion, String toVersion) {
		final MigrationEntry migrationEntry = migrationEntryService.findMigration(fromVersion, toVersion);
		if (migrationEntry == null) {
			return MigraMongoStatus.error(
					"No migration entry found for fromVersion '" + fromVersion + "' and toVersion '" + toVersion + "'");
		}
		if (migrationEntry.getStatus() == MigrationStatus.OK) {
			return MigraMongoStatus.error("Migration entry with fromVersion '" + fromVersion + "' and toVersion '"
					+ toVersion + "' has already status '" + migrationEntry.getStatus() + "'. Nothing will be done");
		}
		final MigrationStatus previousStatus = migrationEntry.getStatus();
		final MigrationEntry correctedMigrationEntry = migrationEntryService
				.setMigrationStatusToFinished(migrationEntry);
		final MigraMongoStatus status = MigraMongoStatus.ok("Status of migrationEntry " + migrationEntry
				+ " changed from '" + previousStatus + "' to '" + MigrationStatus.OK + "'");
		status.addEntry(correctedMigrationEntry);
		return status;
	}

	private boolean isInInconsistentState(MigrationEntry mig) {
		return mig != null && mig.getStatus() != MigrationStatus.OK;
	}

	private MigrationEntry executeMigrationScript(MongoMigrationScript migrationScript) {
		final MigrationEntry migrationEntry = migrationEntryService
				.insertMigrationStatusInProgress(migrationScript.getMigrationInfo());
		try {
			migrationScript.migrate(database);
			return migrationEntryService.setMigrationStatusToFinished(migrationEntry);
		} catch (Exception e) {
			return migrationEntryService.setMigrationStatusToFailed(migrationEntry, e);
		}
	}

	private List<MongoMigrationScript> getMigrationScriptsToApply(String version) {
		final Collection<MongoMigrationScript> migScripts = scriptLookupService.findMongoScripts();
		final List<MongoMigrationScript> migScriptsToApply = findMigScriptsToApply(version, migScripts);
		return migScriptsToApply;
	}

	private List<MongoMigrationScript> findMigScriptsToApply(String version,
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
				nextMigrationScript.getMigrationInfo().getToVersion(), rest);
		candidates.addAll(nextMigScriptsRec);
		return candidates;
	}

	/**
	 * Returns the migration entries that have been applied
	 * 
	 * @return
	 */
	public List<MigrationEntry> getMigrationEntries() {
		return toList(migrationEntryService.getAllMigrationsApplied());
	}

	private <T extends Object> List<T> toList(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
	}

}
