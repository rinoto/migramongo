package com.rinoto.migramongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.dao.MigrationHistoryService;
import com.rinoto.migramongo.lookup.ScriptLookupService;

@RunWith(MockitoJUnitRunner.class)
public class MigraMongoTest {

	@InjectMocks
	MigraMongo migraMongo;
	@Mock
	MigrationHistoryService migEntryService;
	@Mock
	ScriptLookupService lookupService;
	@Mock
	MongoDatabase mongoDatabase;

	@Before
	public void setup() {
		// mocking
		when(migEntryService.insertMigrationStatusInProgress(any(MigrationInfo.class))).thenAnswer(i -> {
			final MigrationInfo migInfo = (MigrationInfo) i.getArguments()[0];
			final MigrationEntry e = createMigrationEntry(migInfo.getFromVersion(), migInfo.getToVersion(), null);
			return e;
		});
		when(migEntryService.setMigrationStatusToFinished(any(MigrationEntry.class))).thenAnswer(i -> {
			final MigrationEntry entry = (MigrationEntry) i.getArguments()[0];
			entry.setStatus(MigrationStatus.OK);
			return entry;
		});
		when(migEntryService.setMigrationStatusToFailed(any(MigrationEntry.class), any(Exception.class))).thenAnswer(
				i -> {
					final MigrationEntry entry = (MigrationEntry) i.getArguments()[0];
					entry.setStatus(MigrationStatus.ERROR);
					entry.setStatusMessage(((Exception) i.getArguments()[1]).getMessage());
					return entry;
				});
	}

