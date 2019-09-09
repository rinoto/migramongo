package com.rinoto.migramongo.spring.jmx;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;

@RunWith(MockitoJUnitRunner.class)
public class MigraMongoJMXTest {

    @InjectMocks
    MigraMongoJMX migraMongoJMX;
    @Mock
    MigraMongo migraMongo;

    @Test
    public void shouldReturnCorrectJsonWithSimpleStatus() {
        // given
        when(migraMongo.migrate()).thenReturn(MigraMongoStatus.ok());

        // when
        final String jsonStatus = migraMongoJMX.migrate();

        // then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status.migrationsApplied, hasSize(0));
        assertThat(status.status, is(MigrationStatus.OK));
    }

    @Test
    public void shouldReturnCorrectJsonWhenMigrationsApplied() {
        // given
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry("3.0", "4.0"));
        when(migraMongo.migrate()).thenReturn(migraMongoStatus);

        // when
        final String jsonStatus = migraMongoJMX.migrate();

        // then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status, samePropertyValuesAs(migraMongoStatus));
    }

    @Test
    public void shouldReturnCorrectJsonWhenMigrationsAsynchApplied() {
        // given
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry("3.0", "4.0"));
        when(migraMongo.migrateAsync()).thenReturn(migraMongoStatus);

        // when
        final String jsonStatus = migraMongoJMX.migrateAsync();

        // then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status, samePropertyValuesAs(migraMongoStatus));
    }

    @Test
    public void shouldReturnCorrectJsonWhenMigrationsAppliedInDryRun() {
        // given
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry("3.0", "4.0"));
        when(migraMongo.dryRun()).thenReturn(migraMongoStatus);

        // when
        final String jsonStatus = migraMongoJMX.dryRun();

        // then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status, samePropertyValuesAs(migraMongoStatus));
    }

    @Test
    public void shouldReturnCorrectJsonWhenStatus() {
        // given
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry("3.0", "4.0"));
        when(migraMongo.status("1")).thenReturn(migraMongoStatus);

        // when
        final String jsonStatus = migraMongoJMX.status("1");

        // then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status, samePropertyValuesAs(migraMongoStatus));
    }

    @Test
    public void shouldReturnNeedsMigrationTrueWhenMigrationsToApply() {
        // given
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry("3.0", "4.0"));
        when(migraMongo.dryRun()).thenReturn(migraMongoStatus);

        // when
        final Boolean needsMigration = migraMongoJMX.needsMigration();

        // then
        assertThat(needsMigration, is(true));
    }

    @Test
    public void shouldReturnNeedsMigrationFalseWhenNoMigrationsToApply() {
        // given
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok();
        when(migraMongo.dryRun()).thenReturn(migraMongoStatus);

        // when
        final Boolean needsMigration = migraMongoJMX.needsMigration();

        // then
        assertThat(needsMigration, is(false));
    }

    @Test
    public void shouldReturnAppliedMigrations() {
        // given
        final List<MigrationEntry> entries = Arrays
            .asList(createMigrationEntry("3.0", "4.0"), createMigrationEntry("4.0", "5.0"));
        when(migraMongo.getMigrationEntries()).thenReturn(entries);

        // when
        final String history = migraMongoJMX.history();

        // then
        final List<MigrationEntry> entriesRetrieved = new Gson()
            .fromJson(history, new TypeToken<List<MigrationEntry>>() {}.getType());
        assertThat(entriesRetrieved, hasSize(entries.size()));
        for (int i = 0; i < entries.size(); i++ ) {
            assertThat(
                entries.get(i),
                allOf(
                    hasProperty("fromVersion", is(entriesRetrieved.get(i).getFromVersion())),
                    hasProperty("toVersion", is(entriesRetrieved.get(i).getToVersion()))));
        }
    }

    @Test
    public void shouldNotReturnAppliedMigrationsWhenNoneAvailable() {
        // given
        when(migraMongo.getMigrationEntries()).thenReturn(Collections.emptyList());

        // when
        final String history = migraMongoJMX.history();

        // then
        final List<MigrationEntry> entriesRetrieved = new Gson()
            .fromJson(history, new TypeToken<List<MigrationEntry>>() {}.getType());
        assertThat(entriesRetrieved, hasSize(0));
    }

    @Test
    public void shouldDestroyLock() {
        // when
        migraMongoJMX.destroyLocks();
        // then
        verify(migraMongo).destroyLocks();
    }

    private MigrationEntry createMigrationEntry(String fromVersion, String toVersion) {
        final MigrationEntry entry = new MigrationEntry();
        entry.setId(ObjectId.get());
        entry.setCreatedAt(new Date());
        entry.setFromVersion(fromVersion);
        entry.setToVersion(toVersion);
        entry.setStatus(MigrationStatus.IN_PROGRESS);
        entry.setUpdatedAt(new Date());
        return entry;
    }

}
