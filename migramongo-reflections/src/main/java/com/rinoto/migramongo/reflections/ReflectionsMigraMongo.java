package com.rinoto.migramongo.reflections;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.dao.MongoLockService;
import com.rinoto.migramongo.dao.MongoMigrationHistoryService;
import com.rinoto.migramongo.reflections.lookup.ReflectionsScriptLookupService;

/**
 * {@link com.rinoto.migramongo.MigraMongo} instance that uses the <a href="https://github.com/ronmamo/reflections">ronmamo reflections library</a> as discovery mechanism.
 * 
 * @author rinoto
 */
public class ReflectionsMigraMongo extends MigraMongo {

    public ReflectionsMigraMongo(MongoDatabase database, String basePackage) {
        super(
            database,
            new MongoMigrationHistoryService(database),
            new MongoLockService(database),
            new ReflectionsScriptLookupService(basePackage));
    }

}
