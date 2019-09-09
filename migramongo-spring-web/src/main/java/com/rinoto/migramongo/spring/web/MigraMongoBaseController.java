package com.rinoto.migramongo.spring.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;
import com.rinoto.migramongo.MigrationEntry;

public class MigraMongoBaseController {

    @Autowired
    private MigraMongo migraMongo;

    /**
     * Performs the DB Migration in a synchronous mode (it will return when the migration is finished)
     * 
     * @return status of the performed migration
     */
    @RequestMapping(value = "/migration/sync", method = RequestMethod.PUT, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MigraMongoStatus> migrateSync() {
        return new ResponseEntity<>(migraMongo.migrate(), HttpStatus.OK);
    }

    /**
     * Performs the DB Migration in an Asynchronous mode (it will return automatically).
     * <ul>
     * <li>if there is nothing to migrate, a status with OK will be returned
     * <li>if there are items to migrate, a status with IN_PROGRESS and the items that will be migrated will be returned. Afterwards you will need to call the /status method to check the status.
     * </ul>
     * 
     * @return status
     */
    @RequestMapping(value = "/migration/async", method = RequestMethod.PUT, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MigraMongoStatus> migrateAsync() {
        return new ResponseEntity<>(migraMongo.migrateAsync(), HttpStatus.OK);
    }

    /**
     * It returns the status of the migration
     * 
     * @param fromVersion we can specify from which version we want to retrieve the status
     * @return status
     */
    @RequestMapping(value = "/migration/status", method = RequestMethod.GET, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MigraMongoStatus> status(
            @RequestParam(value = "fromVersion", required = false) String fromVersion) {
        return new ResponseEntity<>(migraMongo.status(fromVersion), HttpStatus.OK);
    }

    /**
     * It returns all the entries that have been migrated (migration history)
     * 
     * @return history
     */
    @RequestMapping(value = "/migration/history", method = RequestMethod.GET, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<MigrationEntry>> migrationHistory() {
        return new ResponseEntity<>(migraMongo.getMigrationEntries(), HttpStatus.OK);
    }

}
