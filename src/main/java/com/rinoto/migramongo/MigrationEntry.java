package com.rinoto.migramongo;

import java.util.Date;

import org.bson.types.ObjectId;

public class MigrationEntry {

	public ObjectId id;
	public String module;
	public String info;
	public String fromVersion;
	public String toVersion;
	public Date createdAt;
	public String status;
	public String statusMessage;
	public Date updatedAt;

}
