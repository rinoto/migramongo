package com.rinoto.migramongo;

/**
 * Exception thrown when something went wrong in the migration. It contains the status with the proper state and message.
 * 
 * @author rinoto
 */
public class MongoMigrationException extends Exception {

    private static final long serialVersionUID = 1591991696179369392L;

    private final MigraMongoStatus status;

    public MongoMigrationException(MigraMongoStatus status) {
        this.status = status;
    }

    public MigraMongoStatus getStatus() {
        return status;
    }

}
