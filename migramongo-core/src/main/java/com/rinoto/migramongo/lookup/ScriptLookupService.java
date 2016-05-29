package com.rinoto.migramongo.lookup;

import java.util.Collection;

import com.rinoto.migramongo.InitialMongoMigrationScript;
import com.rinoto.migramongo.MongoMigrationScript;

public interface ScriptLookupService {

    InitialMongoMigrationScript findInitialScript();

    Collection<MongoMigrationScript> findMongoScripts();
}
