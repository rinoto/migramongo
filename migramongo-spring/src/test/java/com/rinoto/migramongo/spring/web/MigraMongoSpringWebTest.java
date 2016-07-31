package com.rinoto.migramongo.spring.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = {MigraMongoSpringWebTestConfig.class})
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class MigraMongoSpringWebTest {

    @Test
    public void shouldMigrateInitialAndMigScript1() {
        // when
        System.out.println("hi");
    }

}
