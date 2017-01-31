package com.rinoto.migramongo;

import java.util.List;

import org.bson.types.ObjectId;

public class MigrationEntry extends MigrationRun {

    private ObjectId id;
    private String module;
    private String fromVersion;
    private String toVersion;
    private MigrationType migrationType;
    private boolean repaired = false;
    private boolean skipped = false;
    private List<MigrationRun> reruns;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(String fromVersion) {
        this.fromVersion = fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    @Override
    public String toString() {
        return "MigrationEntry [id=" +
            id +
            ", module=" +
            module +
            ", info=" +
            info +
            ", fromVersion=" +
            fromVersion +
            ", toVersion=" +
            toVersion +
            ", createdAt=" +
            createdAt +
            ", status=" +
            status +
            ", statusMessage=" +
            statusMessage +
            ", updatedAt=" +
            updatedAt +
            "]";
    }

    public boolean isRepaired() {
        return repaired;
    }

    public void setRepaired(boolean repaired) {
        this.repaired = repaired;
    }

    public MigrationType getMigrationType() {
        return migrationType;
    }

    public void setMigrationType(MigrationType migrationType) {
        this.migrationType = migrationType;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public List<MigrationRun> getReruns() {
        return reruns;
    }

    public void setReruns(List<MigrationRun> reruns) {
        this.reruns = reruns;
    }

    public enum MigrationType {
            INITIAL,
            UPGRADE;
    }

}
