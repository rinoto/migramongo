package com.rinoto.migramongo;

import javax.annotation.PreDestroy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

@Configuration
public class MigraMongoTestConfig {

    public static final String MIGRAMONGO_TEST_DB = "migraMongoTestDB";

    protected static MongodExecutable executable;
    protected static MongodProcess mongod;
    protected static MongoClient mongoClient;

    @Bean
    public MongoDatabase mongoDatabase() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        executable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(12345, Network.localhostIsIPv6()))
            .build());
        mongod = executable.start();
        mongoClient = new MongoClient("localhost", 12345);
        return mongoClient.getDatabase(MIGRAMONGO_TEST_DB);
    }

    @Bean
    public MigraMongo migraMongo() throws Exception {
        return new MigraMongo(mongoDatabase());
    }

    @PreDestroy
    public void destroyMongo() {
        mongoClient.close();
        mongod.stop();
        executable.stop();
    }

}
