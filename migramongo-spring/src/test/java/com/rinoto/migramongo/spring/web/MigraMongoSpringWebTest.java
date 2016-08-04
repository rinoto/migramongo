package com.rinoto.migramongo.spring.web;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.rinoto.migramongo.MigraMongo;

@WebAppConfiguration
@ContextConfiguration(classes = {MigraMongoSpringWebTestConfig.class})
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class MigraMongoSpringWebTest {

    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private MigraMongo migraMongo;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        //        this.mockMvc = MockMvcBuilders.standaloneSetup(new MigraMongoTestController()).build();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void shouldMigrateSync() throws Exception {
        //when
        this.mockMvc
            .perform(put("/mongo/migration/sync").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        //then
        verify(migraMongo).migrate();
    }

    @Test
    public void shouldMigrateAsync() throws Exception {
        //when
        this.mockMvc
            .perform(put("/mongo/migration/async").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        //then
        verify(migraMongo).migrateAsync();
    }

    @Test
    public void shouldDeliverStatus() throws Exception {
        //when
        this.mockMvc
            .perform(get("/mongo/migration/status").param("fromVersion", "42").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        //then
        verify(migraMongo).status("42");
    }

}
