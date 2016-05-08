package com.rinoto.migramongo.spring;

import org.springframework.context.ApplicationContext;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.dao.MigrationHistoryService;
import com.rinoto.migramongo.dao.MongoMigrationHistoryService;

public class SpringMigraMongo extends MigraMongo {

	public SpringMigraMongo(ApplicationContext appContext, MongoDatabase database) {
		this(appContext, database, new MongoMigrationHistoryService(database));
	}

	public SpringMigraMongo(ApplicationContext appContext, MongoDatabase database,
			MigrationHistoryService migrationHistoryService) {
		super(database, migrationHistoryService, new SpringScriptLookupService(appContext));
	}

}