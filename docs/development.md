# Development

XOR is a plain Maven project.

## Requirements

* JDK 17 or later. The compiler targets Java 17 (`java.release` in `pom.xml`).
* Maven 3

## Build and test

| Command | Does |
| --- | --- |
| `mvn test` | Compiles and runs the test suite |
| `mvn test -Dtest=JPAQueryOperationTest` | Runs one test class |
| `mvn package` | Builds the JAR, sources JAR and javadoc JAR |
| `mvn install -Dgpg.skip` | Installs the build in the local Maven repository |

The build signs artifacts with `maven-gpg-plugin` in the `verify` phase, so `verify` and `install` need
`-Dgpg.skip` unless a GPG key is configured.

The tests run against an in-memory HSQLDB database with Hibernate ORM and Spring, configured in
`src/test/resources/cfg-test.properties`. That file also contains commented settings for H2, Oracle,
SAP HANA and PostgreSQL, for running the suite against another database.

## Project layout

| Path | Content |
| --- | --- |
| `src/main/java/tools/xor` | Type system, type mappers, generators of collections and relationships |
| `src/main/java/tools/xor/service` | `AggregateManager`, data models, data stores, shapes, Meta API |
| `src/main/java/tools/xor/view` | Views and the query engine |
| `src/main/java/tools/xor/operation` | Operations |
| `src/main/java/tools/xor/generator` | Field generators |
| `src/main/java/tools/xor/service/exim` | Excel and CSV import and export, CSV loader |
| `src/main/java/tools/xor/providers/jdbc` | JDBC data model, data store and SQL dialects |
| `src/test/java/tools/xor/db` | The test domain model (`Task`, `Person`, `Project`, ...) |
| `src/test/java/tools/xor/logic` | Test logic shared between configurations |
| `src/test/java/tools/xor/jpa` | Test classes, mostly one per area, bound to a Spring configuration |
| `src/test/resources` | Spring configurations, `AggregateViews.xml`, Excel and CSV test data |

Most test classes in `jpa` extend a class from `logic` and pick a Spring configuration with
`@ContextConfiguration`, so the same scenario runs against different type mappers and data models. They are
the best reference for using the API.

## Documentation

This documentation lives in `docs/` as GitHub Flavored Markdown with Mermaid diagrams, and is meant to be
read on GitHub. Keep code samples in sync with the tests they are taken from.

Previous: [Visualization](visualization.md) | Back to the [overview](README.md)
