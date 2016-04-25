package com.rinoto.migramongo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
				return new MigraMongoStatus("ERROR",
						"no last migration script found, and no initial migration script provided!");
			}
			lastMigrationApplied = executeInitialMigrationScript(initialMigrationScript);
			status.addEntry(lastMigrationApplied);
		}
		getMigrationScriptsToApply(lastMigrationApplied.toVersion);

		return status;
	}

	private MigrationEntry executeInitialMigrationScript(Object initialMigrationScript) {
		InitialMongoMigrationScript annotation = initialMigrationScript.getClass().getAnnotation(
				InitialMongoMigrationScript.class);
		if (annotation == null) {
			throw new IllegalStateException("InitialMongoMigrationScript " + initialMigrationScript + " of class "
					+ initialMigrationScript.getClass().getName()
					+ " doesn't have an InitialMongoMigrationScript annotation!");
		}
		try {
			executeMigrationScriptMethod(initialMigrationScript);
			return setInitialVersionInMigrationCollection(annotation, null);
		} catch (Exception e) {
			return setInitialVersionInMigrationCollection(annotation, e);
		}
	}

	private void executeMigrationScriptMethod(Object migrationScriptObject) {
		final Method method = getMigrationMethod(migrationScriptObject);
		executeMethod(method, migrationScriptObject);
	}

	private MigrationEntry setInitialVersionInMigrationCollection(InitialMongoMigrationScript annotation, Exception e) {
		final MigrationEntry migEntry = new MigrationEntry();
		migEntry.module = annotation.module();
		migEntry.status = "OK";
		migEntry.fromVersion = annotation.version();
		migEntry.toVersion = annotation.version();
		migEntry.info = annotation.info();

		final Document document = mapMigEntryToDocument(migEntry);
		// TODO - where is the _id???
		getMigramongoCollection().insertOne(document);
		migEntry.id = document.getString("_id");
		return migEntry;

	}

	private void executeMethod(Method method, Object obj) {
		final Parameter[] parameters = method.getParameters();
		if (parameters == null || parameters.length == 0) {
			invoke(method, obj);
		} else if (parameters.length == 1 && parameters[0].getClass().isAssignableFrom(MongoDatabase.class)) {
			invoke(method, obj, database);
		} else {
			throw new IllegalStateException("method " + method + " in class " + obj.getClass()
					+ " has either one parameter that is not of type MongoDatabase, or more than 1 parameter");
		}

	}

	private void invoke(Method method, Object obj, Object... args) {
		try {
			method.invoke(obj, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Exception when invoking method " + method + " from MigrationScript " + obj
					+ " of class " + obj.getClass().getName(), e);
		}
	}

	private Method getMigrationMethod(Object migrationScriptObject) {
		final Method[] methods = migrationScriptObject.getClass().getDeclaredMethods();
		final List<Method> publicMethods = Stream.of(methods).filter(m -> Modifier.isPublic(m.getModifiers()))
				.collect(Collectors.toList());
		if (publicMethods.isEmpty()) {
			throw new IllegalStateException("MigrationScript " + migrationScriptObject + " of class "
					+ migrationScriptObject.getClass().getName() + " doesn't have any public method!");
		}
		if (publicMethods.size() > 1) {
			throw new IllegalStateException("MigrationScript " + migrationScriptObject + " of class "
					+ migrationScriptObject.getClass().getName() + " has " + publicMethods.size()
					+ " public methods, but it should have only 1");

		}
		return publicMethods.get(0);
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
		migEntry.id = doc.getString("_id");
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
		doc.put("_id", migEntry.id);
		doc.put("fromVersion", migEntry.fromVersion);
		doc.put("toVersion", migEntry.toVersion);
		doc.put("createdAt", migEntry.createdAt);
		doc.put("module", migEntry.module);
		doc.put("info", migEntry.info);
		doc.put("status", migEntry.status);
		doc.put("statusMessage", migEntry.statusMessage);
		return doc;
	}

	public List<MigrationScript> getMigrationScriptsToApply(String version) {
		// final Collection<Object> migScripts =
		// appContext.getBeansWithAnnotation(MongoMigrationScript.class).values();
		final Collection<MongoMigrScript> migScripts = appContext.getBeansOfType(MongoMigrScript.class).values()
				.stream().filter(ms -> !InitialMongoMigrScript.class.isInstance(ms)).collect(Collectors.toList());
		// final List<MigrationScript> allMigrationScripts = migScripts.stream()
		// .map(migScript -> new
		// MigrationScript(migScript)).collect(Collectors.toList());

		// }).collect(Collectors.toList());
		final List<MigrationScript> migScriptsToApply = findMigScriptsToApply(version, migScripts);
		return migScriptsToApply;
	}

	private List<MigrationScript> findMigScriptsToApply(String version, Collection<MongoMigrScript> allMigrationScripts) {
		if (allMigrationScripts.isEmpty()) {
			return new ArrayList<>();
		}
		final List<MongoMigrScript> candidates = new ArrayList<>();
		final List<MongoMigrScript> rest = new ArrayList<>();
		for (MongoMigrScript ms : allMigrationScripts) {
			if (ms.getMigrationInfo().getFromVersion().equals("version")) {
				candidates.add(ms);
			} else {
				rest.add(ms);
			}
		}
		if (candidates.isEmpty()) {
			return new ArrayList<>();
		}
		if (candidates.size() > 1) {
			throw new IllegalStateException("There is more than one script with fromVersion " + version + ": "
					+ allMigrationScripts);
		}
		final MongoMigrScript nextMigrationScript = candidates.get(0);
		final List<MigrationScript> nextMigScriptsRec = findMigScriptsToApply(nextMigrationScript.getMigrationInfo()
				.getToVersion(), rest);
		nextMigScriptsRec.add(nextMigrationScript);
		return nextMigScripts;
	}

	public MongoMigrScript getInitialMigrationScript() {
		// final Collection<Object> values =
		// appContext.getBeansWithAnnotation(InitialMongoMigrationScript.class).values();
		Collection<InitialMongoMigrScript> values = appContext.getBeansOfType(InitialMongoMigrScript.class).values();
		if (values.isEmpty()) {
			return null;
		}
		if (values.size() > 1) {
			throw new IllegalStateException("There cannot be more than one InitialMigrationScript!. Found "
					+ values.size() + ": " + values);
		}
		return values.iterator().next();
	}

	public void applyMigration(MigrationScript migrationObject) {

	}

	public class MigrationScript {

		private final MongoMigrationScript mongoMigrationScript;
		private final Object migScriptObject;

		MigrationScript(Object migScriptObject) {
			this.migScriptObject = migScriptObject;
			this.mongoMigrationScript = migScriptObject.getClass().getAnnotation(MongoMigrationScript.class);
		}

		MongoMigrationScript getMigrationInfo() {
			return mongoMigrationScript;
		}

		void migrate() {
			executeMigrationScriptMethod(migScriptObject);
		}

	}

}
