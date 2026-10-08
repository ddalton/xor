# Getting started

This page adds XOR to a Spring and JPA application, configures it, and runs a first create and query. All
configuration and code below comes from the XOR test suite (`src/test/resources` and `src/test/java`).

## Requirements

* Java 17 or later
* A Jakarta Persistence 3.2 provider. The tests use Hibernate ORM 7.4.
* Spring Framework 7.0 for the Spring based configuration shown here

## Maven dependency

```xml
<dependency>
    <groupId>tools.xor</groupId>
    <artifactId>xor</artifactId>
    <version>3.0.1</version>
</dependency>
```

The version shown is the one in this repository's `pom.xml`. To use a snapshot of the source, run
`mvn install -Dgpg.skip` (see [Development](development.md)) and depend on the installed version.

XOR depends on `jakarta.persistence-api`, `spring-core`, `spring-beans`, `spring-context` and `spring-orm`.
Your application supplies:

| Dependency | Why |
| --- | --- |
| A JPA provider, e.g. `org.hibernate.orm:hibernate-core` | XOR does not ship one |
| A JDBC driver and connection pool | The tests use HSQLDB and `commons-dbcp2` |
| `org.apache.poi:poi` and `poi-ooxml` | Only for Excel import and export and domain values. They are `provided` scope in XOR |

## Spring configuration

Three beans are needed:

| Bean | Class | Role |
| --- | --- | --- |
| `aggregateManager` | `tools.xor.service.AggregateManager` | The entry point for every operation |
| `jpadas` | `tools.xor.service.SpringDataModelFactory` | Creates the `DataModel` (types) and the `DataStore` (session) |
| `jpabuilder` | `tools.xor.service.JPASpringDataModelBuilder` | Builds the model from the JPA metamodel |

This is the XOR part of `src/test/resources/spring-jpa-test.xml`:

```xml
<context:component-scan base-package="tools.xor" />

<bean id="aggregateManager" class="tools.xor.service.AggregateManager">
    <property name="dataModelFactory" ref="jpadas" />
</bean>

<bean id="jpadas" class="tools.xor.service.SpringDataModelFactory">
    <property name="name" value="jpadas" />
    <property name="dataModelBuilder" ref="jpabuilder" />
</bean>

<bean id="jpabuilder" class="tools.xor.service.JPASpringDataModelBuilder" />
```

The rest of the file is standard Spring JPA setup: a `DataSource`, a
`LocalContainerEntityManagerFactoryBean` named `entityManagerFactory`, a `JpaTransactionManager` and
`<tx:annotation-driven/>`. XOR joins the transaction of the caller.

Optional `AggregateManager` properties:

