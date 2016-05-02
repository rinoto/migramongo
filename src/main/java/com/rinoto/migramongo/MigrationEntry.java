package com.rinoto.migramongo;

import java.util.Date;

import org.bson.types.ObjectId;

import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;

public class MigrationEntry {

    private ObjectId id;
    private String module;
    private String info;
    private String fromVersion;
    private String toVersion;
    private Date createdAt;
    private MigrationStatus status;
    private String statusMessage;
    private Date updatedAt;
    private boolean repaired = false;

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

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public MigrationStatus getStatus() {
        return status;
    }

    public void setStatus(MigrationStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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

}
