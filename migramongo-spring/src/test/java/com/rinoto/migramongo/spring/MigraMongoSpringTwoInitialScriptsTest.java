package com.rinoto.migramongo.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rinoto.migramongo.MigraMongo;

/**
 * This config has 2 initial mig scripts - it will fail
 * 
 * @author ela
 *
 */
@ContextConfiguration(classes = { MigraMongoSpringTestConfig.class, InitialMigScript.class,
		SecondInitialMigrationScript.class, MigScript1.class })
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class MigraMongoSpringTwoInitialScriptsTest {

	@Autowired
	private MigraMongo migraMongo;

	@Test(expected = IllegalStateException.class)
	public void shouldMigrateInitialAndMigScript1() {
		migraMongo.migrate();
	}

}
