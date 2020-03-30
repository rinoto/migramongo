package com.rinoto.migramongo.spring;

import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.MigraMongoStatus;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@ContextConfiguration(classes = { MigraMongoSpringTestConfig.class, InitialMigScript.class, MigScript1.class })
@DirtiesContext
@ExtendWith(SpringExtension.class)
public class MigraMongoSpringTest {

	@Autowired
	private MigraMongo migraMongo;

	@Test
	public void shouldMigrateInitialAndMigScript1() {
		// when
		final MigraMongoStatus migrate = migraMongo.migrate();
		// then
		assertThat(migrate.status, is(MigrationStatus.OK));
		assertThat(migrate.migrationsApplied, hasSize(2));
		assertThat(migrate.migrationsApplied.get(0).getFromVersion(), is("1"));
		assertThat(migrate.migrationsApplied.get(0).getToVersion(), is("1"));
	}

}
