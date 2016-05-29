package com.rinoto.migramongo.spring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.spring.jmx.MigraMongoJMX;

@RunWith(MockitoJUnitRunner.class)
public class MigraMongoJMXTest {

    @InjectMocks
    MigraMongoJMX migraMongoJMX;
    @Mock
    MigraMongo migraMongo;

    @Test
    public void shouldReturnCorrectJsonWithSimpleStatus() {
        //given 
        when(migraMongo.migrate()).thenReturn(MigraMongoStatus.ok());

        //when
        final String jsonStatus = migraMongoJMX.migrate();

        //then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status.migrationsApplied, hasSize(0));
        assertThat(status.status, is(MigrationStatus.OK));
    }

    @Test
    public void shouldReturnCorrectJsonWhenMigrationsApplied() {
        //given 
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry());
        when(migraMongo.migrate()).thenReturn(migraMongoStatus);

        //when
        final String jsonStatus = migraMongoJMX.migrate();

        //then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status, samePropertyValuesAs(migraMongoStatus));
    }

    @Test
    public void shouldReturnCorrectJsonWhenMigrationsAppliedInDryRun() {
        //given 
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry());
        when(migraMongo.dryRun()).thenReturn(migraMongoStatus);

        //when
        final String jsonStatus = migraMongoJMX.dryRun();

        //then
        final MigraMongoStatus status = new Gson().fromJson(jsonStatus, MigraMongoStatus.class);
        assertThat(status, samePropertyValuesAs(migraMongoStatus));
    }

    @Test
    public void shouldReturnNeedsMigrationTrueWhenMigrationsToApply() {
        //given 
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok().addEntry(createMigrationEntry());
        when(migraMongo.dryRun()).thenReturn(migraMongoStatus);

        //when
        final Boolean needsMigration = migraMongoJMX.needsMigration();

        //then
        assertThat(needsMigration, is(true));
    }

    @Test
    public void shouldReturnNeedsMigrationFalseWhenNoMigrationsToApply() {
        //given 
        final MigraMongoStatus migraMongoStatus = MigraMongoStatus.ok();
        when(migraMongo.dryRun()).thenReturn(migraMongoStatus);

        //when
        final Boolean needsMigration = migraMongoJMX.needsMigration();

        //then
        assertThat(needsMigration, is(false));
    }

    private MigrationEntry createMigrationEntry() {
        final MigrationEntry entry = new MigrationEntry();
        entry.setCreatedAt(new Date());
        entry.setFromVersion("3.0");
        entry.setStatus(MigrationStatus.IN_PROGRESS);
        entry.setUpdatedAt(new Date());
        return entry;
    }

}
