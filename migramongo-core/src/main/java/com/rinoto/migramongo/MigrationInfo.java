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

    @Override
    public String toString() {
        return "MigrationInfo [fromVersion=" +
            fromVersion +
            ", toVersion=" +
            toVersion +
            ", module=" +
            module +
            ", info=" +
            info +
            "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fromVersion == null) ? 0 : fromVersion.hashCode());
        result = prime * result + ((info == null) ? 0 : info.hashCode());
        result = prime * result + ((module == null) ? 0 : module.hashCode());
        result = prime * result + ((toVersion == null) ? 0 : toVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MigrationInfo other = (MigrationInfo) obj;
        if (fromVersion == null) {
            if (other.fromVersion != null)
                return false;
        } else if ( !fromVersion.equals(other.fromVersion))
            return false;
        if (info == null) {
            if (other.info != null)
                return false;
        } else if ( !info.equals(other.info))
            return false;
        if (module == null) {
            if (other.module != null)
                return false;
        } else if ( !module.equals(other.module))
            return false;
        if (toVersion == null) {
            if (other.toVersion != null)
                return false;
        } else if ( !toVersion.equals(other.toVersion))
            return false;
        return true;
    }

}
