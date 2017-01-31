package com.rinoto.migramongo;

import java.util.Date;

import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;

public class MigrationRun {

    protected String info;
    protected MigrationStatus status;
    protected String statusMessage;
    protected Date createdAt;
    protected Date updatedAt;
    public String getInfo() {
        return info;
    }
    public void setInfo(String info) {
        this.info = info;
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

}
