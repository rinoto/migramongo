package com.rinoto.migramongo.reflections.lookup;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.github.classgraph.*;

import com.rinoto.migramongo.InitialMongoMigrationScript;
import com.rinoto.migramongo.MongoMigrationScript;
import com.rinoto.migramongo.lookup.ScriptLookupService;

/**
 * Implementation of {@link com.rinoto.migramongo.lookup.ScriptLookupService}
 * that uses the <a href="https://github.com/classgraph/classgraph">ClassGraph reflections library</a> to find the classes implemeting the {@link com.rinoto.migramongo.MongoMigrationScript}s
 *
 * @author rinoto
 */
public class ReflectionsScriptLookupService implements ScriptLookupService {

    final ClassGraph classGraph;

    public ReflectionsScriptLookupService(String basePackage) {
        classGraph = new ClassGraph().enableAllInfo().acceptPackages(basePackage);
    }

    @Override
    public InitialMongoMigrationScript findInitialScript() {

        try (ScanResult scanResult = classGraph.scan()) {
            final List<Class<? extends InitialMongoMigrationScript>> initialMongoScriptClasses =
                    scanResult.getClassesImplementing(InitialMongoMigrationScript.class.getName())
                            .stream()
                            .map(routeClassInfo -> (Class<? extends InitialMongoMigrationScript>) routeClassInfo.loadClass())
                            .collect(Collectors.toList());

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
            final Class<? extends InitialMongoMigrationScript> initialMongoScriptClass = initialMongoScriptClasses.get(0);
            return instantiate(initialMongoScriptClass);
        }
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

        try (ScanResult scanResult = classGraph.scan()) {
            final List<MongoMigrationScript> migScripts =
                    scanResult.getClassesImplementing(MongoMigrationScript.class.getName())
                            .stream()
                            .map(routeClassInfo -> (Class<? extends MongoMigrationScript>) routeClassInfo.loadClass())
                            .filter(ms -> !InitialMongoMigrationScript.class.isAssignableFrom(ms))
                            .map(ms -> (MongoMigrationScript) instantiate(ms))
                            .collect(Collectors.toList());
            return migScripts;
        }
    }

}
