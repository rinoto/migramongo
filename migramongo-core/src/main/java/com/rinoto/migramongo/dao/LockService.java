package com.rinoto.migramongo.dao;

/**
 * Acquires and releases locks.
 * <p>
 * To avoid concurrent executions in migramongo.
 * 
 */
public interface LockService {

	/**
	 * Acquires the lock
	 * <p>
	 * <b>IMPORTANT</b> The lock <b>MUST</b> be explicitly released by the
	 * client
	 * 
	 * @return true if the lock could be acquired. false if not (i.e. someone
	 *         else alredy acquired it before, and it has not released it yet)
	 */
	boolean acquireLock();

	/**
	 * Releases the lock
	 * 
	 * @return true if the release of the lock was succesful
	 */
	boolean releaseLock();
}
