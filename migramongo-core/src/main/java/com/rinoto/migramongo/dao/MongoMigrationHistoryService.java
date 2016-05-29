package com.rinoto.migramongo.dao;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Date;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;

public class MongoMigrationHistoryService implements MigrationHistoryService {

    public static final String MIGRAMONGO_COLLECTION = "_migramongo";
    private final MongoDatabase database;

    public MongoMigrationHistoryService(MongoDatabase database) {
        this.database = database;
    }

    @Override
    public MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo) {
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.setModule(migrationInfo.getModule());
        migEntry.setStatus(MigrationStatus.IN_PROGRESS);
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

    public MongoCollection<Document> getMigramongoCollection() {
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

    private MigrationEntry mapMigrationEntry(Document doc) {
        if (doc == null) {
            return null;
        }
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.setId(doc.getObjectId("_id"));
        migEntry.setFromVersion(doc.getString("fromVersion"));
        migEntry.setToVersion(doc.getString("toVersion"));
        migEntry.setCreatedAt(doc.getDate("createdAt"));
        migEntry.setModule(doc.getString("module"));
        migEntry.setInfo(doc.getString("info"));
        migEntry.setStatus(MigrationStatus.valueOf(doc.getString("status")));
        migEntry.setStatusMessage(doc.getString("statusMessage"));
        migEntry.setRepaired(doc.getBoolean("repaired"));
        return migEntry;
    }

    private Document mapMigEntryToDocument(MigrationEntry migEntry) {
        final Document doc = new Document();
        if (migEntry.getId() != null) {
            doc.put("_id", migEntry.getId());
        }
        doc.put("fromVersion", migEntry.getFromVersion());
        doc.put("toVersion", migEntry.getToVersion());
        doc.put("createdAt", migEntry.getCreatedAt());
        doc.put("updatedAt", migEntry.getUpdatedAt());
        doc.put("module", migEntry.getModule());
        doc.put("info", migEntry.getInfo());
        doc.put("status", migEntry.getStatus().name());
        doc.put("statusMessage", migEntry.getStatusMessage());
        doc.put("repaired", migEntry.isRepaired());
        return doc;
    }

    @Override
    public MigrationEntry findMigration(String fromVersion, String toVersion) {
        return getMigramongoCollection()
            .find(and(eq("fromVersion", fromVersion), eq("toVersion", toVersion)))
            .map(d -> mapMigrationEntry(d))
            .first();
    }

}
