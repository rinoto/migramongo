package com.rinoto.migramongo.reflections.lookup;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;

import com.rinoto.migramongo.InitialMongoMigrationScript;
import com.rinoto.migramongo.MongoMigrationScript;
import com.rinoto.migramongo.lookup.ScriptLookupService;

/**
 * Implementation of {@link com.rinoto.migramongo.lookup.ScriptLookupService} that uses the <a href="https://github.com/ronmamo/reflections">ronmamo reflections library</a> to find the classes implemeting the {@link com.rinoto.migramongo.MongoMigrationScript}s
 * 
 * @author rinoto
 */
public class ReflectionsScriptLookupService implements ScriptLookupService {

    final Reflections reflections;

    public ReflectionsScriptLookupService(String basePackage) {
        reflections = new Reflections(basePackage);
    }

    @Override
    public InitialMongoMigrationScript findInitialScript() {

        final Set<Class<? extends InitialMongoMigrationScript>> initialMongoScriptClasses = reflections
            .getSubTypesOf(InitialMongoMigrationScript.class);
        if (initialMongoScriptClasses.isEmpty()) {
            return null;
        }
        if (initialMongoScriptClasses.size() > 1) {
            throw new IllegalStateException(
                "There cannot be more than one InitialMigrationScript!. Found " +
                    initialMongoScriptClasses.size() +
                    ": " +
                    initialMongoScriptClasses);
        }
        final Class<? extends InitialMongoMigrationScript> initialMongoScriptClass = initialMongoScriptClasses
            .iterator()
            .next();
        return instantiate(initialMongoScriptClass);
    }

    private <T extends MongoMigrationScript> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Exception while instantiating " + clazz, e);
        }
    }

    @Override
    public Collection<MongoMigrationScript> findMongoScripts() {

        final Set<Class<? extends MongoMigrationScript>> mongoScriptClasses = reflections
            .getSubTypesOf(MongoMigrationScript.class);

        final List<MongoMigrationScript> migScripts = mongoScriptClasses
            .stream()
            .filter(ms -> !InitialMongoMigrationScript.class.isAssignableFrom(ms))
            .map(ms -> (MongoMigrationScript) instantiate(ms))
            .collect(Collectors.toList());

        return migScripts;
    }

}
