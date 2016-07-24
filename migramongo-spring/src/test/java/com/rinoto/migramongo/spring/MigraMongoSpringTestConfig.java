package com.rinoto.migramongo.spring;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.rinoto.migramongo.EmbeddedMongo;
import com.rinoto.migramongo.MigraMongo;
import com.rinoto.migramongo.dao.LockService;
import com.rinoto.migramongo.dao.MigrationHistoryService;
import com.rinoto.migramongo.dao.MongoLockService;
import com.rinoto.migramongo.dao.MongoMigrationHistoryService;
import com.rinoto.migramongo.spring.jmx.MigraMongoJMX;

@Configuration
@Import(EmbeddedMongoConfig.class)
@ComponentScan(basePackageClasses = MigraMongoSpringTestConfig.class)
@EnableMBeanExport
public class MigraMongoSpringTestConfig {

	public static final String MIGRAMONGO_TEST_DB = "migraMongoTestDB";

	@Autowired
	ApplicationContext appContext;
	@Autowired
	EmbeddedMongo embeddedMongo;

	private static MongoClient mongoClient;

	@Bean
	public MongoDatabase mongoDatabase() throws Exception {
		embeddedMongo.start();
		mongoClient = new MongoClient("localhost", 12345);
		return mongoClient.getDatabase(MIGRAMONGO_TEST_DB);
	}

	@Bean
	public MigrationHistoryService migrationHistoryService() throws Exception {
		return new MongoMigrationHistoryService(mongoDatabase());
	}

	@Bean
	public LockService lockService() throws Exception {
		return new MongoLockService(mongoDatabase());
	}

	@Bean
	@Primary
	public MigraMongo migraMongo() throws Exception {
		return new SpringMigraMongo(appContext, mongoDatabase(), migrationHistoryService());
	}

	@Bean
	public MigraMongoJMX migraMongoJMX() throws Exception {
		return new MigraMongoJMX(migraMongo());
	}

	@PreDestroy
	public void destroyMongo() {
		mongoClient.close();
		embeddedMongo.stop();
	}

}
