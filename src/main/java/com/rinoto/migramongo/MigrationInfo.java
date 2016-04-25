package com.rinoto.migramongo;

public class MigrationInfo {

	private final String fromVersion;
	private final String toVersion;

	public MigrationInfo(String fromVersion, String toVersion) {
		this.fromVersion = fromVersion;
		this.toVersion = toVersion;

	}

	public String getFromVersion() {
		return fromVersion;
	}

	public String getToVersion() {
		return toVersion;
	}

}
