package com.rinoto.migramongo;

import java.util.ArrayList;
import java.util.List;

public class MigraMongoStatus {

	public String status;
	public String message;
	public List<MigrationEntry> migrationsApplied = new ArrayList<>();

	public MigraMongoStatus(String status, String message) {
		this.status = status;
		this.message = message;
	}

	public static final MigraMongoStatus ok() {
		return new MigraMongoStatus("OK", "Everything ok");
	}

	public MigraMongoStatus addEntry(MigrationEntry migEntry) {
		migrationsApplied.add(migEntry);
		return this;
	}

	@Override
	public String toString() {
		return "MigraMongoStatus [status=" + status + ", message=" + message + ", appliedEntries=" + migrationsApplied
				+ "]";
	}

}
