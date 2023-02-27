package com.rinoto.migramongo.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import com.rinoto.migramongo.EmbeddedMongo;
import com.rinoto.migramongo.InitialMigrationInfo;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.MigrationRun;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MongoMigrationHistoryServiceTest {

    public static EmbeddedMongo mongo = new EmbeddedMongo();

    MongoMigrationHistoryService migrationHistoryService;

    @BeforeAll
    public static void startMongo() throws Exception {
        mongo.start();
    }

    @AfterAll
    public static void stopMongo() {
        mongo.stop();
    }

    @BeforeEach
    public void clearMigrations() {
        migrationHistoryService = new MongoMigrationHistoryService(mongo.getClient().getDatabase("migramongotest"));
        migrationHistoryService.getMigramongoCollection().drop();
    }

    @Test
    public void shouldNotReturnLastMigrationIfNoneExists() throws Exception {
        assertThat(migrationHistoryService.getLastMigrationApplied(), nullValue());
    }

    @Test
    public void shouldAddInitialMigrationInfo() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        // when - then
        assertThat(migrationHistoryService.getLastMigrationApplied(), hasProperty("fromVersion", is("1")));
    }

    @Test
    public void shouldSetMigrationStatusToFinished() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        final MigrationEntry migInProgress = migrationHistoryService
            .insertMigrationStatusInProgress(new MigrationInfo("1", "2"));
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.IN_PROGRESS)),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));

        // when
        migrationHistoryService.setMigrationStatusToFinished(migInProgress);

        // then
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.OK)),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));
    }

    @Test
    public void shouldSetMigrationStatusToError() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        final MigrationEntry migInProgress = migrationHistoryService
            .insertMigrationStatusInProgress(new MigrationInfo("1", "2"));
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.IN_PROGRESS)),
                hasProperty("skipped", is(false)),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));

        // when
        migrationHistoryService
            .setMigrationStatusToFailed(migInProgress, new RuntimeException("manually set to error"));

        // then
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.ERROR)),
                hasProperty("statusMessage", is("manually set to error")),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));
    }

    @Test
    public void shouldInsertSkippedMigration() throws Exception {
        // when
        migrationHistoryService.insertMigrationStatusSkipped(new MigrationInfo("1", "2"));
        //then
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.OK)),
                hasProperty("skipped", is(true)),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));
    }

    @Test
    public void shouldSetMigrationStatusToRepaired() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        final MigrationEntry migInProgress = migrationHistoryService
            .insertMigrationStatusInProgress(new MigrationInfo("1", "2"));
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.IN_PROGRESS)),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));

        // when
        migrationHistoryService.setMigrationStatusToManuallyRepaired(migInProgress);

        // then
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(
                hasProperty("status", is(MigrationStatus.OK)),
                hasProperty("repaired", is(true)),
                hasProperty("fromVersion", is("1")),
                hasProperty("toVersion", is("2"))));
    }

    @Test
    public void shouldAddInitialAndOneMigrationInfo() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when - then
        assertThat(
            migrationHistoryService.getLastMigrationApplied(),
            allOf(hasProperty("fromVersion", is("1")), hasProperty("toVersion", is("2"))));
    }

    @Test
    public void shouldFindMigrationEntryIfAvailable() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when - then
        assertThat(
            migrationHistoryService.findMigration("1", "2"),
            allOf(hasProperty("fromVersion", is("1")), hasProperty("toVersion", is("2"))));
    }

    @Test
    public void shouldNotFindMigrationsIfNoneAvailable() throws Exception {
        // when
        final List<MigrationEntry> migrations = toList(migrationHistoryService.findMigrations("1"));

        // then
        assertThat(migrations, hasSize(0));
    }

    @Test
    public void shouldFindAllMigrationsIfFromIsNull() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when
        final List<MigrationEntry> migrations = toList(migrationHistoryService.findMigrations(null));

        // when - then
        assertThat(migrations, hasSize(2));
    }

    @Test
    public void shouldFindMigrationsFromACorrectInitialVersion() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when
        final List<MigrationEntry> migrations = toList(migrationHistoryService.findMigrations("1"));

        // when - then
        assertThat(migrations, hasSize(2));
        assertThat(migrations.get(0).getFromVersion(), is("1"));
    }

    @Test
    public void shouldNotFindMigrationsIfFromDoesntExist() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when
        final List<MigrationEntry> migrations = toList(migrationHistoryService.findMigrations("42"));

        // when - then
        assertThat(migrations, hasSize(0));
    }

    @Test
    public void shouldFindMigrationsFromACorrectNonInitialVersion() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));
        addMigration(new MigrationInfo("2", "3"));
        addMigration(new MigrationInfo("3", "4"));
        addMigration(new MigrationInfo("4", "5"));

        // when
        final List<MigrationEntry> migrations = toList(migrationHistoryService.findMigrations("2"));

        // when - then
        assertThat(migrations, hasSize(3));
        assertThat(migrations.get(0).getFromVersion(), is("2"));
        assertThat(migrations.get(2).getFromVersion(), is("4"));
    }

    @Test
    public void shouldFindAllMigrationEntries() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when
        final List<MigrationEntry> allMigrationsApplied = toList(migrationHistoryService.getAllMigrationsApplied());

        // then
        assertThat(allMigrationsApplied, hasSize(2));
    }

    @Test
    public void shouldNotFindMigrationEntriesIfNoneExist() throws Exception {
        // when
        final List<MigrationEntry> allMigrationsApplied = toList(migrationHistoryService.getAllMigrationsApplied());

        // then
        assertThat(allMigrationsApplied, hasSize(0));
    }

    @Test
    public void shouldNotFindMigrationEntryIfNotAvailable() throws Exception {
        // given
        addMigration(new InitialMigrationInfo("1"));
        addMigration(new MigrationInfo("1", "2"));

        // when - then
        assertThat(migrationHistoryService.findMigration("5", "6"), nullValue());
    }

    @Test
    public void shouldAddRerunToMigEntry() throws Exception {
        // given
        final MigrationEntry migEntry = addMigration(new MigrationInfo("1", "2"));
        final MigrationRun migRun = new MigrationRun().update(MigrationStatus.OK, "it's ok");

        // when
        migrationHistoryService.addRunToMigrationEntry(migEntry, migRun);

        final MigrationEntry migEntyUpdated = migrationHistoryService.findMigration("1", "2");
        assertThat(migEntyUpdated.getReruns(), hasSize(1));
        assertMigrationRun(migEntyUpdated.getReruns().get(0), migRun);
    }

    @Test
    public void shouldNotAddRerunToMigEntryIfEntryDoesNotExist() throws Exception {
        // given
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.setFromVersion("1");
        migEntry.setToVersion("2");
        final MigrationRun migRun = new MigrationRun().update(MigrationStatus.OK, "it's ok");

        // when
        final MigrationEntry migEntryWithRuns = migrationHistoryService.addRunToMigrationEntry(migEntry, migRun);

        assertThat(migEntryWithRuns, nullValue());
    }

    @Test
    public void shouldUpdateEntryStatusIfRunIsAdded() throws Exception {
        // given
        final MigrationEntry migEntry = addMigration(new MigrationInfo("1", "2"));
        final MigrationRun migRun = new MigrationRun().update(MigrationStatus.IN_PROGRESS, "in progress");

        // when
        final MigrationEntry migEntryWithRuns = migrationHistoryService.addRunToMigrationEntry(migEntry, migRun);

        assertThat(migEntryWithRuns.getStatus(), is(MigrationStatus.IN_PROGRESS));
        assertThat(migEntryWithRuns.getStatusMessage(), is("in progress"));
    }

    @Test
    public void shouldUpdateReRunStatusOnSuccess() throws Exception {
        // given
        final MigrationEntry migEntry = addMigration(new MigrationInfo("1", "2"));
        final MigrationRun migRun = new MigrationRun().update(MigrationStatus.IN_PROGRESS, "in progress");
        final MigrationEntry migEntryWithRuns = migrationHistoryService.addRunToMigrationEntry(migEntry, migRun);

        // when
        var migEntryFinished = migrationHistoryService.setLastReRunToFinished(migEntryWithRuns);

        assertThat(migEntryFinished.getStatus(), is(MigrationStatus.OK));
        assertThat(migEntryFinished.getStatusMessage(), is("Migration re-run completed successfully"));
        assertThat(migEntryFinished.getReruns(), hasSize(1));
        assertThat(migEntryFinished.getReruns().get(0).getStatus(), is(MigrationStatus.OK));
        assertThat(migEntryFinished.getReruns().get(0).getStatusMessage(), is("Migration re-run completed successfully"));
    }

    @Test
    public void shouldUpdateReRunStatusOnError() throws Exception {
        // given
        final MigrationEntry migEntry = addMigration(new MigrationInfo("1", "2"));
        final MigrationRun migRun = new MigrationRun().update(MigrationStatus.IN_PROGRESS, "in progress");
        final MigrationEntry migEntryWithRuns = migrationHistoryService.addRunToMigrationEntry(migEntry, migRun);

        // when
        var migEntryFinished = migrationHistoryService.setLastReRunToFailed(migEntryWithRuns, new RuntimeException("error"));

        assertThat(migEntryFinished.getStatus(), is(MigrationStatus.ERROR));
        assertThat(migEntryFinished.getStatusMessage(), is("Migration re-run failed with: error"));
        assertThat(migEntryFinished.getReruns(), hasSize(1));
        assertThat(migEntryFinished.getReruns().get(0).getStatus(), is(MigrationStatus.ERROR));
        assertThat(migEntryFinished.getReruns().get(0).getStatusMessage(), is("Migration re-run failed with: error"));
    }

    private void assertMigrationRun(MigrationRun migrationRun, MigrationRun migRun) {
        assertThat(migrationRun.getStatus(), is(migRun.getStatus()));
        assertThat(migrationRun.getStatusMessage(), is(migRun.getStatusMessage()));
        assertThat(migrationRun.getCreatedAt(), is(migRun.getCreatedAt()));
        assertThat(migrationRun.getUpdatedAt(), is(migRun.getUpdatedAt()));

    }

    @Test
    public void shouldAddMultipleRerunsToMigEntry() throws Exception {
        // given
        final MigrationEntry migEntry = addMigration(new MigrationInfo("1", "2"));
        final MigrationRun migRun0 = new MigrationRun().update(MigrationStatus.OK, "it's ok");
        final MigrationRun migRun1 = new MigrationRun().update(MigrationStatus.ERROR, "it's not ok");
        final MigrationRun migRun2 = new MigrationRun().update(MigrationStatus.OK, "it's ok again");

        // when
        migrationHistoryService.addRunToMigrationEntry(migEntry, migRun0);
        migrationHistoryService.addRunToMigrationEntry(migEntry, migRun1);
        migrationHistoryService.addRunToMigrationEntry(migEntry, migRun2);

        final MigrationEntry migEntyUpdated = migrationHistoryService.findMigration("1", "2");
        assertThat(migEntyUpdated.getReruns(), hasSize(3));
        assertMigrationRun(migEntyUpdated.getReruns().get(0), migRun0);
        assertMigrationRun(migEntyUpdated.getReruns().get(1), migRun1);
        assertMigrationRun(migEntyUpdated.getReruns().get(2), migRun2);
    }

    private MigrationEntry addMigration(final MigrationInfo migrationInfo) throws InterruptedException {
        Thread.sleep(1);
        final MigrationEntry migInProgress = migrationHistoryService.insertMigrationStatusInProgress(migrationInfo);
        migrationHistoryService.setMigrationStatusToFinished(migInProgress);
        return migInProgress;
    }

    private <T extends Object> List<T> toList(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
    }

}
