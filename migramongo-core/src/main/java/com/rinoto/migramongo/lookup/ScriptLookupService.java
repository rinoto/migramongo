package com.rinoto.migramongo.lookup;

import java.util.Collection;

import com.rinoto.migramongo.InitialMongoMigrationScript;
import com.rinoto.migramongo.MongoMigrationScript;

/**
 * Service to look up the scripts
 * 
 * @author rinoto
 */
public interface ScriptLookupService {

    /**
     * Delivers the initialMigrationScript, or null if not found.
     * <p>
     * If more than one script is found, an <code>IllegalStateException</code> will be thrown.
     * 
     * @return the initialMigrationScript, or null if not found.
     */
    InitialMongoMigrationScript findInitialScript();

    /**
     * The mongo scripts found. Collection may be empty.
     * 
     * @return mongo scripts found
     */
    Collection<MongoMigrationScript> findMongoScripts();
}
