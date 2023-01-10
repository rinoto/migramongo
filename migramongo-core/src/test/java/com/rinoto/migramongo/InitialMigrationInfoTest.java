package com.rinoto.migramongo;

import com.rinoto.migramongo.MigrationEntry.MigrationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InitialMigrationInfoTest {
    @Test
    void shouldBeAbleToCreateInitialMigrationInfo() {
        var migrationInfo = new InitialMigrationInfo(0);

        assertThat(migrationInfo)
                .returns("0", MigrationInfo::getFromVersion)
                .returns("0", MigrationInfo::getToVersion)
                .returns(MigrationType.INITIAL, MigrationInfo::getMigrationType)
                .returns(null, MigrationInfo::getModule)
                .returns(null, MigrationInfo::getInfo);
    }

    @Test
    void shouldNotAcceptNegativeToVersion() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new InitialMigrationInfo(-1));

        assertThat(exception.getMessage()).isEqualTo("migration version '-1' has to be positive");
    }
}
