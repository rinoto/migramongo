package com.rinoto.migramongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.dao.MigrationHistoryService;

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
			final MigrationEntry e = new MigrationEntry();
			e.fromVersion = migInfo.getFromVersion();
			e.toVersion = migInfo.getToVersion();
			return e;
		});
		when(migEntryService.setMigrationStatusToFinished(any(MigrationEntry.class))).thenAnswer(i -> {
			return (MigrationEntry) i.getArguments()[0];
		});
		when(migEntryService.setMigrationStatusToFailed(any(MigrationEntry.class), any(Exception.class))).thenAnswer(
				i -> {
					return (MigrationEntry) i.getArguments()[0];
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
	public void shouldMigrateInitialScriptWhenNoMigrationExists() {
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
	public void shouldMigrateInitialScriptAndOneNormalOneWhenNoMigrationExists() {
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
		migrationScripts.forEach(ms -> verify(ms).migrate(mongoDatabase));

	}

	@Test
	public void shouldMigrateInitialScriptAndThreeNormalOnesWhenNoMigrationExists() {
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
		migrationScripts.forEach(ms -> verify(ms).migrate(mongoDatabase));
	}

	@Test
	public void shouldMigrateInitialScriptAndThreeNormalOnesWhenNoMigrationExistsAndSomeScriptsDoNotCorrespondWithExistingVersions() {
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
				.forEach(ms -> verify(ms).migrate(mongoDatabase));
	}

	@Test
	public void shouldNotMigrateIfFirstMigrationAlreadyExistsOnDB() throws Exception {
		// given
		// -- last entry in db
		final MigrationEntry lastEntry = new MigrationEntry();
		lastEntry.fromVersion = "1";
		lastEntry.toVersion = "1";
		when(migEntryService.getLastMigrationApplied()).thenReturn(lastEntry);
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

	@Test
	public void shouldMigrateIfInitialIsAlreadyOnDB() throws Exception {
		// given
		// -- last entry in db
		final MigrationEntry lastEntry = new MigrationEntry();
		lastEntry.fromVersion = "1";
		lastEntry.toVersion = "1";
		// - mig scripts provided
		when(migEntryService.getLastMigrationApplied()).thenReturn(lastEntry);
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("8", "9"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(migrationScripts.size()));
		migrationScripts.forEach(ms -> verify(ms).migrate(mongoDatabase));

	}

	@Test
	public void shouldMigrateIfACoupleOfMigrationsAreAlreadyOnDB() throws Exception {
		// given
		// -- last entry in db
		final MigrationEntry lastEntry = new MigrationEntry();
		lastEntry.fromVersion = "4";
		lastEntry.toVersion = "5";
		// - mig scripts provided
		when(migEntryService.getLastMigrationApplied()).thenReturn(lastEntry);
		final List<MongoMigrationScript> migrationScripts = Arrays.asList(mockMongoScript("1", "2"),
				mockMongoScript("2", "8"), mockMongoScript("5", "7"), mockMongoScript("7", "8"));
		when(lookupService.findMongoScripts()).thenReturn(migrationScripts);
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(2));
		migrationScripts.stream().filter(ms -> Integer.valueOf(ms.getMigrationInfo().getFromVersion()) >= 5)
				.forEach(ms -> verify(ms).migrate(mongoDatabase));

	}

	@Test
	public void shouldNotMigrateIfNoNewMigrationScriptsAvailable() throws Exception {
		// given
		// -- last entry in db
		final MigrationEntry lastEntry = new MigrationEntry();
		lastEntry.fromVersion = "4";
		lastEntry.toVersion = "5";
		// - mig scripts provided
		when(migEntryService.getLastMigrationApplied()).thenReturn(lastEntry);
		when(lookupService.findMongoScripts()).thenReturn(Collections.emptyList());
		// when
		final MigraMongoStatus status = migraMongo.migrate();
		// then
		assertThat(status.status, is(MigrationStatus.OK));
		assertThat(status.migrationsApplied, hasSize(0));

	}

	private MongoMigrationScript mockMongoScript(String from, String to) {
		final MongoMigrationScript script = mock(MongoMigrationScript.class);
		when(script.getMigrationInfo()).thenReturn(new MigrationInfo(from, to));
		return script;
	}

	private InitialMongoMigrationScript mockInitialScript(String version) {
		final InitialMongoMigrationScript script = mock(InitialMongoMigrationScript.class);
		when(script.getMigrationInfo()).thenReturn(new InitialMigrationInfo(version));
		return script;
	}

}
