package com.rinoto.migramongo;

public class MigraMongoStatus {

    public static final MigraMongoStatus OK = new MigraMongoStatus("OK", "Everything ok");

    public String status;
    public String message;

    public MigraMongoStatus(String status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String toString() {
        return "MigraMongoStatus [status=" + status + ", message=" + message + "]";
    }

}
