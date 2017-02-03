package com.rinoto.migramongo.reflections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.dao.MongoLockService;

public class ReflectionsMigraMongoTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldInstantiateReflectionsMigraMongoWithPackage() {
        //given
        final MongoDatabase mockDB = mock(MongoDatabase.class);
        when(mockDB.getCollection(MongoLockService.MIGRAMONGO_LOCK_COLLECTION)).thenReturn(mock(MongoCollection.class));
        final ReflectionsMigraMongo rmg = new ReflectionsMigraMongo(mockDB, "nonexistingpackage");

        //when (dummy method) - then -> no exception
        rmg.destroyLocks();
    }
}
