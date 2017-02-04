# migramongo

[![Maven Version](https://maven-badges.herokuapp.com/maven-central/com.github.rinoto.mongo/migramongo-core/badge.svg)](http://search.maven.org/#search|ga|1|a%3A%22migramongo-core%22)
[![Build Status](https://travis-ci.org/rinoto/migramongo.svg?branch=master)](https://travis-ci.org/rinoto/migramongo) 
[![Coverage Status](https://coveralls.io/repos/github/rinoto/migramongo/badge.svg?branch=master)](https://coveralls.io/github/rinoto/migramongo?branch=master)

### Introduction

`Migramongo` is a java tool to help you execute and maintain the history of the migration scripts in your Mongo Database.
The biggest different with respect to other existing tools is that Migramongo forces you to write the scripts in plain Java classes (or Spring beans), instead of Javascript, xml, or whatever. 

You can use `migramongo` [with](#migraMongoWithSpring) or [without](#migraMongoWithoutSpring) Spring.


### <a name="migraMongoWithSpring"></a>Using migramongo with Spring

If you are already using Spring in your project, you should go for this alternative. The biggest advantage is that the migration scripts you write are also Spring Beans, and, as such, you can inject in them any bean dependency (or configuration, add aspects, whatever...).

#### Dependencies

Add the `migramongo-spring` dependency to your project:

```xml
<dependency>
    <groupId>com.github.rinoto.mongo</groupId>
    <artifactId>migramongo-spring</artifactId>
    <version>0.8</version>
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
        return new SpringMigraMongo(appContext, mongoDatabase());
    }

}
```

You can find an example [here](https://github.com/rinoto/migramongo/blob/master/migramongo-spring/src/test/java/com/rinoto/migramongo/spring/MigraMongoSpringTestConfig.java#L51).

### Writing the migration scripts
The migration scripts are simple Java classes implementing the interfaces [InitialMongoMigrationScript](https://github.com/rinoto/migramongo/blob/master/migramongo-core/src/main/java/com/rinoto/migramongo/InitialMongoMigrationScript.java) (for the initial script), or [MongoMigrationScript](https://github.com/rinoto/migramongo/blob/master/migramongo-core/src/main/java/com/rinoto/migramongo/MongoMigrationScript.java) (for the rest of migration scripts). 
Both interfaces define 2 methods: one that delivers the migration information ([getMigrationInfo()](https://github.com/rinoto/migramongo/blob/master/migramongo-core/src/main/java/com/rinoto/migramongo/MongoMigrationScript.java#L19)) and another one that executes the migration ([migrate()](https://github.com/rinoto/migramongo/blob/master/migramongo-core/src/main/java/com/rinoto/migramongo/MongoMigrationScript.java#L26)).

You can implement the interfaces in any Spring Bean in your code. You must have one bean implementing  `InitialMongoMigrationScript` (needed for the initial script), and can have many implementing `MongoMigrationScript`.

Example of `InitialMongoMigrationScript`:

```java
@Component
public class YourProjectMigration_001 implements InitialMongoMigrationScript {

    //optional - you can inject your beans in the migration scripts
    @Inject
    OneOfYourBeans yourBeanDependency;

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
Example of `MongoMigrationScript`:

```java
@Component
public class YourProjectMigration_001_002 implements MongoMigrationScript {

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

You can find more examples in the [tests](https://github.com/rinoto/migramongo/tree/master/migramongo-spring/src/test/java/com/rinoto/migramongo/spring).


Ideally you would place all your migration beans in a `migration` package. E.g.

```
   com.yourproject.mongo.migration.YourProjectMigration_001.java
   com.yourproject.mongo.migration.YourProjectMigration_001_002.java
   com.yourproject.mongo.migration.YourProjectMigration_002_003.java
   com.yourproject.mongo.migration.YourProjectMigration_003_004.java   
```
 

#### Execution

`MigraMongo` offers a couple of methods to execute the migration scripts.

##### Migrating at startup 
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

But this option may lead to problems in a distributed environment, where more than 1 application can startup at the same time. To avoid that more than one process executes the migration at the same time, we have introduced some basic locking support (using Mongo's [findAndModify](https://docs.mongodb.com/manual/reference/method/db.collection.findAndModify) )

##### Calling the migration explicitly via JMX

If you want to have more control over the execution of the scripts, you can register an MBean instead (see [MigraMongoJMX](https://github.com/rinoto/migramongo/blob/master/migramongo-spring/src/main/java/com/rinoto/migramongo/spring/jmx/MigraMongoJMX.java)), that will expose the `MigraMongo` methods:


```java
@Configuration
@EnableMBeanExport
public class MigraMongoSpringSampleConfiguration {

    @Bean
    public MigraMongo migraMongo() throws Exception {
        //NOTE - it is up to you how you get the reference to the com.mongodb.client.MongoDatabase
        return new SpringMigraMongo(appContext, mongoDatabase());
    }

    @Bean
    public MigraMongoJMX migraMongoJMX() throws Exception {
        return new MigraMongoJMX(migraMongo());
    }
}
```

The [MigraMongoJMX](https://github.com/rinoto/migramongo/blob/master/migramongo-spring/src/main/java/com/rinoto/migramongo/spring/jmx/MigraMongoJMX.java) operations will call the migramongo methods, and return a JSON representation of the `MigraMongoStatus` delivered, containing the status and the migrations applied. E.g.

```json
{
    "status": "OK",
    "message": "Everything ok",
    "migrationsApplied": [{
        "fromVersion": "3.0",
        "createdAt": "May 29, 2016 6:06:35 PM",
        "status": "OK",
        "updatedAt": "May 29, 2016 6:06:35 PM",
        "repaired": false
    }]
}
```

`MigraMongoJMX` offers also a couple of interesting methods for getting the migration status, running the migration asynchronously, repairing some migration history entries, etc.. 
Have a look at the  [source code](https://github.com/rinoto/migramongo/blob/master/migramongo-spring/src/main/java/com/rinoto/migramongo/spring/jmx/MigraMongoJMX.java) for more information.

##### Calling the migration explicitly calling a HTTP Endpoint.

You can also register two Spring Controllers that call the migramongo methods: `MigraMongoBaseController` and `MigraMongoAdminController` . Just make sure you secure them properly!
The following is an example of registering a Controller and calling the migramongo methods from it:

```java
@RestController
@RequestMapping("/mongo")
public class MigraMongoController extends MigraMongoBaseController {
    //all endpoints are inherited from <code>MigraMongoBaseController</code>

}
```

The `MigraMongoBaseController` provides methods to execute the migration (sync or async), get the status, and the history. It's advisable to secure the calls to this methods.
The `MigraMongoAdminController` provides methods to re-run a migration, delete the db locks, and repair an entry. The methods in this controller MUST be secured.

In order to use the controllers, you will need the `migramongo-spring-web` dependency:

```xml
<dependency>
    <groupId>com.github.rinoto.mongo</groupId>
    <artifactId>migramongo-spring-web</artifactId>
    <version>0.8</version>
</dependency>
```


### <a name="migraMongoWithoutSpring"></a>Using migramongo without Spring
It's basically the same as with Spring, you just need to change
 the dependency to `migramongo-reflections` 

```xml
<dependency>
    <groupId>com.github.rinoto.mongo</groupId>
    <artifactId>migramongo-reflections</artifactId>
    <version>0.8</version>
</dependency>
```

And your migration script classes do not need to be a Spring Bean, they just must implement the `InitialMongoMigrationScript` and `MongoMigrationScript` interfaces

You can use the class `ReflectionsMigraMongo` to create an instance of `MigraMongo`. 
You just have to pass the instance of your `MongoDatabase`, and the name of the base package where your `MigraMongoScript`s are located:


```java
   ReflectionsMigraMongo migramongo = new ReflectionsMigraMongo(mongoDatabase, "com.yourpackage");
```

The `ReflectionsMigraMongo` uses internally  [ronmamo reflections library](https://github.com/ronmamo/reflections) for the lookup of the classes.


### How it works
The first time you call `MigraMongo.migrate()`, migramongo will look for classes (or Spring Beans) implementing `InitialMongoMigrationScript`  or `MongoMigrationScript`. If found, they will be executed in the following order: 
* first the `InitialMongoMigrationScript`  (there can be only one)
* then, the  `MongoMigrationScript` having the `initialVersion` of the previous `InitialMongoMigrationScript` as `fromVersion`
* then, the next `MongoMigrationScript` having the `toVersion` of the previous `MongoMigrationScript` as `fromVersion`
* and so on...
All that data is written in a collection on the mongo database called `_migramongo_upgrade_info`. The next time you call `migrate()`, only new scripts will be executed. If none available, nothing will be done.
#### and if something goes wrong?
If one of the migration scripts throws an error, the migration process will be interrupted, and the error will be delivered. 
The entry that threw the error will be marked as `ERROR` and the error will have to be fixed (probably manually).
After the error has been fixed, you can call `MigraMongo.repair` to mark the entry in the migration history as repaired, and be able to go on with the migration.

### Why do I have to specify a `from` and a `to` version? 
If you ask yourself this question, consider yourself a happy person for not having had to deal with migration scripts in different branches of the application.
Imagine we only have `version`, and we have migrated our code until version 5. 
* At some point in time, we branch the code (we have `trunk` and `branchA`).
* `branchA` gets deployed in Production, and we develop in `trunk` further.
* then we create the version script 6 for `trunk` (because of a new feature).
* and afterwards we need to create a new migration script for `branchA` because of a bug found in the system.

Which version do we give to the script for `branchA`?
If we use `from` and `to`, the version `6` would actually be `from5To6` and the branch one `from5To5_1`. Then, we just need to create an empty additional `from5_1to6` in `trunk`, and everybody is happy.
