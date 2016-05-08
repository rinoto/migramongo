package com.rinoto.migramongo;

import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class EmbeddedMongo {

    private MongodProcess mongod;
    private MongodExecutable executable;

    public void start() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        executable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(12345, Network.localhostIsIPv6()))
            .build());
        mongod = executable.start();
    }

    public MongoClient getClient() {
        return new MongoClient("localhost", 12345);
    }

    public void stop() {
        mongod.stop();
        executable.stop();
    }

}
