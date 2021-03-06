package com.rinoto.migramongo.spring;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;

/**
 * This config has no initial mig scripts
 *
 * @author ela
 */
@ContextConfiguration(classes = {MigraMongoSpringTestConfig.class, MigScript1.class})
@DirtiesContext
@ExtendWith(SpringExtension.class)
public class MigraMongoSpringNoInitialScriptsTest {

    @Autowired
    private MigraMongo migraMongo;

    @Test
    public void shouldNotMigrateIfInitialNotFound() {
        // when
        final MigraMongoStatus migrate = migraMongo.migrate();
        // then
        assertThat(migrate.status, is(MigrationStatus.OK));
        assertThat(migrate.migrationsApplied, hasSize(0));
    }

}
