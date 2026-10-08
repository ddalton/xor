# Data generation

XOR generates data for the types of a model. Typical uses:

* Reproduce a performance problem when the customer's data cannot be shared
* Create large data sets for performance tests while the model is still changing
* Create data for unit tests against an in-memory database

Data can be generated in two ways:

| Approach | API | Speed | Use when |
| --- | --- | --- | --- |
| Object graphs | `TypeGraph.generateObjectGraph(Settings)`, then `create` or `update` | Goes through the ORM | You want realistic aggregates that pass business logic |
| Rows, straight into tables | `AggregateManager.generate` or `generateSameTX` on a JDBC data model, or the CSV loader | Bypasses the ORM | You need many rows quickly |

In both cases the values of each field come from a **generator**.

## Generators

A generator implements `tools.xor.generator.Generator`. It is attached to a property:

```java
ExtendedProperty property = (ExtendedProperty) entityType.getProperty("name");
property.setGenerator(new StringTemplate(new String[] {"NAME_[VISITOR_CONTEXT]"}));
```

`setGenerator(String incomingProperty, Generator)` attaches a generator that is used only when the entity
is reached through the named relationship. Properties without a generator get random values.

### The Generator interface

| Method | Called for |
| --- | --- |
| `getStringValue(Property, StateGraph.ObjectGenerationVisitor)` | String properties |
| `getIntValue`, `getLongValue`, `getShortValue`, `getByteValue`, `getCharValue`, `getDoubleValue`, `getFloatValue`, `getBigDecimal`, `getBigInteger`, `getDateValue` (each takes the visitor) | Properties of that type |
| `getFanout(Property, Settings, String, StateGraph.ObjectGenerationVisitor)` | Number of elements of a collection |
| `getSubType(EntityType, StateGraph)` | Which subtype to create for a relationship to an inheritance hierarchy |
| `validate(ExtendedProperty)` | Checking the configuration when the generator is attached |
| `init(StateGraph.ObjectGenerationVisitor)` | Initialization before use |
| `getCurrentValue(StateGraph.ObjectGenerationVisitor)` | The current value, used by other generators such as `StringTemplate` |
| `isApplicableToCollectionElement()` | Whether it applies to the elements rather than to the collection |

`DefaultGenerator` implements all of them. Its arguments are a `String[]`. For numbers and dates it returns
a random value between the first argument (minimum) and the second (maximum). For strings it picks one of
the arguments at random.

### Built-in generators

In package `tools.xor.generator` unless stated otherwise:

| Generator | Arguments | Produces |
| --- | --- | --- |
| `DefaultGenerator` | `min, max`, or a list of strings | A random number in the range, or one of the strings |
| `Range` | `min, max` | A number in the range. Also sets the size of a collection |
| `DateRange` | `min, max` in ISO 8601, optional date format | A date in the range |
| `Choices` | A list of values | One of the values at random |
| `LinkedChoices` | A list of values per column | Like `Choices`, but all `LinkedChoices` columns of an entity pick the same row. Useful for unique keys made of several columns |
| `ChoicesPercent` | `value:cumulativeFraction` lines, e.g. `RED:0.18`, `BLUE:0.30`, ..., `ORANGE:1.00` | Values with a given distribution. `NULL` gives a null |
| `RangePercent` | A template, then `start,end:cumulativeFraction` lines | Numbers from ranges with a given distribution |
| `FixedSet` | A list of values | The value at the position of the current element, so a collection gets exactly these values |
| `RandomString` | Optional length | A random alphanumeric string |
| `RandomSubset` | `fanout, start, end`, optional template | A collection of `fanout` distinct numbers from the range, optionally formatted with a `StringTemplate` |
| `StringTemplate` | A template | The template with its tokens replaced, see below |
| `BoundedSubType` | A subtype class name | Instances of that subtype or its subtypes |
| `SubTypeChoices` | Subtype class names | Instances of one of the listed types |
| `tools.xor.CounterGenerator` | `count`, optional `start` | Drives generation of `count` rows and sets the visitor context to the row number |
| `tools.xor.ToOneGenerator` | Start id, then `start,end:size` ranges | Parent ids for a to-one relationship, so that parents get a given number of children |
| `tools.xor.CollectionOwnerGenerator` | Total rows, then `start,end:size` ranges | Collection owners with a given collection size per id range |

