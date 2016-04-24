package com.rinoto.migramongo;

import java.util.Date;

public class MigrationEntry {

	public String id;
	public String module;
	public String info;
	public String fromVersion;
	public String toVersion;
	public Date createdAt;
	public String status;
	public String statusMessage;

}
