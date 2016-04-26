package com.rinoto.migramongo.spring;

import org.springframework.context.ApplicationContext;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.dao.MongoMigrationEntryService;

public class SpringMigraMongo extends MigraMongo {

    public SpringMigraMongo(ApplicationContext appContext, MongoDatabase database) {
        super(database, new MongoMigrationEntryService(database), new SpringScriptLookupService(appContext));
    }

}
