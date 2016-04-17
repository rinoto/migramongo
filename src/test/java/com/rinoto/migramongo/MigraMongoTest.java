package com.rinoto.migramongo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = {MigraMongoTestConfig.class, MigScript1.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class MigraMongoTest {

    @Autowired
    private MigraMongo migraMongo;

    @Test
    public void a() {
        MigraMongoStatus migrate = migraMongo.migrate();
        System.out.println(migrate);
    }

}
