# migramongo

[![Build Status](https://travis-ci.org/rinoto/migramongo.svg?branch=master)](https://travis-ci.org/rinoto/migramongo) 
[![Coverage Status](https://coveralls.io/repos/github/rinoto/migramongo/badge.svg?branch=master)](https://coveralls.io/github/rinoto/migramongo?branch=master)

### Introduction

`Migramongo` is a tool to help you execute and mantain the history of the migration scripts in your Mongo Database.


### Using migramongo with Spring

#### Dependencies

Add the `migramongo-spring` dependency to your project:

```xml
<dependency>
    <groupId>com.github.rinoto.mongo</groupId>
    <artifactId>migramongo-spring</artifactId>
    <version>0.1</version>
</dependency>
```
#### Configuration

Register `Migramongo` with an instance of `SpringMigraMongo` in a `@Configuration`. All you need is a reference to the `com.mongodb.client.MongoDatabase`. For example:

```java
@Configuration
public class MigraMongoSpringSampleConfiguration {

    @Autowired
    private ApplicationContext appContext;

    @Bean
    public MigraMongo migraMongo() throws Exception {
        //NOTE - it is up to you how you get the reference to the com.mongodb.client.MongoDatabase
        return new SpringMigraMongo(appContext, mongoDatabase(), new MongoMigrationHistoryService(mongoDatabase()));
    }

}
```

### Writing the migration scripts
The migration scripts are simple Java classes implementing the interfaces `InitialMongoMigrationScript` (for the initial script), or `MongoMigrationScript` (for the rest of migration scripts). Both interfaces define 2 methods: one that delivers the migration information (`getMigrationInfo()`) and another one that executes the migration (`migrate()`).

You can implement the interfaces in any Spring Bean in your code. You must have one bean implementing  `InitialMongoMigrationScript` (needed for the initial script), and can have many implementing   `MongoMigrationScript`.

```java
@Component
public class YourProjectMigration_001 implements InitialMongoMigrationScript {

    @Override
    public InitialMigrationInfo getMigrationInfo() {
        return new InitialMigrationInfo("001");
    }

    @Override
    public void migrate(MongoDatabase database) throws Exception {
        //write your migration code here
    }
}
```

```java
@Component
public class YourProjectMigration_001_002 implements InitialMongoMigrationScript {

    @Override
    public InitialMigrationInfo getMigrationInfo() {
        return new InitialMigrationInfo("001");
    }

    @Override
    public void migrate(MongoDatabase database) throws Exception {
        //write your migration code here
    }
}
```

```java
@Component
public class YourProjectMigration_001 implements MongoMigrationScript {

    @Override
    public MigrationInfo getMigrationInfo() {
        return new MigrationInfo("001", "002");
    }

    @Override
    public void migrate(MongoDatabase database) throws Exception {
        //perform your migration here
    }

}
```



Ideally you would place all your migration beans in a `migration` package. E.g.

```
   com.yourproject.mongo.migration.YourProjectMigration_001.java
   com.yourproject.mongo.migration.YourProjectMigration_001_002.java
   com.yourproject.mongo.migration.YourProjectMigration_002_003.java
   com.yourproject.mongo.migration.YourProjectMigration_003_004.java   
```
 

#### Execution

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

    @Bean
    public MigraMongoJMX migraMongoJMX() throws Exception {
        return new MigraMongoJMX(migraMongo());
    }
}
```

#### How it works
The first time you call `MigraMongo.migrate()`, migramongo will look for Spring Beans implementing `InitialMongoMigrationScript`  or `MongoMigrationScript`. If found, they will be executed in the following order: 
* first the `InitialMongoMigrationScript`  (there can be only one)
* then, the  `MongoMigrationScript` having the `initialVersion` of the previous `InitialMongoMigrationScript` as `fromVersion`
* then, the next `MongoMigrationScript` having the `toVersion` of the previous `MongoMigrationScript` as `fromVersion`
* and so on...
All that data is written in a collection on the mongo database called `_migramongo_history`. The next time you call `migrate`, only new scripts will be executed. If none available, nothing will be done.
