package com.rinoto.migramongo.spring;

import org.springframework.context.ApplicationContext;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.dao.MongoLockService;
import com.rinoto.migramongo.dao.MongoMigrationHistoryService;
import com.rinoto.migramongo.spring.lookup.SpringScriptLookupService;

public class SpringMigraMongo extends MigraMongo {

	public SpringMigraMongo(ApplicationContext appContext, MongoDatabase database) {
		super(database, new MongoMigrationHistoryService(database), new MongoLockService(database),
				new SpringScriptLookupService(appContext));
	}

}
