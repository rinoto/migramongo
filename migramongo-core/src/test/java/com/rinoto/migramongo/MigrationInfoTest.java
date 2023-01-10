package com.rinoto.migramongo;

import com.rinoto.migramongo.MigrationEntry.MigrationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MigrationInfoTest {
    @Test
    void shouldBeAbleToCreateMigrationInfo() {
         var migrationInfo = new MigrationInfo(0, 1);

         assertThat(migrationInfo)
                 .returns("0", MigrationInfo::getFromVersion)
                 .returns("1", MigrationInfo::getToVersion)
                 .returns(MigrationType.UPGRADE, MigrationInfo::getMigrationType)
                 .returns(null, MigrationInfo::getModule)
                 .returns(null, MigrationInfo::getInfo);
     }

    @Test
    void shouldNotAcceptEqualVersions() {
       var exception = assertThrows(IllegalArgumentException.class, () -> new MigrationInfo(0, 0));

       assertThat(exception.getMessage()).isEqualTo("migration versions from '0' and to '0' have to be different");
    }

    @Test
    void shouldNotAcceptNegativeFromVersion() {
       var exception = assertThrows(IllegalArgumentException.class, () -> new MigrationInfo(-1, 0));

       assertThat(exception.getMessage()).isEqualTo("migration versions from '-1' and to '0' have to be positive");
    }

    @Test
    void shouldNotAcceptNegativeToVersion() {
       var exception = assertThrows(IllegalArgumentException.class, () -> new MigrationInfo(0, -1));

       assertThat(exception.getMessage()).isEqualTo("migration versions from '0' and to '-1' have to be positive");
    }
}
