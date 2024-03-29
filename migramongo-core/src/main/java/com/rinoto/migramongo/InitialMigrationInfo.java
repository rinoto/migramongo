package com.rinoto.migramongo;

import com.rinoto.migramongo.MigrationEntry.MigrationType;

public class InitialMigrationInfo extends MigrationInfo {

	public InitialMigrationInfo(String initialVersion) {
		super(initialVersion, initialVersion);
	}

	public InitialMigrationInfo(int initialVersion) {
		super(initialVersion);
	}

	@Override
	public MigrationType getMigrationType() {
		return MigrationType.INITIAL;
	}

}
