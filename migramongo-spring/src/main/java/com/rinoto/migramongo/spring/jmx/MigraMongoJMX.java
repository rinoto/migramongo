package com.rinoto.migramongo.spring.jmx;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.gson.Gson;
import com.rinoto.migramongo.MigraMongo;

@ManagedResource
public class MigraMongoJMX {

	private final MigraMongo migraMongo;
	private Gson gson;

	public MigraMongoJMX(MigraMongo migraMongo) {
		this.migraMongo = migraMongo;
	}

	@ManagedOperation
	public String migrate() {
		return toJson(migraMongo.migrate());
	}

	@ManagedOperation
	public String migrateAsync() {
		return toJson(migraMongo.migrateAsync());
	}

	@ManagedOperation
	public String dryRun() {
		return toJson(migraMongo.dryRun());
	}

	@ManagedOperation
	public String status(String fromVersion) {
		return toJson(migraMongo.status(fromVersion));
	}

	@ManagedOperation
	public Boolean needsMigration() {
		return !migraMongo.dryRun().migrationsApplied.isEmpty();
	}

	@ManagedOperation
	public String history() {
		return toJson(migraMongo.getMigrationEntries());
	}

	private String toJson(Object obj) {
		return getGson().toJson(obj);
	}

	private Gson getGson() {
		if (gson == null) {
			gson = new Gson();
		}
		return gson;
	}

}
