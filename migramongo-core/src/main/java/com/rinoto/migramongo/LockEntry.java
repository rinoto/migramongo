package com.rinoto.migramongo;

import java.util.Date;

public class LockEntry {

	private final boolean locked;
	private final Date lastLockDate;
	private final Date lastReleaseDate;

	public LockEntry(boolean locked, Date lastLockDate, Date lastReleaseDate) {
		this.locked = locked;
		this.lastLockDate = lastLockDate;
		this.lastReleaseDate = lastReleaseDate;
	}

	public boolean isLocked() {
		return locked;
	}

	public Date getLastLockDate() {
		return lastLockDate;
	}

	public Date getLastReleaseDate() {
		return lastReleaseDate;
	}

}