Several more (`ModGenerator`, `QueryChoices`, `LocalizedString`, `DependencyReference`, `DependencySequence`,
`DependencyStringHash`, `ElementPositionGenerator`) are documented in their javadoc.

### StringTemplate tokens

| Token | Replaced with |
| --- | --- |
| `[VISITOR_CONTEXT]` | The current context value, e.g. the row number from a `CounterGenerator` |
| `[VISITOR_CONTEXT_0]` ... `[VISITOR_CONTEXT_2]` | Indexed context values |
| `[GENERATOR]` | `getCurrentValue` of the generator linked to this template |
| `[GLOBAL_SEQ]` | A global sequence from `Settings` |
| `[THREAD_NO]` | The current thread id |
| `[ENTITY_SIZE]` | The `EntitySize` name from `Settings` |

## Example: a task with 100 children, straight into the database

From `JPAMutableJsonTest.testChildrenMix`. The JDBC model's `TASK` table gets one root task and 100 child
tasks:

```java
Shape shape = amJDBC.getDataModel().getShape(JDBCDataModel.RELATIONAL_SHAPE);
if (shape == null) {
    shape = amJDBC.getDataModel().createShape(JDBCDataModel.RELATIONAL_SHAPE);
}

JDBCType task = (JDBCType) shape.getType("TASK");
task.clearGenerators();

// TASKPARENT_UUID = "ID_" + the parent id chosen by the ToOneGenerator below
ExtendedProperty taskparent = (ExtendedProperty) task.getProperty("TASKPARENT_UUID");
Generator parentgen = new StringTemplate(new String[] {"ID_[GENERATOR]"});
taskparent.setGenerator(parentgen);

// UUID and NAME derived from the row number
ExtendedProperty rootid = (ExtendedProperty) task.getProperty("UUID");
rootid.setGenerator(new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"}));
ExtendedProperty namep = (ExtendedProperty) task.getProperty("NAME");
namep.setGenerator(new StringTemplate(new String[] {"NAME_[VISITOR_CONTEXT]"}));

// Discriminator column
ExtendedProperty dtype = (ExtendedProperty) task.getProperty("DTYPE");
dtype.setGenerator(new DefaultGenerator(new String[] {"Task"}));

ToOneGenerator toonegen = new ToOneGenerator(new String[] {
    "0",
    "0,0:0",     // root task
    "1,100:100"  // root task with 100 children
});

// 101 rows: 1 root task and 100 child tasks
CounterGenerator gensettings = new CounterGenerator(101);
gensettings.addListener(toonegen);
task.addGenerator(gensettings);
gensettings.addVisit(new DefaultGenerator.GeneratorVisit(toonegen, (GeneratorRecipient) parentgen));

Settings settings = new Settings();
Transaction tx = amJDBC.createTransaction(settings);
tx.begin();
amJDBC.generateSameTX(shape.getName(), Arrays.asList("TASK"), settings);
```

The `CounterGenerator` drives the rows. For each row it notifies the `ToOneGenerator`, and the
`GeneratorVisit` hands the `ToOneGenerator` to the parent id template, whose `[GENERATOR]` token then
resolves to the parent's row number. `generate` loads the data on separate threads; `generateSameTX` uses
the calling thread and its JDBC connection, so a rollback also removes the generated data.

## Domain values from Excel

Random values often break business rules, and shared entities (the same owner on many tasks) should really
be shared. A domain values workbook assigns generators and their arguments per attribute:

```java
DataModel das = aggregateManager.getDataModel();
InputStream inputStream = Thread.currentThread().getContextClassLoader()
    .getResourceAsStream("DomainValues.xlsx");
das.initGenerators(inputStream);
```

`initGenerators` calls `ExcelExportImport.initGenerators`, which reads:

