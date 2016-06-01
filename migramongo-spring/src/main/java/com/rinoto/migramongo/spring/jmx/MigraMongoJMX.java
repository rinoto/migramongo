package com.rinoto.migramongo.spring.jmx;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.gson.Gson;
import com.rinoto.migramongo.MigraMongo;

@ManagedResource
public class MigraMongoJMX {

    private final MigraMongo migraMongo;
    private final Gson gson;

    public MigraMongoJMX(MigraMongo migraMongo) {
        this.migraMongo = migraMongo;
        this.gson = new Gson();
    }

    @ManagedOperation
    public String migrate() {
        return gson.toJson(migraMongo.migrate());
    }

    @ManagedOperation
    public String dryRun() {
        return gson.toJson(migraMongo.dryRun());
    }

    @ManagedOperation
    public Boolean needsMigration() {
        return !migraMongo.dryRun().migrationsApplied.isEmpty();
    }

    @ManagedOperation
    public String history() {
        return gson.toJson(migraMongo.getMigrationEntries());
    }

}
