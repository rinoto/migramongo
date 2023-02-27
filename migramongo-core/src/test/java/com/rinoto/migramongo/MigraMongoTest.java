package com.rinoto.migramongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.dao.LockService;
import com.rinoto.migramongo.dao.MigrationHistoryService;
import com.rinoto.migramongo.lookup.ScriptLookupService;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MigraMongoTest {

    @InjectMocks
    MigraMongo migraMongo;
    @Mock
    MigrationHistoryService migEntryService;
    @Mock
    ScriptLookupService lookupService;
    @Mock
    LockService lockService;
    @Mock
    MongoDatabase mongoDatabase;

    @BeforeEach
    public void setup() {
        // mocking
        lenient().when(migEntryService.insertMigrationStatusInProgress(any(MigrationInfo.class))).thenAnswer(i -> {
            final MigrationInfo migInfo = (MigrationInfo) i.getArguments()[0];
            final MigrationEntry e = createMigrationEntry(migInfo.getFromVersion(), migInfo.getToVersion(), null);
            return e;
        });
        lenient().when(migEntryService.insertMigrationStatusSkipped(any(MigrationInfo.class))).thenAnswer(i -> {
            final MigrationInfo migInfo = (MigrationInfo) i.getArguments()[0];
            final MigrationEntry e = createMigrationEntry(migInfo.getFromVersion(), migInfo.getToVersion(), null);
            return e;
        });
        lenient().when(migEntryService.setMigrationStatusToFinished(any(MigrationEntry.class))).thenAnswer(i -> {
            final MigrationEntry entry = (MigrationEntry) i.getArguments()[0];
            entry.setStatus(MigrationStatus.OK);
            return entry;
        });
        lenient().when(migEntryService.setMigrationStatusToFailed(any(MigrationEntry.class), any(Exception.class)))
                .thenAnswer(i -> {
                    final MigrationEntry entry = (MigrationEntry) i.getArguments()[0];
                    entry.setStatus(MigrationStatus.ERROR);
                    entry.setStatusMessage(((Exception) i.getArguments()[1]).getMessage());
                    return entry;
                });
        lenient().when(lockService.acquireLock()).thenReturn(true);
    }

    @Test
    public void shouldNotMigrateAnythingIfNoInitialScriptIsProvided() {
        // given
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldMigrateInitialScriptWhenNoMigrationExists() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(1));
        verify(mockInitialScript).migrate(mongoDatabase);
    }

    @Test
    public void shouldPerformDryRunOnInitialScriptWhenNoMigrationExists() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        // when
        final MigraMongoStatus status = migraMongo.dryRun();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(1));
        assertThat(status.migrationsApplied.get(0).getFromVersion(), is("1"));
        assertThat(status.migrationsApplied.get(0).getToVersion(), is("1"));
        // IMPORTANT - no interactions in the script - migration has not been
        // performed!
        verify(mockInitialScript, times(0)).migrate(mongoDatabase);
    }

    @Test
    public void shouldReturnStatusInDryRunWhenNoInitialScriptFound() throws Exception {
        // given - if no initial exists, nothing will be migrated
        // when
        final MigraMongoStatus status = migraMongo.dryRun();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldReturnErrorStatusInDryRunWhenExceptionIsThrown() throws Exception {
        //given
        mockLastMigrationApplied("1", "2", MigrationStatus.ERROR);
        // when
        final MigraMongoStatus status = migraMongo.dryRun();
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldMigrateInitialScriptAndOneNormalOneWhenNoMigrationExists() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(2));
        verify(mockInitialScript).migrate(mongoDatabase);
        for (MongoMigrationScript ms : migrationScripts) {
            verify(ms).migrate(mongoDatabase);
        }

    }

    @Test
    public void shouldApplyAllMigrationWithIncludedPropertySetToFalseByDefault() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        migraMongo.setIncludedInInitialMigration(false);
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        final MongoMigrationScript migrationScript = mockMongoScript("1", "2");
        when(lookupService.findMongoScripts()).thenReturn(List.of(migrationScript));
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(2));
        assertThat(status.migrationsApplied.get(0).isSkipped(), is(false));
        assertThat(status.migrationsApplied.get(1).isSkipped(), is(false));
        verify(mockInitialScript).migrate(mongoDatabase);
        verify(migrationScript).migrate(mongoDatabase);
    }

    @Test
    public void shouldMigrateInitialScriptAndThreeNormalOnesWhenNoMigrationExists() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(mockMongoScript("1", "2"), mockMongoScript("2", "8"), mockMongoScript("8", "9"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(migrationScripts.size() + 1));
        verify(mockInitialScript).migrate(mongoDatabase);
        for (MongoMigrationScript ms : migrationScripts) {
            verify(ms).migrate(mongoDatabase);
        }
    }

    @Test
    public void shouldMigrateInitialScriptAndThreeNormalOnesWhenNoMigrationExistsAndSomeScriptsDoNotCorrespondWithExistingVersions()
            throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(
                        mockMongoScript("1", "2"),
                        mockMongoScript("2", "8"),
                        mockMongoScript("A", "B"),
                        mockMongoScript("B", "C"),
                        mockMongoScript("8", "9"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(4));
        verify(mockInitialScript).migrate(mongoDatabase);
        migrationScripts.stream().filter(ms -> ms.getMigrationInfo().getFromVersion().matches("[0-9]*")).forEach(ms -> {
            try {
                verify(ms).migrate(mongoDatabase);
            } catch (Exception e) {
                throw new RuntimeException("Exception while migrating", e);
            }
        });
    }

    @Test
    public void shouldNotMigrateIfFirstMigrationAlreadyExistsOnDB() throws Exception {
        // given
        // -- last entry in db
        mockLastEntry("1", "1");
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then - no need to lookup for the initial script
        verify(lookupService, times(0)).findInitialScript();
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    private MigrationRun mockLastEntry(String from, String to) {
        final MigrationEntry lastEntry = createMigrationEntry(from, to, MigrationStatus.OK);
        when(migEntryService.getLastMigrationApplied()).thenReturn(lastEntry);
        return lastEntry;
    }

    @Test
    public void shouldMigrateIfInitialIsAlreadyOnDB() throws Exception {
        // given
        // -- last entry in db
        mockLastEntry("1", "1");
        // - mig scripts provided
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(mockMongoScript("1", "2"), mockMongoScript("2", "8"), mockMongoScript("8", "9"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(migrationScripts.size()));
        for (MongoMigrationScript ms : migrationScripts) {
            verify(ms).migrate(mongoDatabase);
        }
    }

    @Test
    public void shouldStopMigrationIfAnyScriptFails() throws Exception {
        // given
        // -- last entry in db
        mockLastEntry("1", "1");
        // - mig scripts provided
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(
                        mockMongoScript("1", "2"),
                        mockMongoScript("2", "8", new RuntimeException("script failing")),
                        mockMongoScript("8", "9"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.migrationsApplied, hasSize(2));
        assertThat(
                status.message,
                allOf(
                        containsString("fromVersion '2'"),
                        containsString("toVersion '8'"),
                        containsString("script failing")));
        verify(migrationScripts.get(0)).migrate(mongoDatabase);
        verify(migrationScripts.get(1)).migrate(mongoDatabase);
        verify(migrationScripts.get(2), never()).migrate(mongoDatabase);
    }

    @Test
    public void shouldNotStartMigrationIfLastScriptIsInError() throws Exception {
        // given
        // -- last entry in db
        final MigrationRun mockLastEntry = mockLastEntry("1", "1");
        mockLastEntry.setStatus(MigrationStatus.ERROR);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.migrationsApplied, hasSize(0));
        assertThat(
                status.message,
                allOf(
                        containsString("Last Migration is in status ERROR"),
                        containsString("fromVersion=1"),
                        containsString("toVersion=1")));
        verify(lookupService, times(0)).findMongoScripts();
    }

    @Test
    public void shouldMigrateIfACoupleOfMigrationsAreAlreadyOnDB() throws Exception {
        // given
        // -- last entry in db
        mockLastEntry("4", "5");
        // - mig scripts provided
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(
                        mockMongoScript("1", "2"),
                        mockMongoScript("2", "8"),
                        mockMongoScript("5", "7"),
                        mockMongoScript("7", "8"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(2));
        migrationScripts
                .stream()
                .filter(ms -> Integer.valueOf(ms.getMigrationInfo().getFromVersion()) >= 5)
                .forEach(ms -> {
                    try {
                        verify(ms).migrate(mongoDatabase);
                    } catch (Exception e) {
                        throw new RuntimeException("Exception while migrating", e);
                    }
                });

    }

    @Test
    public void shouldNotMigrateIfNoNewMigrationScriptsAvailable() throws Exception {
        // given
        // -- last entry in db
        mockLastEntry("4", "5");
        // - mig scripts provided
        when(lookupService.findMongoScripts()).thenReturn(Collections.emptyList());
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldNotRepairEntryThatDoesNotExist() throws Exception {
        // when
        final MigraMongoStatus status = migraMongo.repair("2", "3");
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.message, containsString("No migration entry found"));
        assertThat(status.migrationsApplied, hasSize(0));
        verify(migEntryService, never()).setMigrationStatusToManuallyRepaired(any());
    }

    @Test
    public void shouldRepairEntryInErrorStatus() throws Exception {
        // given
        MigrationEntry migrationEntry = mockEntry("4", "5", MigrationStatus.ERROR);
        // when
        final MigraMongoStatus status = migraMongo.repair("4", "5");
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(
                status.message,
                containsString("changed from '" + MigrationStatus.ERROR + "' to '" + MigrationStatus.OK + "'"));
        assertThat(status.migrationsApplied, hasSize(1));
        verify(migEntryService, times(1)).setMigrationStatusToManuallyRepaired(migrationEntry);
    }

    @Test
    public void shouldRepairEntryInInProgressStatus() throws Exception {
        // given
        MigrationEntry migrationEntry = mockEntry("4", "5", MigrationStatus.IN_PROGRESS);
        // when
        final MigraMongoStatus status = migraMongo.repair("4", "5");
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(
                status.message,
                containsString("changed from '" + MigrationStatus.IN_PROGRESS + "' to '" + MigrationStatus.OK + "'"));
        assertThat(status.migrationsApplied, hasSize(1));
        verify(migEntryService, times(1)).setMigrationStatusToManuallyRepaired(migrationEntry);
    }

    @Test
    public void shouldRequireAndReleaseLockDuringRepair() throws Exception {
        // given
        MigrationEntry migrationEntry = mockEntry("4", "5", MigrationStatus.IN_PROGRESS);
        when(lockService.acquireLock()).thenReturn(true);
        // when
        final MigraMongoStatus status = migraMongo.repair("4", "5");
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        verify(lockService).acquireLock();
        verify(lockService).releaseLock();
    }

    @Test
    public void shouldFailToRepairIfLockCanNotBeAcquired() throws Exception {
        // given
        when(lockService.acquireLock()).thenReturn(false);
        // when
        final MigraMongoStatus status = migraMongo.repair("4", "5");
        // then
        assertThat(status.status, is(MigrationStatus.LOCK_NOT_ACQUIRED));
    }

    @Test
    public void shouldNotRepairEntryInOkStatus() throws Exception {
        // given
        mockEntry("4", "5", MigrationStatus.OK);
        // when
        final MigraMongoStatus status = migraMongo.repair("4", "5");
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.message, containsString("has already status '" + MigrationStatus.OK + "'"));
        assertThat(status.migrationsApplied, hasSize(0));
        verify(migEntryService, never()).setMigrationStatusToManuallyRepaired(any());
    }

    @Test
    public void shouldFailIfThereAreMoreThanOneMigrationScriptsToApply() throws Exception {
        // given
        mockLastEntry("1", "1");
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(mockMongoScript("1", "2"), mockMongoScript("2", "8"), mockMongoScript("2", "9"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);

        // when
        MigraMongoStatus status = migraMongo.migrate();

        //then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.message, Matchers.startsWith("There is more than one script with fromVersion 2"));
    }

    @Test
    public void shouldReturnAppliedEntries() throws Exception {
        // given
        when(migEntryService.getAllMigrationsApplied())
                .thenReturn(
                        Arrays
                                .asList(
                                        createMigrationEntry("1.0", "2.0", MigrationStatus.OK),
                                        createMigrationEntry("2.0", "3.0", MigrationStatus.OK)));
        // when
        final List<MigrationEntry> migrationEntries = migraMongo.getMigrationEntries();
        // then
        assertThat(migrationEntries, hasSize(2));
    }

    @Test
    public void shouldMigrateAsynchWhenThereIsSomethingToMigrate() throws Exception {
        // given
        final CountDownLatch latch = new CountDownLatch(1);
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(mockInitialScript).migrate(Mockito.any(MongoDatabase.class));
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        // when
        final MigraMongoStatus status = migraMongo.migrateAsync();
        // then
        latch.await(2, TimeUnit.SECONDS);
        assertThat(status.status, is(MigrationStatus.IN_PROGRESS));
        assertThat(status.migrationsApplied, hasSize(1));
        verify(mockInitialScript).migrate(mongoDatabase);
    }

    @Test
    public void shouldExecuteAsynchInitializerFunction() throws Exception {
        // given
        final CountDownLatch asyncFunctionLatch = new CountDownLatch(1);
        migraMongo.setAsyncInitializerFunction(() -> asyncFunctionLatch.countDown());

        final CountDownLatch latch = new CountDownLatch(1);
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(mockInitialScript).migrate(Mockito.any(MongoDatabase.class));
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        // when
        migraMongo.migrateAsync();

        // then
        latch.await(2, TimeUnit.SECONDS);
        assertThat(asyncFunctionLatch.getCount(), is(0L));
    }

    @Test
    public void shouldExecuteAsynchDestroyerFunction() throws Exception {
        // given
        final CountDownLatch asyncFunctionLatch = new CountDownLatch(1);
        migraMongo.setAsyncDestroyerFunction(() -> asyncFunctionLatch.countDown());

        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        doAnswer(i -> null).when(mockInitialScript).migrate(Mockito.any(MongoDatabase.class));
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        // when
        migraMongo.migrateAsync();

        // then
        asyncFunctionLatch.await(2, TimeUnit.SECONDS);
        assertThat(asyncFunctionLatch.getCount(), is(0L));
    }

    @Test
    public void shouldDeliverOkStatusWhenEverythingOk() throws Exception {
        // given
        when(migEntryService.findMigrations("1"))
                .thenReturn(Arrays.asList(createMigrationEntry("1", "2", MigrationStatus.OK)));
        // when
        final MigraMongoStatus status = migraMongo.status("1");
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(1));
    }

    @Test
    public void shouldDeliverInProgressStatusWhenAtLeastOneEntryIsInProgress() throws Exception {
        // given
        when(migEntryService.findMigrations("1"))
                .thenReturn(
                        Arrays
                                .asList(
                                        createMigrationEntry("1", "2", MigrationStatus.OK),
                                        createMigrationEntry("1", "2", MigrationStatus.IN_PROGRESS),
                                        createMigrationEntry("1", "2", MigrationStatus.OK)));
        // when
        final MigraMongoStatus status = migraMongo.status("1");
        // then
        assertThat(status.status, is(MigrationStatus.IN_PROGRESS));
        assertThat(status.migrationsApplied, hasSize(3));
    }

    @Test
    public void shouldDeliverErrorStatusWhenAtLeastOneEntryIsInProgress() throws Exception {
        // given
        when(migEntryService.findMigrations("1"))
                .thenReturn(
                        Arrays
                                .asList(
                                        createMigrationEntry("1", "2", MigrationStatus.OK),
                                        createMigrationEntry("1", "2", MigrationStatus.ERROR),
                                        createMigrationEntry("1", "2", MigrationStatus.OK)));
        // when
        final MigraMongoStatus status = migraMongo.status("1");
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.migrationsApplied, hasSize(3));
    }

    @Test
    public void shouldNotMigrateAsynchWhenThereIsNothingToMigrate() throws Exception {
        // given
        mockLastMigrationApplied("1", "2");
        // when
        final MigraMongoStatus status = migraMongo.migrateAsync();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldNotMigrateIfItCannotAcquireLock() {
        // given
        when(lockService.acquireLock()).thenReturn(false);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.LOCK_NOT_ACQUIRED));
    }

    @Test
    public void shouldReleaseLockWhenMigratingSync() {
        // when
        migraMongo.migrate();
        // then
        verify(lockService).releaseLock();
    }

    @Test
    public void shouldDestroyLock() {
        // when
        migraMongo.destroyLocks();
        // then
        verify(lockService).destroyLock();
    }

    @Test
    public void shouldNotExecuteMigrationScriptMarkedWithIncludedInInitialTrue() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(mockMongoScript("1", "2", true), mockMongoScript("2", "3", false), mockMongoScript("3", "4", true));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);

        // when
        final MigraMongoStatus status = migraMongo.migrate();

        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(migrationScripts.size() + 1));
        verify(mockInitialScript).migrate(mongoDatabase);
        final Map<Boolean, List<MongoMigrationScript>> scriptsGrouppedByIncludedInInitial = migrationScripts
                .stream()
                .collect(Collectors.groupingBy(ms -> ms.includedInInitialMigrationScript()));
        //included in initial = true
        for (MongoMigrationScript scriptIncludedInInitial : scriptsGrouppedByIncludedInInitial.get(true)) {
            verify(scriptIncludedInInitial, times(0)).migrate(mongoDatabase);
        }
        //included in initial = false
        for (MongoMigrationScript scriptNotIncludedInInitial : scriptsGrouppedByIncludedInInitial.get(false)) {
            verify(scriptNotIncludedInInitial).migrate(mongoDatabase);
        }
    }

    @Test
    public void shouldExecuteMigrationScriptMarkedWithIncludedInInitialTrueIfItsNotAnInitialMigration()
            throws Exception {
        // given
        mockLastMigrationApplied("0", "1");
        final List<MongoMigrationScript> migrationScripts = Arrays
                .asList(mockMongoScript("1", "2"), mockMongoScript("2", "3"), mockMongoScript("3", "4"));
        when(lookupService.findMongoScripts()).thenReturn(migrationScripts);

        // when
        final MigraMongoStatus status = migraMongo.migrate();

        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(migrationScripts.size()));
        //all are migrated, independent on the isIncludedInInitial
        for (MongoMigrationScript migrationScript : migrationScripts) {
            verify(migrationScript).migrate(mongoDatabase);
        }
    }

    @Test
    public void aMongoMigrationScriptShouldBeInIncludedInInitialMigrationByDefault() throws Exception {
        //given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        final MongoMigrationScript migrationScript = new MongoMigrationScript() {
            private final MigrationInfo migrationInfo = new MigrationInfo("1", "2");

            @Override
            public MigrationInfo getMigrationInfo() {
                return migrationInfo;
            }

            @Override
            public void migrate(MongoDatabase database) {
                //not needed
            }
        };
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        when(lookupService.findMongoScripts()).thenReturn(List.of(migrationScript));
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertNull(migrationScript.includedInInitialMigrationScript());
        assertThat(status.status, is(MigrationStatus.OK));
        verify(migEntryService).insertMigrationStatusSkipped(migrationScript.getMigrationInfo());
        assertThat(status.migrationsApplied, hasSize(2));
        verify(mockInitialScript).migrate(mongoDatabase);
    }

    @Test
    public void shouldMigrateInitialScriptEvenIfIncludedInInitialIsFalse() throws Exception {
        // given
        final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
        when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
        // when
        final MigraMongoStatus status = migraMongo.migrate();
        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(status.migrationsApplied, hasSize(1));
        verify(mockInitialScript).migrate(mongoDatabase);
    }

    @Test
    public void shouldNotRerunIfEntryDoesNotExist() throws Exception {
        // when
        final MigraMongoStatus status = migraMongo.rerun("NOTEXISTING1", "NOTEXISTING2");
        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.message, containsString("No migration entry found"));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldNotRerunIfScriptDoesNotExist() throws Exception {
        // given
        mockEntry("2", "3", MigrationStatus.OK);
        final List<MongoMigrationScript> mongoScripts = Arrays.asList(mockMongoScript("1", "2", (Boolean) null));
        when(lookupService.findMongoScripts()).thenReturn(mongoScripts);

        // when
        final MigraMongoStatus status = migraMongo.rerun("2", "3");

        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(status.message, containsString("No migration script found"));
        assertThat(status.migrationsApplied, hasSize(0));
    }

    @Test
    public void shouldRerunIfEntryAndScriptExist() throws Exception {
        // given
        final MigrationEntry entry = mockEntry("1", "2", MigrationStatus.OK);
        final MongoMigrationScript mongoScript = mockMongoScript("1", "2", (Boolean) null);
        final List<MongoMigrationScript> mongoScripts = Arrays.asList(mongoScript);
        when(lookupService.findMongoScripts()).thenReturn(mongoScripts);
        when(migEntryService.addRunToMigrationEntry(eq(entry), any(MigrationRun.class))).thenAnswer(i -> {
            final MigrationRun migRun = (MigrationRun) i.getArguments()[1];
            entry.setReruns(Arrays.asList(migRun));
            return entry;
        });

        // when
        final MigraMongoStatus status = migraMongo.rerun("1", "2");

        // then
        assertThat(status.status, is(MigrationStatus.OK));
        assertThat(
                status.message,
                allOf(
                        containsString("Re-run of Migration"),
                        containsString("fromVersion 1"),
                        containsString("toVersion 2"),
                        containsString("successfully")));
        assertThat(status.migrationsApplied, hasSize(1));
        final MigrationEntry migrationApplied = status.migrationsApplied.get(0);
        assertThat(migrationApplied.getReruns(), hasSize(1));
        final MigrationRun migrationRun = migrationApplied.getReruns().get(0);
        assertThat(migrationRun.status, is(MigrationStatus.OK));
        assertThat(migrationRun.statusMessage, is("Migration completed correctly"));
        verify(mongoScript).migrate(mongoDatabase);
    }

    @Test
    public void shouldAcquireAndReleaseLockDuringMigrationRerun() throws Exception {
        // given
        final MigrationEntry entry = mockEntry("1", "2", MigrationStatus.OK);
        final MongoMigrationScript mongoScript = mockMongoScript("1", "2", (Boolean) null);
        final List<MongoMigrationScript> mongoScripts = Arrays.asList(mongoScript);
        when(lookupService.findMongoScripts()).thenReturn(mongoScripts);
        when(migEntryService.addRunToMigrationEntry(eq(entry), any(MigrationRun.class))).thenAnswer(i -> {
            final MigrationRun migRun = (MigrationRun) i.getArguments()[1];
            entry.setReruns(Arrays.asList(migRun));
            return entry;
        });

        doAnswer((answer) -> {
            verify(lockService).acquireLock();
            return null;
        }).when(mongoScript).migrate(any(MongoDatabase.class));

        migraMongo.rerun("1", "2");

        verify(lockService).releaseLock();
    }

    @Test
    public void shouldReleaseLockIfMigrationRerunFails() throws Exception {
        // given
        final MigrationEntry entry = mockEntry("1", "2", MigrationStatus.OK);
        final MongoMigrationScript mongoScript = mockMongoScript("1", "2", (Boolean) null);
        final List<MongoMigrationScript> mongoScripts = Arrays.asList(mongoScript);
        when(lookupService.findMongoScripts()).thenReturn(mongoScripts);
        when(migEntryService.addRunToMigrationEntry(eq(entry), any(MigrationRun.class))).thenAnswer(i -> {
            final MigrationRun migRun = (MigrationRun) i.getArguments()[1];
            entry.setReruns(Arrays.asList(migRun));
            return entry;
        });

        doThrow(new RuntimeException("")).when(mongoScript).migrate(any(MongoDatabase.class));

        migraMongo.rerun("1", "2");

        verify(lockService).releaseLock();
    }

    @Test
    public void shouldAddRerunEntryIfScriptFails() throws Exception {
        // given
        final MigrationEntry entry = mockEntry("1", "2", MigrationStatus.OK);
        final MongoMigrationScript mongoScript = mockMongoScript("1", "2", new RuntimeException("whatever exception"));
        final List<MongoMigrationScript> mongoScripts = Arrays.asList(mongoScript);
        when(lookupService.findMongoScripts()).thenReturn(mongoScripts);
        when(migEntryService.addRunToMigrationEntry(eq(entry), any(MigrationRun.class))).thenAnswer(i -> {
            final MigrationRun migRun = (MigrationRun) i.getArguments()[1];
            entry.setReruns(Arrays.asList(migRun));
            return entry;
        });

        // when
        final MigraMongoStatus status = migraMongo.rerun("1", "2");

        // then
        assertThat(status.status, is(MigrationStatus.ERROR));
        assertThat(
                status.message,
                allOf(
                        containsString("Error when re-running migration"),
                        containsString("fromVersion 1"),
                        containsString("toVersion 2")));
        assertThat(status.migrationsApplied, hasSize(1));
        final MigrationEntry migrationApplied = status.migrationsApplied.get(0);
        assertThat(migrationApplied.getReruns(), hasSize(1));
        final MigrationRun migrationRun = migrationApplied.getReruns().get(0);
        assertThat(migrationRun.status, is(MigrationStatus.ERROR));
        assertThat(migrationRun.statusMessage, is("whatever exception"));
        verify(mongoScript).migrate(mongoDatabase);
    }

    private MigrationEntry mockEntry(String fromVersion, String toVersion, MigrationStatus status) {
        final MigrationEntry migEntry = createMigrationEntry(fromVersion, toVersion, status);
        when(migEntryService.findMigration(fromVersion, toVersion)).thenReturn(migEntry);
        return migEntry;
    }

    public static MigrationEntry createMigrationEntry(String fromVersion, String toVersion, MigrationStatus status) {
        final MigrationEntry migEntry = new MigrationEntry();
        migEntry.setFromVersion(fromVersion);
        migEntry.setToVersion(toVersion);
        migEntry.setStatus(status);
        return migEntry;
    }

    private MongoMigrationScript mockMongoScript(String from, String to) {
        return mockMongoScript(from, to, false);
    }

    private MongoMigrationScript mockMongoScript(String from, String to, Boolean includedInInitial) {
        final MongoMigrationScript script = mock(MongoMigrationScript.class);
        when(script.getMigrationInfo()).thenReturn(new MigrationInfo(from, to));
        lenient().when(script.includedInInitialMigrationScript()).thenReturn(includedInInitial);
        return script;
    }

    private MongoMigrationScript mockMongoScript(String from, String to, Throwable throwable) throws Exception {
        final MongoMigrationScript script = mockMongoScript(from, to);
        Mockito.doThrow(throwable).when(script).migrate(mongoDatabase);
        return script;
    }

    private InitialMongoMigrationScript mockInitialScript(String version) {
        final InitialMongoMigrationScript script = mock(
                InitialMongoMigrationScript.class,
                withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        when(script.getMigrationInfo()).thenReturn(new InitialMigrationInfo(version));
        return script;
    }

    private MigrationRun mockLastMigrationApplied(String from, String to) {
        return mockLastMigrationApplied(from, to, MigrationStatus.OK);
    }

    private MigrationRun mockLastMigrationApplied(String from, String to, MigrationStatus status) {
        final MigrationEntry migEntry = createMigrationEntry(from, to, status);
        when(migEntryService.getLastMigrationApplied()).thenReturn(migEntry);
        return migEntry;
    }

}
