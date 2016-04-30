package com.rinoto.migramongo.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rinoto.migramongo.InitialMigrationInfo;
import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;
import com.rinoto.migramongo.spring.MigraMongoSpringTestConfig;

@ContextConfiguration(classes = { MigraMongoSpringTestConfig.class })
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class MongoMigrationHistoryServiceTest {

	@Autowired
	MongoMigrationHistoryService migrationHistoryService;

	@Before
	public void clearMigrations() {
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

	private void addMigration(final MigrationInfo migrationInfo) throws InterruptedException {
		Thread.sleep(1);
		final MigrationEntry migInProgress = migrationHistoryService.insertMigrationStatusInProgress(migrationInfo);
		migrationHistoryService.setMigrationStatusToFinished(migInProgress);
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

}
