package com.rinoto.migramongo.spring.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

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
import com.rinoto.migramongo.MigraMongoStatus;
import com.rinoto.migramongo.MigraMongoStatus.MigrationStatus;
import com.rinoto.migramongo.MigraMongoTest;

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
        //given
        final MigraMongoStatus status = MigraMongoStatus
            .ok()
            .addEntry(MigraMongoTest.createMigrationEntry("3", "4", MigrationStatus.OK));
        when(migraMongo.status("42")).thenReturn(status);

        //when - then
        this.mockMvc
            .perform(get("/mongo/migration/status").param("fromVersion", "42").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.migrationsApplied[0].fromVersion").value("3"));
        verify(migraMongo).status("42");
    }

    @Test
    public void shouldDeliverHistory() throws Exception {
        //given
        when(migraMongo.getMigrationEntries()).thenReturn(
            Arrays.asList(
                MigraMongoTest.createMigrationEntry("3", "4", MigrationStatus.OK),
                MigraMongoTest.createMigrationEntry("4", "5", MigrationStatus.OK)));

        //when - then
        this.mockMvc
            .perform(get("/mongo/migration/history").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].status").value("OK"))
            .andExpect(jsonPath("$.[0].fromVersion").value("3"))
            .andExpect(jsonPath("$.[0].toVersion").value("4"))
            .andExpect(jsonPath("$.[1].status").value("OK"))
            .andExpect(jsonPath("$.[1].fromVersion").value("4"))
            .andExpect(jsonPath("$.[1].toVersion").value("5"));
        verify(migraMongo).getMigrationEntries();
    }

    @Test
    public void shouldDestroyLocks() throws Exception {
        //when - then
        this.mockMvc.perform(delete("/mongo/admin/migration/lock")).andExpect(status().isOk());
        verify(migraMongo).destroyLocks();
    }

    @Test
    public void shouldRepair() throws Exception {
        //when
        this.mockMvc
            .perform(put("/mongo/admin/migration/repair?fromVersion=1&toVersion=2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        //then
        verify(migraMongo).repair("1", "2");
    }

    @Test
    public void shouldRerun() throws Exception {
        //when
        this.mockMvc
            .perform(put("/mongo/admin/migration/rerun?fromVersion=1&toVersion=2").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        //then
        verify(migraMongo).rerun("1", "2");
    }

}
