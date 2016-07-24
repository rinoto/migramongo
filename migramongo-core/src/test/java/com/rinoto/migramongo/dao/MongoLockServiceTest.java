package com.rinoto.migramongo.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.EmbeddedMongo;

public class MongoLockServiceTest {

	public static EmbeddedMongo mongo = new EmbeddedMongo();

	private MongoLockService lockService;
	private MongoDatabase database;

	@BeforeClass
	public static void startMongo() throws Exception {
		mongo.start();
	}

	@AfterClass
	public static void stopMongo() {
		mongo.stop();
	}

	@Before
	public void init() {
		database = mongo.getClient().getDatabase("migramongotest");
		database.getCollection(MongoLockService.MIGRAMONGO_LOCK_COLLECTION).drop();
		lockService = new MongoLockService(database);
	}

	@Test
	public void shouldAcquireLock() throws Exception {
		assertThat(lockService.acquireLock(), is(true));
	}

	@Test
	public void shouldNotAcquireLockTwice() throws Exception {
		// given
		lockService.acquireLock();
		// when - then
		assertThat(lockService.acquireLock(), is(false));
	}

	@Test
	public void shouldReleaseLockIfAcquireFirst() throws Exception {
		// given
		lockService.acquireLock();
		// when - then
		assertThat(lockService.releaseLock(), is(true));
	}

	@Test
	public void shouldNotReleaseLockIfNotAcquiredFirst() throws Exception {
		assertThat(lockService.releaseLock(), is(false));
	}

	@Test
	public void shouldCreateASecondInstanceOfLockService() throws Exception {
		// no exceptions
		new MongoLockService(database);
	}

}
