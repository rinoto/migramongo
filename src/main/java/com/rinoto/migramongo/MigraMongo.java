package com.rinoto.migramongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.dao.MigrationHistoryService;

public class MigraMongo {

	private final ScriptLookupService scriptLookupService;
	private final MigrationHistoryService migrationEntryService;
	private MongoDatabase database;

	public MigraMongo(MongoDatabase database, MigrationHistoryService migrationEntryService,
			ScriptLookupService scriptLookupService) {
		this.database = database;
		this.migrationEntryService = migrationEntryService;
		this.scriptLookupService = scriptLookupService;
	}

	public MigraMongoStatus migrate() {
		final MigraMongoStatus status = MigraMongoStatus.ok();
		MigrationEntry lastMigrationApplied = migrationEntryService.getLastMigrationApplied();
		if (lastMigrationApplied == null) {
			final InitialMongoMigrationScript initialMigrationScript = scriptLookupService.findInitialScript();
			if (initialMigrationScript == null) {
				return new MigraMongoStatus(MigrationStatus.ERROR,
						"no last migration script found, and no initial migration script provided!");
			}
			lastMigrationApplied = executeMigrationScript(initialMigrationScript);
			status.addEntry(lastMigrationApplied);
		}
		final List<MongoMigrationScript> migrationScriptsToApply = getMigrationScriptsToApply(lastMigrationApplied
				.getToVersion());
		migrationScriptsToApply.forEach(ms -> {
			final MigrationEntry migEntry = executeMigrationScript(ms);
			status.addEntry(migEntry);
		});

		return status;
	}

	private MigrationEntry executeMigrationScript(MongoMigrationScript migrationScript) {
		final MigrationEntry migrationEntry = migrationEntryService.insertMigrationStatusInProgress(migrationScript
				.getMigrationInfo());
		try {
			migrationScript.migrate(database);
			return migrationEntryService.setMigrationStatusToFinished(migrationEntry);
		} catch (Exception e) {
			return migrationEntryService.setMigrationStatusToFailed(migrationEntry, e);
		}
	}

	public List<MongoMigrationScript> getMigrationScriptsToApply(String version) {
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
			throw new IllegalStateException("There is more than one script with fromVersion " + version + ": "
					+ allMigrationScripts);
		}
		final MongoMigrationScript nextMigrationScript = candidates.get(0);
		final List<MongoMigrationScript> nextMigScriptsRec = findMigScriptsToApply(nextMigrationScript
				.getMigrationInfo().getToVersion(), rest);
		candidates.addAll(nextMigScriptsRec);
		return candidates;
	}

}
