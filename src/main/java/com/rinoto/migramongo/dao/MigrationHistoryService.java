package com.rinoto.migramongo.dao;

import com.rinoto.migramongo.MigrationEntry;
import com.rinoto.migramongo.MigrationInfo;

public interface MigrationHistoryService {

    MigrationEntry getLastMigrationApplied();

    MigrationEntry setMigrationStatusToFinished(MigrationEntry migrationEntry);

    MigrationEntry setMigrationStatusToFailed(MigrationEntry migrationEntry, Exception e);

    MigrationEntry insertMigrationStatusInProgress(MigrationInfo migrationInfo);

}
