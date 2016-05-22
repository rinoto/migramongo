# migramongo

[![Build Status](https://travis-ci.org/rinoto/migramongo.svg?branch=master)](https://travis-ci.org/rinoto/migramongo) 
[![Coverage Status](https://coveralls.io/repos/github/rinoto/migramongo/badge.svg?branch=master)](https://coveralls.io/github/rinoto/migramongo?branch=master)

### Introduction

`Migramongo` is a tool to help you execute and mantain the history of the migration scripts in your Mongo Database.


### Using migramongo with Spring

Add the `migramongo-spring` dependency to your project:

```xml
<dependency>
    <groupId>com.github.rinoto.mongo</groupId>
    <artifactId>migramongo-spring</artifactId>
    <version>0.1</version>
</dependency>
```

Register `Migramongo` with an instance of `SpringMigraMongo` in a `@Configuration`. All you need is a reference to the `com.mongodb.client.MongoDatabase`. For example:

```java
@Configuration
public class MigraMongoSpringSampleConfiguration {

    @Bean
    public MigraMongo migraMongo() throws Exception {
        //NOTE - it is up to you how you get the reference to the com.mongodb.client.MongoDatabase
        return new SpringMigraMongo(appContext, mongoDatabase(), new MongoMigrationHistoryService(mongoDatabase()));
    }

}
```

`MigraMongo` offers a couple of methods to execute the migration scripts. 
The easiest way to make sure that all your scripts have been executed is to call the `migrate` method at the startup of your application (i.e. in any `@PostConstruct`):

```java
@Component
public class MyStartupBean {

   @Autowired
   private MigraMongo migraMongo;

    @PostConstruct
    public void executeMigrationScripts throws Exception {
        migraMongo.migrate();
    }

}
```

But this option may lead to problems in a distributed environment, where more than 1 application can startup at the same time.
If you want to have more control over the execution of the scripts, you can register an MBean instead, that will expose the `MigraMongo` methods:


```java
@Configuration
@EnableMBeanExport
public class MigraMongoSpringSampleConfiguration {

    @Bean
    public MigraMongo migraMongo() throws Exception {
        //NOTE - it is up to you how you get the reference to the com.mongodb.client.MongoDatabase
        return new SpringMigraMongo(appContext, mongoDatabase(), new MongoMigrationHistoryService(mongoDatabase()));
    }

}
```
