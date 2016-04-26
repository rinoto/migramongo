package com.rinoto.migramongo.lookup;

import java.util.Collection;

import com.rinoto.migramongo.InitialMongoMigrScript;
import com.rinoto.migramongo.MongoMigrScript;

public interface ScriptLookupService {

    InitialMongoMigrScript findInitialScript();

    Collection<MongoMigrScript> findMongoScripts();
}
