package com.rinoto.migramongo;

import java.util.ArrayList;
import java.util.List;

public class MigraMongoStatus {

	public MigrationStatus status;
	public String message;
	public List<MigrationEntry> migrationsApplied = new ArrayList<>();

	public MigraMongoStatus(MigrationStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public static final MigraMongoStatus ok() {
		return ok("Everything ok");
	}

	public static final MigraMongoStatus ok(String message) {
		return new MigraMongoStatus(MigrationStatus.OK, message);
	}

	public static final MigraMongoStatus error(String errorMessage) {
		return new MigraMongoStatus(MigrationStatus.ERROR, errorMessage);
	}

	public static final MigraMongoStatus inProgress(String message) {
		return new MigraMongoStatus(MigrationStatus.IN_PROGRESS, message);
	}

	public MigraMongoStatus addEntry(MigrationEntry migEntry) {
		migrationsApplied.add(migEntry);
		return this;
	}

	public MigraMongoStatus withEntries(List<MigrationEntry> entries) {
		this.migrationsApplied = entries;
		return this;
	}

	public enum MigrationStatus {
		OK, LOCK_NOT_ACQUIRED, ERROR, IN_PROGRESS;
	}

	@Override
	public String toString() {
		return "[status=" + status + ", message=" + message + ", migrationsApplied=" + migrationsApplied + "]";
	}

	public static MigraMongoStatus lockNotAcquired() {
		return new MigraMongoStatus(MigrationStatus.LOCK_NOT_ACQUIRED, "Couldn't obtain lock - doing nothing");
	}

}
