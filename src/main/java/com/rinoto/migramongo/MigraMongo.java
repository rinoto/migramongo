package com.rinoto.migramongo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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
		MigrationEntry lastMigrationApplied = getLastMigrationApplied();
		if (lastMigrationApplied == null) {
			final Object initialMigrationScript = getInitialMigrationScript();
			if (initialMigrationScript == null) {
				return new MigraMongoStatus("ERROR",
						"no last migration script found, and no initial migration script provided!");
			}
			lastMigrationApplied = executeInitialMigrationScript(initialMigrationScript);
		}
		return MigraMongoStatus.OK;
	}

	private MigrationEntry executeInitialMigrationScript(Object initialMigrationScript) {
		InitialMongoMigrationScript annotation = initialMigrationScript.getClass().getAnnotation(
				InitialMongoMigrationScript.class);
		if (annotation == null) {
			throw new IllegalStateException("InitialMongoMigrationScript " + initialMigrationScript + " of class "
					+ initialMigrationScript.getClass().getName()
					+ " doesn't have an InitialMongoMigrationScript annotation!");
		}
		final Method method = getMigrationMethod(initialMigrationScript);
		try {
			executeMethod(method, initialMigrationScript);
			return setInitialVersionInMigrationCollection(annotation, null);
		} catch (Exception e) {
			return setInitialVersionInMigrationCollection(annotation, e);
		}
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

	private Method getMigrationMethod(Object initialMigrationScript) {
		final Method[] methods = initialMigrationScript.getClass().getDeclaredMethods();
		final List<Method> publicMethods = Stream.of(methods).filter(m -> Modifier.isPublic(m.getModifiers()))
				.collect(Collectors.toList());
		if (publicMethods.isEmpty()) {
			throw new IllegalStateException("MigrationScript " + initialMigrationScript + " of class "
					+ initialMigrationScript.getClass().getName() + " doesn't have any public method!");
		}
		if (publicMethods.size() > 1) {
			throw new IllegalStateException("MigrationScript " + initialMigrationScript + " of class "
					+ initialMigrationScript.getClass().getName() + " has " + publicMethods.size()
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
		doc.put("fromVersion", migEntry.fromVersion);
		doc.put("toVersion", migEntry.toVersion);
		doc.put("createdAt", migEntry.createdAt);
		doc.put("module", migEntry.module);
		doc.put("info", migEntry.info);
		doc.put("status", migEntry.status);
		doc.put("statusMessage", migEntry.statusMessage);
		return doc;
	}

	public Collection<Object> getMigrationScripts() {
		return appContext.getBeansWithAnnotation(MongoMigrationScript.class).values();
	}

	public Object getInitialMigrationScript() {
		final Collection<Object> values = appContext.getBeansWithAnnotation(InitialMongoMigrationScript.class).values();
		if (values.isEmpty()) {
			return null;
		}
		if (values.size() > 1) {
			throw new IllegalStateException("There cannot be more than one InitialMigrationScript!. Found "
					+ values.size() + ": " + values);
		}
		return values.iterator().next();
	}

	public void applyMigration(Object migrationObject) {

	}

}