| Sheet | Content |
| --- | --- |
| `Domain types` | One row per entity type: column 1 the sheet name, column 2 the entity type name, optional column 3 the incoming relationship the generators apply to |
| One sheet per entity type | Row 1: attribute names. Row 2: the generator class name for each attribute. Following rows: the generator arguments, one per row, until the first empty cell |

`src/test/resources/DomainValues.xlsx` uses `tools.xor.generator.Range`, `Choices` and `DateRange` for
`tools.xor.db.pm.Task`. Generator classes are created with their `String[]` constructor, so a custom
generator works here too.

The easiest way to create the workbook is to [export](import-export.md) an aggregate, rename the
`Relationships` sheet to `Domain types` and adapt its columns, delete the sheets and columns that need no
domain values, and insert the generator row under each header.

## Collection sparseness

The size of generated collections controls how big an object graph gets. A collection gets a random size
of up to `EntitySize.size() * sparseness`, where `EntitySize` is `SMALL` (35), `MEDIUM` (350, the default),
`LARGE` (1000) or `XLARGE` (3500), and sparseness defaults to 1.0. It can be set globally and per
relationship:

```java
Settings settings = new Settings();
settings.setSparseness(0.2f);
settings.getCollectionSparseness().put("dependants", 0.1f);
```

A `Range` generator on a collection fixes its size range unless a sparseness is set for that path. A
`FixedSet` generator on an element property sets the size to the number of its values.

## Subtype selection

For relationships to an inheritance hierarchy the subtype is chosen at random. Restrict it with
`BoundedSubType` (one subtype and its descendants) or `SubTypeChoices` (a list of types) on the
relationship property.

## Generating an object graph

```java
DataModel das = aggregateManager.getDataModel();
Settings settings = das.settings().aggregate(Task.class).build();
settings.setEntitySize(EntitySize.LARGE);
settings.setSparseness(0.01f);

TypeGraph sg = settings.getView().getTypeGraph((EntityType) settings.getEntityType());
JSONObject task = (JSONObject) sg.generateObjectGraph(settings);

settings.setPostFlush(true);
aggregateManager.update(task, settings);
```

## Loading CSV files with the CSV loader

`tools.xor.service.exim.CSVLoader` loads CSV files straight into tables over JDBC, resolving foreign keys and
generating missing columns. Each file has three sections:

1. A header line with the CSV column names.
2. One line of JSON describing the table.
3. The data lines.

From `src/test/resources/csvloader/test1/Task.csv`:

```text
name, displayname, description,ownedBy,dtype,createdon
{ "entityName":"tools.xor.db.pm.Task", "tableName":"Task", "dateFormat":"yyyy-MM-dd HH:mm:ss", "columns":["UUID", "DTYPE", "name", "displayName", "description","createdon"], "keys" : ["name"], "foreignKeys" : [ { "foreignKey" : "ownedBy_UUID", "foreignKeyTable" : "Person", "select" : "uuid", "join" : [{"name":"ownedBy"}]} ]}
TASK_1,Task 1, Task 1,johnd,Task,2019-11-31 23:59:59
```

Here `ownedBy_UUID` is filled with `SELECT uuid FROM Person WHERE name = <ownedBy value>`.

| Schema key | Meaning |
| --- | --- |
| `tableName` | Required. The table to load |
| `entityName` | The Java entity name |
| `columns` | The table columns that get values |
| `columnAliases` | Maps a table column to a CSV field when the names differ |
| `dateFormat` | Format of date columns |
| `keys` | Columns that identify a row, needed to update nullable foreign keys later |
| `foreignKeys` | Columns resolved with `foreignKeyTable`, `select` and `join` |
| `columnGenerators` | Generators for columns that are not in the file: `column` plus `className` and `arguments`, or `beanName` |
| `entityGenerator` | Generates all rows of a table that has no data file, e.g. with `tools.xor.QueryGenerator` |
| `dependsOn` | Tables to load first when foreign keys do not imply the order |
| `springConfigs` | Spring XML files (`locationPattern`) that define generator beans referenced by `beanName` |

