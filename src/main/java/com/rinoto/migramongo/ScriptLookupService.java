package com.rinoto.migramongo;

import java.util.Collection;

public interface ScriptLookupService {

    InitialMongoMigrationScript findInitialScript();

    Collection<MongoMigrationScript> findMongoScripts();
}
