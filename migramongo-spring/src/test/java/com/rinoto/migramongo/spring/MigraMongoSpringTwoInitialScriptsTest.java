package com.rinoto.migramongo.spring;

import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * This config has 2 initial mig scripts - it will fail
 *
 * @author ela
 *
 */
@ContextConfiguration(classes = { MigraMongoSpringTestConfig.class, InitialMigScript.class,
		SecondInitialMigrationScript.class, MigScript1.class })
@DirtiesContext
@ExtendWith(SpringExtension.class)
public class MigraMongoSpringTwoInitialScriptsTest {

	@Autowired
	private MigraMongo migraMongo;

	@Test
	public void shouldMigrateInitialAndMigScript1() {
		// when
		MigraMongoStatus status = migraMongo.migrate();

		//then
		assertThat(status.status, is(MigraMongoStatus.MigrationStatus.ERROR));
		assertThat(status.message, Matchers.startsWith("There cannot be more than one InitialMigrationScript"));
	}

}
