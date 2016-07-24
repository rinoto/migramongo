package com.rinoto.migramongo.dao;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationEntry.MigrationType;
import com.rinoto.migramongo.MigrationInfo;

public class MongoMigrationHistoryService implements MigrationHistoryService {

	public static final String MIGRAMONGO_COLLECTION = "_migramongo_upgrade_info";
	private final MongoDatabase database;

	public MongoMigrationHistoryService(MongoDatabase database) {
		this.database = database;
	}

	@Override
	public MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo) {
		final MigrationEntry migEntry = new MigrationEntry();
		migEntry.setModule(migrationInfo.getModule());
		migEntry.setStatus(MigrationStatus.IN_PROGRESS);
		migEntry.setMigrationType(migrationInfo.getMigrationType());
		migEntry.setFromVersion(migrationInfo.getFromVersion());
		migEntry.setToVersion(migrationInfo.getToVersion());
		migEntry.setInfo(migrationInfo.getInfo());
		migEntry.setCreatedAt(new Date());

		return insertMigrationEntry(migEntry);

	}

	private MigrationEntry insertMigrationEntry(MigrationEntry migEntry) {

		migEntry.setUpdatedAt(new Date());

		final Document document = mapMigEntryToDocument(migEntry);
		if (migEntry.getId() == null) {
			getMigramongoCollection().insertOne(document);
			migEntry.setId(document.getObjectId("_id"));
		} else {
			getMigramongoCollection().replaceOne(eq("_id", migEntry.getId()), document);
		}
		return migEntry;
	}

	MongoCollection<Document> getMigramongoCollection() {
		return this.database.getCollection(MIGRAMONGO_COLLECTION);
	}

	@Override
	public MigrationEntry setMigrationStatusToFailed(MigrationEntry migrationEntry, Exception e) {
		migrationEntry.setStatus(MigrationStatus.ERROR);
		migrationEntry.setStatusMessage(e.getMessage());
		return insertMigrationEntry(migrationEntry);
	}

	@Override
	public MigrationEntry setMigrationStatusToFinished(MigrationEntry migrationEntry) {
		migrationEntry.setStatus(MigrationStatus.OK);
		return insertMigrationEntry(migrationEntry);
	}

	@Override
	public MigrationEntry setMigrationStatusToManuallyRepaired(MigrationEntry migrationEntry) {
		migrationEntry.setStatus(MigrationStatus.OK);
		migrationEntry.setRepaired(true);
		return insertMigrationEntry(migrationEntry);
	}

	@Override
	public MigrationEntry getLastMigrationApplied() {
		final Document doc = getMigramongoCollection().find().sort(new Document("createdAt", -1)).first();
		return mapMigrationEntry(doc);
	}

	@Override
	public Iterable<MigrationEntry> getAllMigrationsApplied() {
		return getMigramongoCollection().find().sort(new Document("createdAt", 1)).map(d -> mapMigrationEntry(d));
	}

	private MigrationEntry mapMigrationEntry(Document doc) {
		if (doc == null) {
			return null;
		}
		final MigrationEntry migEntry = new MigrationEntry();
		migEntry.setId(doc.getObjectId("_id"));
		migEntry.setModule(doc.getString("module"));
		migEntry.setInfo(doc.getString("info"));
		migEntry.setFromVersion(doc.getString("fromVersion"));
		migEntry.setToVersion(doc.getString("toVersion"));
		migEntry.setMigrationType(MigrationType.valueOf(doc.getString("type")));
		migEntry.setStatus(MigrationStatus.valueOf(doc.getString("status")));
		migEntry.setStatusMessage(doc.getString("statusMessage"));
		migEntry.setCreatedAt(doc.getDate("createdAt"));
		migEntry.setUpdatedAt(doc.getDate("updatedAt"));
		migEntry.setRepaired(doc.getBoolean("repaired"));
		return migEntry;
	}

	private Document mapMigEntryToDocument(MigrationEntry migEntry) {
		final Document doc = new Document();
		if (migEntry.getId() != null) {
			doc.put("_id", migEntry.getId());
		}
		doc.put("module", migEntry.getModule());
		doc.put("info", migEntry.getInfo());
		doc.put("fromVersion", migEntry.getFromVersion());
		doc.put("toVersion", migEntry.getToVersion());
		doc.put("type", migEntry.getMigrationType().name());
		doc.put("status", migEntry.getStatus().name());
		doc.put("statusMessage", migEntry.getStatusMessage());
		doc.put("createdAt", migEntry.getCreatedAt());
		doc.put("updatedAt", migEntry.getUpdatedAt());
		doc.put("repaired", migEntry.isRepaired());
		return doc;
	}

	@Override
	public MigrationEntry findMigration(String fromVersion, String toVersion) {
		return getMigramongoCollection().find(and(eq("fromVersion", fromVersion), eq("toVersion", toVersion)))
				.map(d -> mapMigrationEntry(d)).first();
	}

	@Override
	public Iterable<MigrationEntry> findMigrations(String fromVersion) {
		if (fromVersion == null) {
			// returning all the migrations
			return getMigramongoCollection().find().sort(new Document("createdAt", 1)).map(d -> mapMigrationEntry(d));
		}
		final List<MigrationEntry> entriesFrom = new ArrayList<>();
		// fromVersion is not null -> we have to find first the one with
		// fromVersion, and then go up
		String nextFromVersion = fromVersion;
		boolean initialFound = false;
		while (true) {
			final Bson fromVersionFilter = eq("fromVersion", nextFromVersion);
			final Bson upgradeFilter = eq("type", MigrationType.UPGRADE.name());
			final FindIterable<Document> find = getMigramongoCollection().find();
			if (initialFound) {
				find.filter(and(fromVersionFilter, upgradeFilter));
			} else {
				find.filter(fromVersionFilter);
			}
			final MigrationEntry migEntry = find.map(d -> mapMigrationEntry(d)).first();
			if (migEntry == null) {
				return entriesFrom;
			}
			entriesFrom.add(migEntry);
			nextFromVersion = migEntry.getToVersion();
			if (migEntry.getMigrationType() == MigrationType.INITIAL) {
				initialFound = true;
			}
		}

	}
}