	@Test
	public void shouldFailIfNoInitialScriptIsProvided() {
		// given
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.ERROR));
		assertThat(status.message, containsString("no last migration script found"));
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
	public void shouldReturnStatusInDryRunWhenExceptionThrown() throws Exception {
		// given - if no initial exists, exception will be thrown
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
	public void shouldMigrateInitialScriptAndThreeNormalOnesWhenNoMigrationExists() throws Exception {
		// given
		final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
		when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("8", "9"));
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
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("A", "B"), mockMongoScript("B", "C"),
				mockMongoScript("8", "9"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(4));
		verify(mockInitialScript).migrate(mongoDatabase);
		migrationScripts.stream().filter(ms -> ms.getMigrationInfo().getFromVersion().matches("[0-9]*"))
				.forEach(ms -> {
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
		// -- initial script provided
		final InitialMongoMigrationScript mockInitialScript = mockInitialScript("1");
		when(lookupService.findInitialScript()).thenReturn(mockInitialScript);
		// when
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(0));
		verifyZeroInteractions(mockInitialScript);
	}

	private MigrationEntry mockLastEntry(String from, String to) {
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
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("8", "9"));
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
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8", new RuntimeException("script failing")), mockMongoScript("8", "9"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.ERROR));
		assertThat(status.migrationsApplied, hasSize(2));
		assertThat(
				status.message,
				allOf(containsString("fromVersion '2'"), containsString("toVersion '8'"),
						containsString("script failing")));
		verify(migrationScripts.get(0)).migrate(mongoDatabase);
		verify(migrationScripts.get(1)).migrate(mongoDatabase);
		verify(migrationScripts.get(2), never()).migrate(mongoDatabase);
	}

	@Test
	public void shouldNotStartMigrationIfLastScriptIsInError() throws Exception {
		// given
		// -- last entry in db
		final MigrationEntry mockLastEntry = mockLastEntry("1", "1");
		mockLastEntry.setStatus(MigrationStatus.ERROR);
		// - mig scripts provided
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("8", "9"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.ERROR));
		assertThat(status.migrationsApplied, hasSize(0));
		assertThat(
				status.message,
				allOf(containsString("Last Migration is in status ERROR"), containsString("fromVersion=1"),
						containsString("toVersion=1")));
		for (MongoMigrationScript ms : migrationScripts) {
			verify(ms, never()).migrate(mongoDatabase);
		}
	}

	@Test
	public void shouldMigrateIfACoupleOfMigrationsAreAlreadyOnDB() throws Exception {
		// given
		// -- last entry in db
		mockLastEntry("4", "5");
		// - mig scripts provided
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("5", "7"), mockMongoScript("7", "8"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(2));
		migrationScripts.stream().filter(ms -> Integer.valueOf(ms.getMigrationInfo().getFromVersion()) >= 5)
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
	}

	@Test
	public void shouldRepairEntryInErrorStatus() throws Exception {
		// given
		mockEntry("4", "5", MigrationStatus.ERROR);
		// when
		final MigraMongoStatus status = migraMongo.repair("4", "5");
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.message, containsString("changed from '" + MigrationStatus.ERROR + "' to '"
				+ MigrationStatus.OK + "'"));
		assertThat(status.migrationsApplied, hasSize(1));
	}

	@Test
	public void shouldRepairEntryInInProgressStatus() throws Exception {
		// given
		mockEntry("4", "5", MigrationStatus.IN_PROGRESS);
		// when
		final MigraMongoStatus status = migraMongo.repair("4", "5");
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.message, containsString("changed from '" + MigrationStatus.IN_PROGRESS + "' to '"
				+ MigrationStatus.OK + "'"));
		assertThat(status.migrationsApplied, hasSize(1));
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
	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailIfThereAreMoreThanOneMigrationScriptsToApply() throws Exception {
		// given
		mockLastEntry("1", "1");
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("2", "9"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);

		// when - expects IllegalStateException
		migraMongo.migrate();
	}

	@Test
	public void shouldReturnAppliedEntries() throws Exception {
		// given
		when(migEntryService.getAllMigrationsApplied()).thenReturn(
				Arrays.asList(createMigrationEntry("1.0", "2.0", MigrationStatus.OK),
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
	public void shouldDeliverOkStatusWhenEverythingOk() throws Exception {
		// given
		when(migEntryService.findMigrations("1")).thenReturn(
				Arrays.asList(createMigrationEntry("1", "2", MigrationStatus.OK)));
		// when
		final MigraMongoStatus status = migraMongo.status("1");
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(1));
	}

	@Test
	public void shouldDeliverInProgressStatusWhenAtLeastOneEntryIsInProgress() throws Exception {
		// given
		when(migEntryService.findMigrations("1")).thenReturn(
				Arrays.asList(createMigrationEntry("1", "2", MigrationStatus.OK),
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
		when(migEntryService.findMigrations("1")).thenReturn(
				Arrays.asList(createMigrationEntry("1", "2", MigrationStatus.OK),
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

	private void mockEntry(String fromVersion, String toVersion, MigrationStatus status) {
		final MigrationEntry migEntry = createMigrationEntry(fromVersion, toVersion, status);
		when(migEntryService.findMigration(fromVersion, toVersion)).thenReturn(migEntry);
	}

	private MigrationEntry createMigrationEntry(String fromVersion, String toVersion, MigrationStatus status) {
		final MigrationEntry migEntry = new MigrationEntry();
		migEntry.setFromVersion(fromVersion);
		migEntry.setToVersion(toVersion);
		migEntry.setStatus(status);
		return migEntry;
	}

	private MongoMigrationScript mockMongoScript(String from, String to) {
		final MongoMigrationScript script = mock(MongoMigrationScript.class);
		when(script.getMigrationInfo()).thenReturn(new MigrationInfo(from, to));
		return script;
	}

	private MongoMigrationScript mockMongoScript(String from, String to, Throwable throwable) throws Exception {
		final MongoMigrationScript script = mockMongoScript(from, to);
		Mockito.doThrow(throwable).when(script).migrate(mongoDatabase);
		return script;
	}

	private InitialMongoMigrationScript mockInitialScript(String version) {
		final InitialMongoMigrationScript script = mock(InitialMongoMigrationScript.class);
		when(script.getMigrationInfo()).thenReturn(new InitialMigrationInfo(version));
		return script;
	}

	private MigrationEntry mockLastMigrationApplied(String from, String to) {
		final MigrationEntry migEntry = createMigrationEntry(from, to, MigrationStatus.OK);
		when(migEntryService.getLastMigrationApplied()).thenReturn(migEntry);
		return migEntry;
	}

}
