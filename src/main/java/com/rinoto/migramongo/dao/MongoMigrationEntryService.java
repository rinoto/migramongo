package com.rinoto.migramongo.dao;

import static com.mongodb.client.model.Filters.eq;

import java.util.Date;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;

public class MongoMigrationEntryService implements MigrationEntryService {

    private static final String MIGRAMONGO_COLLECTION = "_migramongo";
    private final MongoDatabase database;

    public MongoMigrationEntryService(MongoDatabase database) {
        this.database = database;
    }

    @Override
    public MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo) {
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.module = migrationInfo.getModule();
        migEntry.status = "IN_PROGRESS";
        migEntry.fromVersion = migrationInfo.getFromVersion();
        migEntry.toVersion = migrationInfo.getToVersion();
        migEntry.info = migrationInfo.getInfo();
        migEntry.createdAt = new Date();

        return insertMigrationEntry(migEntry);

    }

    private MigrationEntry insertMigrationEntry(MigrationEntry migEntry) {

        migEntry.updatedAt = new Date();

        final Document document = mapMigEntryToDocument(migEntry);
        if (migEntry.id == null) {
            getMigramongoCollection().insertOne(document);
            migEntry.id = document.getObjectId("_id");
        } else {
            getMigramongoCollection().replaceOne(eq("_id", migEntry.id), document);
        }
        return migEntry;
    }

    public MongoCollection<Document> getMigramongoCollection() {
        MongoCollection<Document> collection = this.database.getCollection(MIGRAMONGO_COLLECTION);
        if (collection == null) {
            this.database.createCollection(MIGRAMONGO_COLLECTION);
            collection = this.database.getCollection(MIGRAMONGO_COLLECTION);
        }
        return collection;
    }

    @Override
    public MigrationEntry setMigrationStatusToFailed(MigrationEntry migrationEntry, Exception e) {
        migrationEntry.status = "ERROR";
        migrationEntry.statusMessage = e.getMessage();
        return insertMigrationEntry(migrationEntry);
    }

    @Override
    public MigrationEntry setMigrationStatusToFinished(MigrationEntry migrationEntry) {
        migrationEntry.status = "OK";
        return insertMigrationEntry(migrationEntry);
    }

    @Override
    public MigrationEntry getLastMigrationApplied() {
        final Document doc = getMigramongoCollection().find().sort(new Document("createdAt", 1)).first();
        return mapMigrationEntry(doc);
    }

    private MigrationEntry mapMigrationEntry(Document doc) {
        if (doc == null) {
            return null;
        }
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.id = doc.getObjectId("_id");
        migEntry.fromVersion = doc.getString("fromVersion");
        migEntry.toVersion = doc.getString("toVersion");
        migEntry.createdAt = doc.getDate("createdAt");
        migEntry.module = doc.getString("module");
        migEntry.info = doc.getString("info");
        migEntry.status = doc.getString("status");
        migEntry.statusMessage = doc.getString("statusMessage");
        return migEntry;
    }

    private Document mapMigEntryToDocument(MigrationEntry migEntry) {
        final Document doc = new Document();
        if (migEntry.id != null) {
            doc.put("_id", migEntry.id);
        }
        doc.put("fromVersion", migEntry.fromVersion);
        doc.put("toVersion", migEntry.toVersion);
        doc.put("createdAt", migEntry.createdAt);
        doc.put("updatedAt", migEntry.updatedAt);
        doc.put("module", migEntry.module);
        doc.put("info", migEntry.info);
        doc.put("status", migEntry.status);
        doc.put("statusMessage", migEntry.statusMessage);
        return doc;
    }

}
