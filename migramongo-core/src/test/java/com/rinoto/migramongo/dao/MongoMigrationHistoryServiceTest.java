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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rinoto.migramongo.EmbeddedMongo;
import com.rinoto.migramongo.InitialMigrationInfo;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;

public class MongoMigrationHistoryServiceTest {

	public static EmbeddedMongo mongo = new EmbeddedMongo();

	MongoMigrationHistoryService migrationHistoryService;

	@BeforeClass
	public static void startMongo() throws Exception {
		mongo.start();
	}

	@AfterClass
	public static void stopMongo() {
		mongo.stop();
	}

	@Before
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
		final MigrationEntry migInProgress = migrationHistoryService.insertMigrationStatusInProgress(new MigrationInfo(
				"1", "2"));
		assertThat(
				migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("status", is(MigrationStatus.IN_PROGRESS)), hasProperty("fromVersion", is("1")),
						hasProperty("toVersion", is("2"))));

		// when
		migrationHistoryService.setMigrationStatusToFinished(migInProgress);

		// then
		assertThat(
				migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("status", is(MigrationStatus.OK)), hasProperty("fromVersion", is("1")),
						hasProperty("toVersion", is("2"))));
	}

	@Test
	public void shouldSetMigrationStatusToError() throws Exception {
		// given
		addMigration(new InitialMigrationInfo("1"));
		final MigrationEntry migInProgress = migrationHistoryService.insertMigrationStatusInProgress(new MigrationInfo(
				"1", "2"));
		assertThat(
				migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("status", is(MigrationStatus.IN_PROGRESS)), hasProperty("fromVersion", is("1")),
						hasProperty("toVersion", is("2"))));

		// when
		migrationHistoryService
				.setMigrationStatusToFailed(migInProgress, new RuntimeException("manually set to error"));

		// then
		assertThat(
				migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("status", is(MigrationStatus.ERROR)),
						hasProperty("statusMessage", is("manually set to error")), hasProperty("fromVersion", is("1")),
						hasProperty("toVersion", is("2"))));
	}

	@Test
	public void shouldSetMigrationStatusToRepaired() throws Exception {
		// given
		addMigration(new InitialMigrationInfo("1"));
		final MigrationEntry migInProgress = migrationHistoryService.insertMigrationStatusInProgress(new MigrationInfo(
				"1", "2"));
		assertThat(
				migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("status", is(MigrationStatus.IN_PROGRESS)), hasProperty("fromVersion", is("1")),
						hasProperty("toVersion", is("2"))));

		// when
		migrationHistoryService.setMigrationStatusToManuallyRepaired(migInProgress);

		// then
		assertThat(
				migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("status", is(MigrationStatus.OK)), hasProperty("repaired", is(true)),
						hasProperty("fromVersion", is("1")), hasProperty("toVersion", is("2"))));
	}

	@Test
	public void shouldAddInitialAndOneMigrationInfo() throws Exception {
		// given
		addMigration(new InitialMigrationInfo("1"));
		addMigration(new MigrationInfo("1", "2"));

		// when - then
		assertThat(migrationHistoryService.getLastMigrationApplied(),
				allOf(hasProperty("fromVersion", is("1")), hasProperty("toVersion", is("2"))));
	}

	@Test
	public void shouldFindMigrationEntryIfAvailable() throws Exception {
		// given
		addMigration(new InitialMigrationInfo("1"));
		addMigration(new MigrationInfo("1", "2"));

		// when - then
		assertThat(migrationHistoryService.findMigration("1", "2"),
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

	private void addMigration(final MigrationInfo migrationInfo) throws InterruptedException {
		Thread.sleep(1);
		final MigrationEntry migInProgress = migrationHistoryService.insertMigrationStatusInProgress(migrationInfo);
		migrationHistoryService.setMigrationStatusToFinished(migInProgress);
	}

	private <T extends Object> List<T> toList(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
	}

}
