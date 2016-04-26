package com.rinoto.migramongo.lookup;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.rinoto.migramongo.InitialMongoMigrScript;
import com.rinoto.migramongo.MongoMigrScript;

public class SpringScriptLookupService implements ScriptLookupService {

    private final ApplicationContext appContext;

    public SpringScriptLookupService(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public InitialMongoMigrScript findInitialScript() {
        // final Collection<Object> values =
        // appContext.getBeansWithAnnotation(InitialMongoMigrationScript.class).values();
        Collection<InitialMongoMigrScript> values = appContext.getBeansOfType(InitialMongoMigrScript.class).values();
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
    public Collection<MongoMigrScript> findMongoScripts() {
        final Collection<MongoMigrScript> migScripts = appContext
            .getBeansOfType(MongoMigrScript.class)
            .values()
            .stream()
            .filter(ms -> !InitialMongoMigrScript.class.isInstance(ms))
            .collect(Collectors.toList());
        return migScripts;
    }

}
