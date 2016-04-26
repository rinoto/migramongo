package com.rinoto.migramongo;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MigraMongo {

    private static final String MIGRAMONGO_COLLECTION = "_migramongo";
    private final MongoDatabase database;

    @Autowired
    private ApplicationContext appContext;

    public MigraMongo(MongoDatabase database) {
        this.database = database;
    }

    public MigraMongoStatus migrate() {
        final MigraMongoStatus status = MigraMongoStatus.ok();
        MigrationEntry lastMigrationApplied = getLastMigrationApplied();
        if (lastMigrationApplied == null) {
            final MongoMigrScript initialMigrationScript = getInitialMigrationScript();
            if (initialMigrationScript == null) {
                return new MigraMongoStatus(
                    "ERROR",
                    "no last migration script found, and no initial migration script provided!");
            }
            lastMigrationApplied = executeMigrationScript(initialMigrationScript);
            status.addEntry(lastMigrationApplied);
        }
        final List<MongoMigrScript> migrationScriptsToApply = getMigrationScriptsToApply(
            lastMigrationApplied.toVersion);
        migrationScriptsToApply.forEach(ms -> {
            final MigrationEntry migEntry = executeMigrationScript(ms);
            status.addEntry(migEntry);
        });

        return status;
    }

    private MigrationEntry executeMigrationScript(MongoMigrScript migrationScript) {
        final MigrationEntry migrationEntry = insertMigrationStatusInProgress(migrationScript.getMigrationInfo());
        try {
            migrationScript.migrate(database);
            return setMigrationStatusToFinished(migrationEntry);
        } catch (Exception e) {
            return setMigrationStatusToFailed(migrationEntry, e);
        }
    }

    private MigrationEntry setMigrationStatusToFailed(MigrationEntry migrationEntry, Exception e) {
        migrationEntry.status = "ERROR";
        migrationEntry.statusMessage = e.getMessage();
        return insertMigrationEntry(migrationEntry);
    }

    private MigrationEntry setMigrationStatusToFinished(MigrationEntry migrationEntry) {
        migrationEntry.status = "OK";
        return insertMigrationEntry(migrationEntry);
    }

    private MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo) {
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

    public List<MongoMigrScript> getMigrationScriptsToApply(String version) {
        // final Collection<Object> migScripts =
        // appContext.getBeansWithAnnotation(MongoMigrationScript.class).values();
        final Collection<MongoMigrScript> migScripts = appContext
            .getBeansOfType(MongoMigrScript.class)
            .values()
            .stream()
            .filter(ms -> !InitialMongoMigrScript.class.isInstance(ms))
            .collect(Collectors.toList());
            // final List<MigrationScript> allMigrationScripts = migScripts.stream()
            // .map(migScript -> new
            // MigrationScript(migScript)).collect(Collectors.toList());

        // }).collect(Collectors.toList());
        final List<MongoMigrScript> migScriptsToApply = findMigScriptsToApply(version, migScripts);
        return migScriptsToApply;
    }

    private List<MongoMigrScript> findMigScriptsToApply(
            String version,
            Collection<MongoMigrScript> allMigrationScripts) {
        if (allMigrationScripts.isEmpty()) {
            return new ArrayList<>();
        }
        final List<MongoMigrScript> candidates = new ArrayList<>();
        final List<MongoMigrScript> rest = new ArrayList<>();
        for (MongoMigrScript ms : allMigrationScripts) {
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
        final MongoMigrScript nextMigrationScript = candidates.get(0);
        final List<MongoMigrScript> nextMigScriptsRec = findMigScriptsToApply(
            nextMigrationScript.getMigrationInfo().getToVersion(),
            rest);
        candidates.addAll(nextMigScriptsRec);
        return candidates;
    }

    public MongoMigrScript getInitialMigrationScript() {
        // final Collection<Object> values =
        // appContext.getBeansWithAnnotation(InitialMongoMigrationScript.class).values();
        Collection<InitialMongoMigrScript> values = appContext.getBeansOfType(InitialMongoMigrScript.class).values();
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException(
                "There cannot be more than one InitialMigrationScript!. Found " + values.size() + ": " + values);
        }
        return values.iterator().next();
    }

    public void applyMigration(MongoMigrScript migrationObject) {
        migrationObject.migrate(database);
    }

}
