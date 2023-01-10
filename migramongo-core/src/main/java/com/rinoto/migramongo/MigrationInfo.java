package com.rinoto.migramongo;

import com.rinoto.migramongo.MigrationEntry.MigrationType;

public class MigrationInfo {

    private final String fromVersion;
    private final String toVersion;
    private String module;
    private String info;

    public MigrationInfo(String fromVersion, String toVersion) {
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;

    }

    public MigrationInfo(int fromVersion, int toVersion) {
        this(Integer.toString(fromVersion), Integer.toString(toVersion));

        if (fromVersion < 0 || toVersion < 0) {
            throw new IllegalArgumentException(String.format("migration versions from '%s' and to '%s' have to be positive", fromVersion, toVersion));
        }
        if (fromVersion == toVersion) {
            throw new IllegalArgumentException(String.format("migration versions from '%s' and to '%s' have to be different", fromVersion, toVersion));
        }
    }

    protected MigrationInfo(int initialVersion) {
        this(Integer.toString(initialVersion), Integer.toString(initialVersion));

        if (initialVersion < 0) {
            throw new IllegalArgumentException(String.format("migration version '%s' has to be positive", initialVersion));
        }
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    public String getModule() {
        return module;
    }

    public String getInfo() {
        return info;
    }

    public MigrationType getMigrationType() {
        return MigrationType.UPGRADE;
    }

}
