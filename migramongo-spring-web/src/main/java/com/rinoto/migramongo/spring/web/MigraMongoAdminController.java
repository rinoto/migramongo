package com.rinoto.migramongo.spring.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;

public class MigraMongoAdminController {

    @Autowired
    private MigraMongo migraMongo;

    /**
     * 'repairs' an entry in the <i>_migramongo</i> collection that has been marked as <code>ERROR</code> or has hanged in <code>IN_PROGRESS</code> status.
     * <p>
     * the entry gets defined by the <code>fromVersion</code> and <code>toVersion</code> parameters
     * <p>
     * if the entry does not exist, or it was not in one of the allowed states for repairing, an error status will be thrown
     * 
     * @param fromVersion from
     * @param toVersion to
     * @return status of the performed migration
     */
    @RequestMapping(value = "/migration/repair", method = RequestMethod.PUT, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MigraMongoStatus> repair(
            @RequestParam(value = "fromVersion", required = true) String fromVersion,
            @RequestParam(value = "toVersion", required = true) String toVersion) {
        return new ResponseEntity<>(migraMongo.repair(fromVersion, toVersion), HttpStatus.OK);
    }

    /**
     * runs again an entry in the <i>_migramongo</i> collection
     * <p>
     * the entry gets defined by the <code>fromVersion</code> and <code>toVersion</code> parameters
     * <ul>
     * <li>if the entry does not exist, an error status will be thrown
     * <li>if it exists, it will be re-runned, and the status of the run will be shown
     * </ul>
     * 
     * @param fromVersion from
     * @param toVersion to
     * @return status of the performed migration
     */
    @RequestMapping(value = "/migration/rerun", method = RequestMethod.PUT, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MigraMongoStatus> rerun(
            @RequestParam(value = "fromVersion", required = true) String fromVersion,
            @RequestParam(value = "toVersion", required = true) String toVersion) {
        return new ResponseEntity<>(migraMongo.rerun(fromVersion, toVersion), HttpStatus.OK);
    }

    /**
     * It removes the locks. To be used in the case when the locks in the DB are in an inconsistent state.
     */
    @RequestMapping(value = "/migration/lock", method = RequestMethod.DELETE)
    public void deleteLocks() {
        migraMongo.destroyLocks();
    }

}
