package com.rinoto.migramongo.dao;

import java.util.Date;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoLockService implements LockService {

	public static final String MIGRAMONGO_LOCK_COLLECTION = "_migramongo_lock";
	private final MongoDatabase database;

	public MongoLockService(MongoDatabase database) {
		this.database = database;
		initLockCollection();
	}

	private void initLockCollection() {
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
		final Document lockDocument = new LockEntryDocBuilder().lock().build();
		final Document result = getLockCollection().findOneAndUpdate(
				new Document(LockEntryDocBuilder.LOCK_ENTRY, true), new Document("$set", lockDocument));
		return result != null && result.getBoolean(LockEntryDocBuilder.LOCKED, true) == false;
	}

	@Override
	public boolean releaseLock() {
		final Document releaseDocument = new LockEntryDocBuilder().release().build();
		final Document result = getLockCollection().findOneAndUpdate(
				new Document(LockEntryDocBuilder.LOCK_ENTRY, true), new Document("$set", releaseDocument));
		return result != null && result.getBoolean(LockEntryDocBuilder.LOCKED, false) == true;
	}

	static class LockEntryDocBuilder {

		static String LOCK_ENTRY = "lockEntry";
		static String LOCKED = "locked";
		private Document doc;

		LockEntryDocBuilder() {
			doc = new Document();
			doc.put(LOCK_ENTRY, true);
			doc.put(LOCKED, false);
		}

		LockEntryDocBuilder lock() {
			doc.put(LOCKED, true);
			doc.put("lastLockDate", new Date());
			return this;
		}

		LockEntryDocBuilder release() {
			doc.put(LOCKED, false);
			doc.put("lastReleaseDate", new Date());
			return this;
		}

		Document build() {
			return doc;
		}

	}
}