Loading happens in passes: rows are inserted with their required foreign keys, in an order derived from the
foreign keys, and then nullable foreign keys are updated. From `CSVLoaderTest.test1`:

```java
Shape shape = amJDBC.getDataModel().getShape();
CSVLoader csvLoader = new CSVLoader(shape, "csvloader/test1/");

// The files have no UUID column, so generate the primary keys
for (CSVState state : csvLoader.getGraph().getVertices()) {
    EntityType entityType = (EntityType) state.getType();
    entityType.getProperty("UUID").setGenerator(new StringTemplate(new String[] {"ID_[VISITOR_CONTEXT]"}));
}

amJDBC.configure(null);
JDBCDataStore dataStore = (JDBCDataStore) amJDBC.getDataStore();
csvLoader.importData(new Settings(), dataStore);
```

`importDataParallel(Settings, DataModelFactory, int numThreads)` loads with several threads. A schema-only
file (`*.schema`) with generators can produce data without a CSV file; `csvloader/test4/Task.schema` uses
generator beans from `spring-task-generators.xml`:

```json
{ "entityName":"tools.xor.db.pm.Task", "tableName":"Task", "dateFormat":"yyyy-MM-dd HH:mm:ss", "keys":[ "name" ],
  "entityGenerator":{ "beanName" : "cowner", "visits" : { "idtemplate" : "celement", "nametemplate" : "celement", "parentidtemplate" : "cowner" } },
  "columnGenerators":[ { "column":"UUID", "beanName" : "idtemplate" }, { "column":"TASKPARENT_UUID", "beanName" : "parentidtemplate" },
                       { "column":"NAME", "beanName" : "nametemplate" }, { "column":"DTYPE", "className": "tools.xor.generator.StringTemplate", "arguments": ["Task"]} ],
  "springConfigs": [{"locationPattern" : "classpath*:spring-task-generators.xml"}] }
```

The full schema description is in the javadoc of `CSVLoader`, and the JSON schema that validates it is
`src/main/resources/CSVLoaderSchema.json`.

## Writing a custom generator

Extend `DefaultGenerator`, keep a public constructor that takes `String[]` (the Excel domain values and the
CSV loader create generators by reflection with it), and override the methods for the types you produce:

```java
package com.example.generator;

import tools.xor.Property;
import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

/**
 * Generates e-mail addresses such as user42@example.com.
 * The optional first argument is the domain name.
 */
public class EmailGenerator extends DefaultGenerator {

    private int counter;

    // Required: generators are created by reflection with a String[] argument
    public EmailGenerator(String[] arguments) {
        super(arguments);
    }

    @Override
    public String getStringValue(Property property, StateGraph.ObjectGenerationVisitor visitor) {
        String domain = getValues().length > 0 ? getValues()[0] : "example.com";

        // When a CounterGenerator drives the generation, the visitor context is the current row number
        Object context = visitor == null ? null : visitor.getContext();
        String local = "user" + (context != null ? context : counter++);

        return local + "@" + domain;
    }
}
```

Register it in any of these ways:

* **In code**, on a domain or JDBC property:

  ```java
  EntityType personType = (EntityType) das.getShape().getType(Person.class);
  ExtendedProperty email = (ExtendedProperty) personType.getProperty("email");
  email.setGenerator(new EmailGenerator(new String[] {"example.org"}));
  ```

* **In a domain values workbook**: put `com.example.generator.EmailGenerator` in the generator row of the
  `email` column and `example.org` below it.
* **In a CSV loader schema**:

  ```json
  "columnGenerators": [
      { "column": "EMAIL", "className": "com.example.generator.EmailGenerator", "arguments": ["example.org"] }
  ]
  ```

* **As a Spring bean** in a file listed under `springConfigs`, referenced with `"beanName"`.

Override `validate(ExtendedProperty)` to reject properties the generator cannot serve, and implement
`GeneratorRecipient` if the generator should receive another generator through a `GeneratorVisit`, as
`StringTemplate` does.

Previous: [Operations](operations.md) | Next: [Import and export](import-export.md)
