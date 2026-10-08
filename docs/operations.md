# Operations

Every operation is a method on `AggregateManager` that takes the input object and a `Settings` object. The
view in the settings sets the scope: which attributes and relationships the operation reads or writes.

## Summary

| Method | Input | Result | Description |
| --- | --- | --- | --- |
| `create(Object, Settings)` | External or domain object | Persistent entity | Creates the entity and every dependent object in the view. Dependent objects are created, not looked up |
| `update(Object, Settings)` | External or domain object | Persistent entity | Loads the entity by id or natural key and applies the values in the view. Creates it if it does not exist |
| `read(Object, Settings)` | Entity, or an object with its id or natural key | External object | Loads the entity and returns the part selected by the view |
| `query(Object, Settings)` | Optional object with an id | `List` of external objects | Runs the view's queries. Without an id, returns all entities of the type that match the filters |
| `delete(Object, Settings)` | Persistent entity | none | Deletes the entity and its dependent objects |
| `clone(Object, Settings)` | Persistent entity | Persistent entity | Deep copy within the scope of the view |
| `patch(List, List, Settings)` | Objects and optional snapshots | Persistent entities | Bulk first-level field updates without walking the object graph |
| `toDomain(Object, Settings)` | External object | Domain object | Conversion only, no database access |
| `toExternal(Object, Settings)` | Domain object | External object | Conversion only, no database access |
| `migrate(AggregateManager, Settings)` | Source `AggregateManager` | none | Copies the entities listed under `entities.to.migrate` in `xor.properties` from another database |
| `dml(Settings)` | none | List or count | Runs an INSERT, UPDATE, DELETE or SELECT statement |

The type of the result objects depends on the [TypeMapper](architecture.md#typemapper). With the default
mapper they are domain classes, with a JSON mapper they are `JSONObject`s.

The examples below use the JSON configuration from [Getting started](getting-started.md#working-with-json)
and come from `JPAMutableJsonTest` and `DefaultMutableJson`.

## Reading data

### A list of entities

Set the entity type and a view, and pass `null` as the input object. From `JPAMutableJsonTest.queryEntity`:

```java
Settings settings = new Settings();
settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
settings.setView(aggregateService.getView("TASKCHILDREN"));
List<?> result = aggregateService.query(null, settings);
```

Each element is a `JSONObject` in the shape of `TASKCHILDREN`, for example:

```json
{
   "id": "ff8080815b683aa9015b683ab5b20000",
   "name": "SETUP_DSL",
   "displayName": "Setup DSL",
   "description": "Setup high-speed broadband internet using DSL technology",
   "detailedDescription": "",
   "taskChildren": [
      {
         "id": "ff8080815b683aa9015b683ab5bf0001",
         "name": "TASK_1",
         "displayName": "Task 1",
         "description": "This is the first child task"
      }
   ]
}
```

### A single entity

`read` takes the entity, or an object that carries its id. With JSON input, add the type under
`XOR.type` (`Constants.XOR.TYPE`). From `JPAMutableJsonTest.updateSingleField`:

```java
JSONObject json = new JSONObject();
json.put("id", task.getId());
json.put(Constants.XOR.TYPE, Task.class.getName());
json = (JSONObject) aggregateService.read(json, settings);
```

An entity can also be looked up by its natural key. From `DefaultQueryOperation`:

```java
MetaEntityState state = new MetaEntityState();
state.setName(MetaEntityStateEnum.ACTIVE.name());
state = (MetaEntityState) aggregateService.read(state, new Settings());
```

## Modifying data

`create`, `update` and `delete` accept the external model. A domain object can be converted to it first
with `toExternal`.

> **Note**
> With a JSON type mapper, map-valued attributes are returned as JSON objects keyed by the string form of
> the map key. Writing maps from JSON with `create` or `update` is not supported yet.

### Create an aggregate

A complex aggregate is created with one call, children included:

```java
JSONObject json = new JSONObject();
json.put("name", "SETUP_DSL");
json.put("displayName", "Setup DSL");
json.put("description", "Setup high-speed broadband internet using DSL technology");

Settings settings = new Settings();
settings.setEntityClass(Task.class);
Task task = (Task) aggregateService.create(json, settings);
```

Save order between aggregates is computed from the model. See
[Architecture](architecture.md#saving-in-dependency-order).

### Update a single attribute

A view with one attribute updates only that attribute. From `JPAMutableJsonTest.updateSingleField`:

```java
List<String> attributes = new ArrayList<>();
attributes.add("description");
AggregateView view = new AggregateView("DESC");
view.setAttributeList(attributes);

Settings settings = new Settings();
settings.setView(view);
settings.setEntityClass(Task.class);

JSONObject json = new JSONObject();
json.put("id", task.getId());
json.put("description", "New description");

aggregateService.update(json, settings);
```

### Point an association at another entity

Reference the association in the view and identify the new target by id or natural key. Here the natural key
of `Task` is `name`, and `auditTask` is changed to the task named `AUDITNEW`. From
`JPAMutableJsonTest`:

```json
{
   "id": "ff8080815b6917cb015b6917dbba0000",
   "auditTask": {
      "name": "AUDITNEW"
   }
}
```

```java
List<String> paths = new ArrayList<>();
paths.add("auditTask");
AggregateView refView = new AggregateView("REFERENCE_UPDATE");
refView.setAttributeList(paths);

Settings settings = new Settings();
settings.setView(refView);
settings.setEntityClass(Task.class);

Task updatedTask = (Task) aggregateManager.update(json, settings);
```

The test declares the natural key with `EntityType.setNaturalKey(new String[] {"name"})` on both the domain
and the external type.

### Delete

From `JPAMappedByTest.deleteRequired`:

```java
DataModel das = aggregateManager.getDataModel();
Type deptType = das.getShape().getType(Department.class);

Settings settings = new Settings();
settings.setPostFlush(true);
settings.setEntityType(deptType);
settings.expand(new AssociationSetting(Head.class));
settings.init(das.getShape());

d = (Department) aggregateManager.create(d, settings);
aggregateManager.delete(d, settings);
```

`setPostFlush(true)` flushes the session after the operation. `setPreFlush(true)` flushes before it, which
is useful before native queries.

## Settings at a glance

| Method | Purpose |
| --- | --- |
| `setView(View)` | Scope of the operation |
| `setEntityClass(Class)`, `setEntityType(Type)` | The entity type, when it cannot be inferred from the input |
| `expand(AssociationSetting)` | Add associations to the scope |
| `setParam(String, Object)`, `setParams(Map)` | Values for view filters and query parameters |
| `setOffset(Integer)`, `setLimit(Integer)`, `setNextToken(Map)` | Paging and scrolling |
| `setPreFlush(boolean)`, `setPostFlush(boolean)` | Flush the session before or after the operation |

`DataModel.settings()` returns a builder for common cases, for example
`das.settings().aggregate(Task.class).build()`.

Previous: [Custom queries and tuning](query-tuning.md) | Next: [Data generation](data-generation.md)
