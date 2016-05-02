package com.rinoto.migramongo;

import java.util.ArrayList;
import java.util.List;

public class MigraMongoStatus {

    public MigrationStatus status;
    public String message;
    public List<MigrationEntry> migrationsApplied = new ArrayList<>();

    public MigraMongoStatus(MigrationStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public static final MigraMongoStatus ok() {
        return ok("Everything ok");
    }

    public static final MigraMongoStatus ok(String message) {
        return new MigraMongoStatus(MigrationStatus.OK, message);
    }

    public static final MigraMongoStatus error(String errorMessage) {
        return new MigraMongoStatus(MigrationStatus.ERROR, errorMessage);
    }

    public MigraMongoStatus addEntry(MigrationEntry migEntry) {
        migrationsApplied.add(migEntry);
        return this;
    }

    @Override
    public String toString() {
        return "MigraMongoStatus [status=" +
            status +
            ", message=" +
            message +
            ", appliedEntries=" +
            migrationsApplied +
            "]";
    }

    public enum MigrationStatus {
            OK,
            ERROR,
            IN_PROGRESS;
    }

}
