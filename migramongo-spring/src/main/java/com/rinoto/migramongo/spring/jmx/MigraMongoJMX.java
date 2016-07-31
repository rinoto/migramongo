package com.rinoto.migramongo.spring.jmx;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.gson.Gson;
import com.rinoto.migramongo.MigraMongo;

/**
 * MBean that can be registered to expose the Migramongo methods to be called
 * via JMX.
 * 
 * @author rinoto
 */
@ManagedResource
public class MigraMongoJMX {

	private final MigraMongo migraMongo;
	private Gson gson;

	public MigraMongoJMX(MigraMongo migraMongo) {
		this.migraMongo = migraMongo;
	}

	/**
	 * Performs the migration operation. See
	 * {@link com.rinoto.migramongo.MigraMongo#migrate()}
	 * 
	 * @return the {@link com.rinoto.migramongo.MigraMongoStatus} in JSON format
	 */
	@ManagedOperation
	public String migrate() {
		return toJson(migraMongo.migrate());
	}

	/**
	 * Performs the migration operation in an asynchronous way. See
	 * {@link com.rinoto.migramongo.MigraMongo#migrateAsync()}
	 * 
	 * @return the {@link com.rinoto.migramongo.MigraMongoStatus} in JSON
	 *         format. If the status is OK, it means that there is nothing to
	 *         migrate. If status is IN_PROGRESS, it means that the migration
	 *         process has started, and you need to call the
	 *         {@link #status(String)} method to check the status.
	 */
	@ManagedOperation
	public String migrateAsync() {
		return toJson(migraMongo.migrateAsync());
	}

	/**
	 * Delivers the list of scripts that would be run, if we would run the
	 * migration. See {@link com.rinoto.migramongo.MigraMongo#dryRun()}
	 * <p>
	 * <b>This method doesn't perform the migration</b>
	 * 
	 * @return the {@link com.rinoto.migramongo.MigraMongoStatus} in JSON format
	 */
	@ManagedOperation
	public String dryRun() {
		return toJson(migraMongo.dryRun());
	}

	/**
	 * It retrieves the status of the migration entries since the specified
	 * version. See {@link com.rinoto.migramongo.MigraMongo#status(String)}
	 * 
	 * @param fromVersion
	 *            the version from which we want the status to be retrieved. If
	 *            null, the whole status hitory would be retrieved.
	 * @return the {@link com.rinoto.migramongo.MigraMongoStatus} in JSON
	 *         format.
	 */
	@ManagedOperation
	public String status(String fromVersion) {
		return toJson(migraMongo.status(fromVersion));
	}

	/**
	 * @return true if migration is needed. false otherwise.
	 */
	@ManagedOperation
	public Boolean needsMigration() {
		return !migraMongo.dryRun().migrationsApplied.isEmpty();
	}

	/**
	 * @return all migration entries as list of
	 *         {@link com.rinoto.migramongo.MigrationEntry} in JSON format.
	 */
	@ManagedOperation
	public String history() {
		return toJson(migraMongo.getMigrationEntries());
	}

	/**
	 * destroys (re-inits) the locks - useful in the case when the lock gets
	 * corrupted
	 */
	@ManagedOperation
	public void destroyLocks() {
		migraMongo.destroyLocks();
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
