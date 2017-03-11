package com.rinoto.migramongo.reflections.lookup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collection;

import org.junit.Test;

import com.rinoto.migramongo.InitialMongoMigrationScript;
import com.rinoto.migramongo.MongoMigrationScript;
import com.rinoto.migramongo.reflections.lookup.classes.InitialMongoScriptForTesting;
import com.rinoto.migramongo.reflections.lookup.multiple.InitialMongoScriptForTesting1;
import com.rinoto.migramongo.reflections.lookup.wrongclasses.IllegalInitialMigrationScript;

public class ReflectionsScriptLookupTest {

    @Test
    public void shouldFindInitialMigrationScript() {
        //given
        final ReflectionsScriptLookupService lookup = new ReflectionsScriptLookupService(
            InitialMongoScriptForTesting.class.getPackage().getName());

        //when
        final InitialMongoMigrationScript findInitialScript = lookup.findInitialScript();

        //then
        assertThat(findInitialScript.getMigrationInfo().getFromVersion(), is("1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFindMongoScripts() {
        //given
        final ReflectionsScriptLookupService lookup = new ReflectionsScriptLookupService(
            InitialMongoScriptForTesting.class.getPackage().getName());

        //when
        final Collection<MongoMigrationScript> mongoScripts = lookup.findMongoScripts();

        //then
        assertThat(mongoScripts, hasSize(3));
        assertThat(
            mongoScripts,
            containsInAnyOrder(
                hasProperty("migrationInfo", hasProperty("fromVersion", is("1"))),
                hasProperty("migrationInfo", hasProperty("fromVersion", is("2"))),
                hasProperty("migrationInfo", hasProperty("fromVersion", is("3")))));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailIfClassIsAbstract() {
        //given
        final ReflectionsScriptLookupService lookup = new ReflectionsScriptLookupService(
            IllegalInitialMigrationScript.class.getPackage().getName());

        //when - expect exception
        lookup.findInitialScript();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailIfItFindsMoreThanOneImplementationOfInitialScripts() {
        //given
        final ReflectionsScriptLookupService lookup = new ReflectionsScriptLookupService(
            InitialMongoScriptForTesting1.class.getPackage().getName());

        //when - expect exception
        lookup.findInitialScript();
    }

    @Test
    public void shouldReturnNullIfNoInitialScriptFound() {
        //given
        final ReflectionsScriptLookupService lookup = new ReflectionsScriptLookupService("com.rinoto.nonexisting");

        //when - expect exception
        final InitialMongoMigrationScript found = lookup.findInitialScript();

        assertThat(found, nullValue());
    }

}
