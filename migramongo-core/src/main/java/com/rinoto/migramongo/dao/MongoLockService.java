package com.rinoto.migramongo.dao;

import java.util.Date;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.LockEntry;

public class MongoLockService implements LockService {

    public static final String MIGRAMONGO_LOCK_COLLECTION = "_migramongo_lock";
    private final MongoDatabase database;

    public MongoLockService(MongoDatabase database) {
        this.database = database;
        initLockCollection();
    }

    private void initLockCollection() {
        //if the table already exists, we do not need to create it anymore
        if (getLockCollection().countDocuments() > 0) {
            return;
        }
        // assuring that the index exists
        getLockCollection().createIndex(new BasicDBObject(LockEntryDocBuilder.LOCK_ENTRY, 1));
        // creating the first entry - as "lockEntry" is unique, it will fail if
        // one already exists, but it's ok
        final Document basicLockEntry = new LockEntryDocBuilder().build();
        getLockCollection().insertOne(basicLockEntry);
    }

    private MongoCollection<Document> getLockCollection() {
        return this.database.getCollection(MIGRAMONGO_LOCK_COLLECTION);
    }

    @Override
    public boolean acquireLock() {
        if (getLockCollection().countDocuments() == 0) {
            throw new IllegalStateException(
                "Cannot acquire lock because lock table has not been initialized. Initialize MongoLockService properly, or call destroyLock");
        }
        final Document lockDocument = new LockEntryDocBuilder().lock().build();
        final Document result = getLockCollection()
            .findOneAndUpdate(new Document(LockEntryDocBuilder.LOCK_ENTRY, true), new Document("$set", lockDocument));
        return result != null && result.getBoolean(LockEntryDocBuilder.LOCKED, true) == false;
    }

    @Override
    public boolean releaseLock() {
        final Document releaseDocument = new LockEntryDocBuilder().release().build();
        final Document result = getLockCollection().findOneAndUpdate(
            new Document(LockEntryDocBuilder.LOCK_ENTRY, true),
            new Document("$set", releaseDocument));
        return result != null && result.getBoolean(LockEntryDocBuilder.LOCKED, false) == true;
    }

    static class LockEntryDocBuilder {

        static String LOCK_ENTRY = "lockEntry";
        static String LOCKED = "locked";
        static String LAST_LOCK_DATE = "lastLockDate";
        static String LAST_RELEASE_DATE = "lastReleaseDate";
        private Document doc;

        LockEntryDocBuilder() {
            doc = new Document();
            doc.put(LOCK_ENTRY, true);
            doc.put(LOCKED, false);
        }

        LockEntryDocBuilder lock() {
            doc.put(LOCKED, true);
            doc.put(LAST_LOCK_DATE, new Date());
            return this;
        }

        LockEntryDocBuilder release() {
            doc.put(LOCKED, false);
            doc.put(LAST_RELEASE_DATE, new Date());
            return this;
        }

        Document build() {
            return doc;
        }

    }

    @Override
    public LockEntry getLockInformation() {
        final Document lockDocument = getLockCollection()
            .find(new Document(LockEntryDocBuilder.LOCK_ENTRY, true))
            .first();
        return new LockEntry(
            lockDocument.getBoolean(LockEntryDocBuilder.LOCKED),
            lockDocument.getDate(LockEntryDocBuilder.LAST_LOCK_DATE),
            lockDocument.getDate(LockEntryDocBuilder.LAST_RELEASE_DATE));
    }

    @Override
    public void destroyLock() {
        getLockCollection().deleteMany(new Document(LockEntryDocBuilder.LOCK_ENTRY, true));
        initLockCollection();
    }
}
