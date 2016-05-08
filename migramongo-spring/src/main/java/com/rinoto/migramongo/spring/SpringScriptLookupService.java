package com.rinoto.migramongo.spring;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.rinoto.migramongo.InitialMongoMigrationScript;
import com.rinoto.migramongo.MongoMigrationScript;
import com.rinoto.migramongo.ScriptLookupService;

public class SpringScriptLookupService implements ScriptLookupService {

    private final ApplicationContext appContext;

    public SpringScriptLookupService(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public InitialMongoMigrationScript findInitialScript() {
        // final Collection<Object> values =
        // appContext.getBeansWithAnnotation(InitialMongoMigrationScript.class).values();
        Collection<InitialMongoMigrationScript> values = appContext.getBeansOfType(InitialMongoMigrationScript.class).values();
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException("There cannot be more than one InitialMigrationScript!. Found " +
                values.size() +
                ": " +
                values);
        }
        return values.iterator().next();
    }

    @Override
    public Collection<MongoMigrationScript> findMongoScripts() {
        final Collection<MongoMigrationScript> migScripts = appContext
            .getBeansOfType(MongoMigrationScript.class)
            .values()
            .stream()
            .filter(ms -> !InitialMongoMigrationScript.class.isInstance(ms))
            .collect(Collectors.toList());
        return migScripts;
    }

}
