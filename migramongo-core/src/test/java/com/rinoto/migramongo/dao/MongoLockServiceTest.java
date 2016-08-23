package com.rinoto.migramongo.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void onlyOneThreadShouldAcquireLockWhenRunningConcurrently() throws Exception {
        // given
        final int numberOfWorkers = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
        final AtomicInteger locksAcquired = new AtomicInteger(0);
        final AtomicInteger locksRejected = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfWorkers; i++ ) {
            executor.submit(
                () -> lockService.acquireLock() ? locksAcquired.incrementAndGet() : locksRejected.incrementAndGet());
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(locksAcquired.get(), is(1));
        assertThat(locksRejected.get(), is(numberOfWorkers - 1));
    }

    @Test
    public void onlyOneThreadShouldAcquireLockWhenRunningConcurrentlyAndLockObjectsAreCreatedConcurrently()
            throws Exception {
        // given
        final int numberOfWorkers = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
        final AtomicInteger locksAcquired = new AtomicInteger(0);
        final AtomicInteger locksRejected = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfWorkers; i++ ) {
            executor.submit(() -> {
                final LockService thisLockService = new MongoLockService(database);
                return thisLockService.acquireLock()
                    ? locksAcquired.incrementAndGet()
                    : locksRejected.incrementAndGet();
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // then
        assertThat(locksAcquired.get(), is(1));
        assertThat(locksRejected.get(), is(numberOfWorkers - 1));
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

    @Test
    public void shouldGetLockInfoWhenUnlocked() throws Exception {
        assertThat(lockService.getLockInformation().isLocked(), is(false));
        assertThat(lockService.getLockInformation().getLastLockDate(), nullValue());
        assertThat(lockService.getLockInformation().getLastReleaseDate(), nullValue());
    }

    @Test
    public void shouldGetLockInfoWhenLocked() throws Exception {
        // when
        lockService.acquireLock();
        // then
        assertThat(lockService.getLockInformation().isLocked(), is(true));
        assertThat(lockService.getLockInformation().getLastLockDate(), not(nullValue()));
        assertThat(lockService.getLockInformation().getLastReleaseDate(), nullValue());
    }

    @Test
    public void shouldGetLockInfoWhenLockedAndReleased() throws Exception {
        // when
        lockService.acquireLock();
        Thread.sleep(1);
        lockService.releaseLock();
        // then
        assertThat(lockService.getLockInformation().isLocked(), is(false));
        assertThat(lockService.getLockInformation().getLastLockDate(), not(nullValue()));
        assertThat(lockService.getLockInformation().getLastReleaseDate(), not(nullValue()));
        assertThat(
            lockService.getLockInformation().getLastLockDate(),
            lessThan(lockService.getLockInformation().getLastReleaseDate()));
    }

    @Test
    public void shouldDestroyLock() throws Exception {
        // when
        lockService.destroyLock();

        // then
        assertThat(lockService.getLockInformation().isLocked(), is(false));
        assertThat(lockService.getLockInformation().getLastLockDate(), nullValue());
        assertThat(lockService.getLockInformation().getLastReleaseDate(), nullValue());
    }

    @Test
    public void shouldDestroyLockWhileLocked() throws Exception {
        // when
        lockService.acquireLock();
        lockService.destroyLock();

        // then
        assertThat(lockService.getLockInformation().isLocked(), is(false));
        assertThat(lockService.getLockInformation().getLastLockDate(), nullValue());
        assertThat(lockService.getLockInformation().getLastReleaseDate(), nullValue());
    }

}
