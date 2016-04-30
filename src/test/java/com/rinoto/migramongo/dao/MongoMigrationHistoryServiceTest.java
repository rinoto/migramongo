package com.rinoto.migramongo.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rinoto.migramongo.spring.MigraMongoSpringTestConfig;

@ContextConfiguration(classes = { MigraMongoSpringTestConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class MongoMigrationHistoryServiceTest {

	@Autowired
	MigrationHistoryService migrationHistoryService;

	@Test
	public void shouldNotReturnLastMigrationIfNoneExists() throws Exception {
		assertThat(migrationHistoryService.getLastMigrationApplied(), nullValue());
	}

}
