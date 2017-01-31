package com.rinoto.migramongo.dao;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBList;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationEntry.MigrationType;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.MigrationRun;

public class MongoMigrationHistoryService implements MigrationHistoryService {

    public static final String MIGRAMONGO_COLLECTION = "_migramongo_upgrade_info";
    private final MongoDatabase database;

    public MongoMigrationHistoryService(MongoDatabase database) {
        this.database = database;
    }

    @Override
    public MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo) {
        final MigrationEntry migEntry = mapMigrationInfoToMigrationEntry(migrationInfo, MigrationStatus.IN_PROGRESS);
        return insertMigrationEntry(migEntry);
    }

    @Override
    public MigrationEntry insertMigrationStatusSkipped(MigrationInfo migrationInfo) {
        final MigrationEntry migEntry = mapMigrationInfoToMigrationEntry(migrationInfo, MigrationStatus.OK);
        migEntry.setSkipped(true);
        return insertMigrationEntry(migEntry);
    }

    private MigrationEntry mapMigrationInfoToMigrationEntry(MigrationInfo migrationInfo, MigrationStatus status) {
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.setModule(migrationInfo.getModule());
        migEntry.setStatus(status);
        migEntry.setMigrationType(migrationInfo.getMigrationType());
        migEntry.setFromVersion(migrationInfo.getFromVersion());
        migEntry.setToVersion(migrationInfo.getToVersion());
        migEntry.setInfo(migrationInfo.getInfo());
        migEntry.setCreatedAt(new Date());
        return migEntry;
    }

    private MigrationEntry insertMigrationEntry(MigrationEntry migEntry) {

        migEntry.setUpdatedAt(new Date());

        final Document document = mapMigEntryToDocument(migEntry);
        if (migEntry.getId() == null) {
            getMigramongoCollection().insertOne(document);
            migEntry.setId(document.getObjectId("_id"));
        } else {
            replaceMigrationEntry(migEntry, document);
        }
        return migEntry;
    }

    private void replaceMigrationEntry(MigrationEntry migEntry, final Document document) {
        getMigramongoCollection().replaceOne(eq("_id", migEntry.getId()), document);
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
        migEntry.setFromVersion(doc.getString("fromVersion"));
        migEntry.setToVersion(doc.getString("toVersion"));
        migEntry.setMigrationType(MigrationType.valueOf(doc.getString("type")));
        fillMigrationRunDataIntoObject(migEntry, doc);
        final Boolean repaired = doc.getBoolean("repaired");
        if (repaired != null) {
            migEntry.setRepaired(repaired);
        }
        final Boolean skipped = doc.getBoolean("skipped");
        if (skipped != null) {
            migEntry.setSkipped(skipped);
        }
        final BasicDBList reruns = doc.get("reruns", BasicDBList.class);
        if (reruns != null) {
            final List<MigrationRun> rerunsList = new ArrayList<>();
            for (Object migRunDoc : reruns) {
                final MigrationRun migRun = mapMigrationRun((Document) migRunDoc);
                rerunsList.add(migRun);
            }
            migEntry.setReruns(rerunsList);
        }
        return migEntry;
    }

    private MigrationRun mapMigrationRun(Document migRunDoc) {
        final MigrationRun migRun = new MigrationRun();
        fillMigrationRunDataIntoObject(migRun, migRunDoc);
        return migRun;
    }

    private void fillMigrationRunDataIntoObject(MigrationRun migRun, Document doc) {
        migRun.setInfo(doc.getString("info"));
        migRun.setStatus(MigrationStatus.valueOf(doc.getString("status")));
        migRun.setStatusMessage(doc.getString("statusMessage"));
        migRun.setCreatedAt(doc.getDate("createdAt"));
        migRun.setUpdatedAt(doc.getDate("updatedAt"));
    }

    private Document mapMigEntryToDocument(MigrationEntry migEntry) {
        final Document migEntryDoc = new Document();
        if (migEntry.getId() != null) {
            migEntryDoc.put("_id", migEntry.getId());
        }
        migEntryDoc.put("module", migEntry.getModule());
        migEntryDoc.put("fromVersion", migEntry.getFromVersion());
        migEntryDoc.put("toVersion", migEntry.getToVersion());
        migEntryDoc.put("type", migEntry.getMigrationType().name());
        migEntryDoc.put("repaired", migEntry.isRepaired());
        migEntryDoc.put("skipped", migEntry.isSkipped());
        fillMigrationRunDataIntoDocument(migEntryDoc, migEntry);
        if (migEntry.getReruns() != null) {
            final BasicDBList migRunList = new BasicDBList();
            for (MigrationRun migRun : migEntry.getReruns()) {
                final Document migRunDoc = mapMigRunToDoc(migRun);
                migRunList.add(migRunDoc);
            }
            migEntryDoc.put("reruns", migRunList);
        }
        return migEntryDoc;
    }

    private Document mapMigRunToDoc(MigrationRun migRun) {
        final Document migRunDoc = new Document();
        fillMigrationRunDataIntoDocument(migRunDoc, migRun);
        return migRunDoc;
    }

    private void fillMigrationRunDataIntoDocument(Document migRunDoc, MigrationRun migRun) {
        migRunDoc.put("info", migRun.getInfo());
        migRunDoc.put("status", migRun.getStatus().name());
        migRunDoc.put("statusMessage", migRun.getStatusMessage());
        migRunDoc.put("createdAt", migRun.getCreatedAt());
        migRunDoc.put("updatedAt", migRun.getUpdatedAt());
    }

    @Override
    public MigrationEntry findMigration(String fromVersion, String toVersion) {
        return mapMigrationEntry(findMigrationDoc(fromVersion, toVersion));
    }

    private Document findMigrationDoc(String fromVersion, String toVersion) {
        return getMigramongoCollection().find(and(eq("fromVersion", fromVersion), eq("toVersion", toVersion))).first();
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

    @Override
    public MigrationEntry addRunToMigrationEntry(MigrationEntry migEntry, MigrationRun migRun) {
        final Document migEntryDoc = findMigrationDoc(migEntry.getFromVersion(), migEntry.getToVersion());
        if (migEntryDoc == null) {
            return null;
        }
        final Document migRunDoc = mapMigRunToDoc(migRun);
        BasicDBList list = migEntryDoc.get("reruns", BasicDBList.class);
        if (list == null) {
            list = new BasicDBList();
            migEntryDoc.put("reruns", migRunDoc);
        } else {
            list.add(migRunDoc);
        }
        replaceMigrationEntry(migEntry, migEntryDoc);
        return mapMigrationEntry(migEntryDoc);
    }

}
