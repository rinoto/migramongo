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
     * Performs the DB Migration in a synchronous mode (it will return when the migration is finished)
     * 
     * @return status of the performed migration
     */
    @RequestMapping(value = "/migration/repair", method = RequestMethod.PUT, produces = {
        MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MigraMongoStatus> repair(
            @RequestParam(value = "fromVersion", required = true) String fromVersion,
            @RequestParam(value = "toVersion", required = true) String toVersion) {
        return new ResponseEntity<>(migraMongo.repair(fromVersion, toVersion), HttpStatus.OK);
    }

}