| Property | Default | Purpose |
| --- | --- | --- |
| `typeMapper` | `DefaultTypeMapper` | Selects the external model, e.g. JSON. See [Architecture](architecture.md#typemapper) |
| `viewFiles` | none | Extra view files to load in addition to `AggregateViews.xml` |
| `associationStrategy` | `DefaultAssociationStrategy` | Decides which extra objects are processed with an aggregate |
| `persistenceType` | not set | Set to `JDBC` when the data model is built from JDBC, as in the JDBC configuration below |
| `viewsDirectory` | none | Directory where generated view files are written |

### Java configuration

The same beans in a `@Configuration` class:

```java
@Configuration
public class XorConfig {

    @Bean
    public SpringDataModelFactory jpadas() {
        SpringDataModelFactory factory = new SpringDataModelFactory();
        factory.setName("jpadas");
        factory.setDataModelBuilder(new JPASpringDataModelBuilder());
        return factory;
    }

    @Bean
    public AggregateManager aggregateManager() {
        AggregateManager am = new AggregateManager();
        am.setDataModelFactory(jpadas());
        return am;
    }
}
```

Beans are referenced by calling the `@Bean` methods rather than by type, because `SpringDataModelFactory`
and the data model builders are also annotated with `@Component` and would be found twice when
`tools.xor` is component scanned.

### Working with JSON

To exchange data as `org.json.JSONObject` instead of domain objects, set a JSON type mapper. This is from
`src/test/resources/spring-mutable-JSON-jpa-test.xml`:

```xml
<bean id="aggregateManager" class="tools.xor.service.AggregateManager">
    <property name="viewFiles">
        <list>
            <value>CustomViews.xml</value>
        </list>
    </property>
    <property name="typeMapper" ref="typeMapper" />
    <property name="dataModelFactory" ref="mutablejsonjpadas" />
</bean>

<bean id="typeMapper" class="tools.xor.ExcelJsonTypeMapper">
    <property name="domainPackagePath" value="tools.xor.db" />
</bean>
```

`ExcelJsonTypeMapper` extends `MutableJsonTypeMapper` and adds type and identity information to each JSON
object, which keeps shared and cyclic references intact.

### Using plain JDBC

XOR can also work directly on the database catalog, without an ORM. From
`src/test/resources/spring-jdbc-test.xml`:

```xml
<bean id="aggregateManager" class="tools.xor.service.AggregateManager">
    <property name="typeMapper" ref="typeMapper" />
    <property name="dataModelFactory" ref="jdbcdas" />
    <property name="persistenceType" value="JDBC" />
</bean>

<bean id="jdbcdas" class="tools.xor.service.SpringDataModelFactory">
    <property name="name" value="jdbcdas" />
    <property name="dataModelBuilder" ref="jdbcbuilder" />
</bean>

<bean id="jdbcbuilder" class="tools.xor.service.JDBCSpringDataModelBuilder" />

<bean id="typeMapper" class="tools.xor.UnchangedTypeMapper" />
```

> **Note**
> Under JPA, stored procedures, the query join table and BLOB creation need a provider specific
> `tools.xor.service.PersistenceUtil`, set with the `persistenceUtil` property of
> `JPASpringDataModelBuilder`. The tests contain a Hibernate implementation,
> `src/test/java/tools/xor/service/HibernatePersistenceUtil.java`, that you can adapt.

## Views file

At startup `AggregateManager` loads `AggregateViews.xml` from the root of the classpath, followed by any
files listed in `viewFiles`. A missing `AggregateViews.xml` is only logged as a warning. See [Views](views.md).

## Optional settings: `xor.properties`

A `xor.properties` file on the classpath tunes some internals. It is read as a file, so place it in a
classes directory rather than inside a JAR. Some of the keys:

| Key | Meaning |
| --- | --- |
| `query.pool.size` | Threads used by the parallel query dispatcher (default 4) |
| `query.join.table` | Name of the query join table (default `XOR_QUERY_JOIN_`) |
| `excel.streaming` | Use the POI streaming workbook (`SXSSFWorkbook`) for Excel export |

The keys are defined in `tools.xor.util.Constants.Config`.

## A first create and read

With a JSON type mapper, create a `Person` from a `JSONObject` and read it back. From
`DefaultMutableJson.checkStringField`:

```java
JSONObject json = new JSONObject();
json.put("name", "DILIP_DALTON");
json.put("displayName", "Dilip Dalton");
json.put("description", "Software engineer in the bay area");
json.put("userName", "daltond");

Settings settings = new Settings();
settings.setEntityClass(Person.class);
Person person = (Person) aggregateService.create(json, settings);

JSONObject result = (JSONObject) aggregateService.read(person, settings);
assert(result.get("name").toString().equals("DILIP_DALTON"));
```

`create` returns the persistent entity. `read` returns the entity in the external model, here a
`JSONObject`.

## A first query

`query` fetches data in the shape of a view. From `DefaultQueryOperation.queryPerson`, with the default
type mapper:

```java
Person person = new Person();
person.setName("GEORGE_WASHINGTON_4");
person.setDisplayName("George Washington");
person.setDescription("First President of the United States of America");
person.setUserName("gwashington");
person = (Person) aggregateService.create(person, new Settings());

Settings settings = new Settings();
settings.setView(aggregateService.getView("BASICINFO"));
List<?> toList = aggregateService.query(person, settings);

Person result = (Person) toList.get(0);
assert(result.getName().equals("GEORGE_WASHINGTON_4"));
```

Because the input object has an id, the query is restricted to that entity. Pass `null` as the first
argument and set the entity type on the settings to query all entities of a type:

```java
settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
List<?> tasks = aggregateService.query(null, settings);
```

Previous: [Overview](README.md) | Next: [Architecture](architecture.md)
