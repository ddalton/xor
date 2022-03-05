---
layout: docs
title:  Code Samples
permalink: /docs/operations/
---

XOR allows efficient data querying and update of entities.
The supported operations are:

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Operation</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>Create</code></p></td>
      <td><p>

          Used to create a new entity. All dependent objects of this entity will be created and not fetched from the database. 
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Update</code></p></td>
      <td><p>

          Updates or creates a new entity. Always checks the database if an object with the given id/key is present and if so loads that entity and updates it.
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Merge</code></p></td>
      <td><p>

          Same behavior as update operation. The only difference is with collection handling. Obsolete entries will be deleted in the update operation, whereas it will not be removed in the merge operation.
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Clone</code></p></td>
      <td><p>

          Makes a deep copy of a persistent entity. The copy scope can be defined by a XOR view specification.
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Read</code></p></td>
      <td><p>

          Reads an entity according to an XOR view specification. The read requires a context such as the root entity id/key based off which the read needs to be performed.
          A variant of the read API is the query API which does not need this context.

          The read operation returns a non-persistent managed object, i.e., an object of an external type such as JSON.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Query</code></p></td>
      <td><p>

          Retrieves multiple entities according to an XOR view specification. 
          The difference with the read operation is that the query API does not need context. It reads just based on the entity type.


      </p></td>
    </tr>
    <tr>
      <td><p><code>Delete</code></p></td>
      <td><p>

          Delete an entity and all its dependant objects.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Load</code></p></td>
      <td><p>

          Load a persistent entity from the database. The object returned is a persistent managed entity.

      </p></td>
    </tr>
  </tbody>
</table>
</div>

Additionally the following two operations are provided that allows object conversion between the External and Domain models.
There is no database access during these operations.


<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Operation</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>toDomain</code></p></td>
      <td><p>

          Takes an object from the External model and converts it to a corresponding object in the Domain model.
          The values based on the desired view scope are populated in the domain model object. 

      </p></td>
    </tr>
    <tr>
      <td><p><code>toExternal</code></p></td>
      <td><p>

          Takes an object from the Domain model and converts it to a corresponding object in the External model.
          The values based on the desired view scope are populated in the External model object. 


      </p></td>
    </tr>
  </tbody>
</table>
</div>

Described below are some examples that showcase the usage of the above operations.


# Data Retrieval

The read operation is used to retrieve data from the database and return in the External model format. So if the MutableJsonTypeMapper is configured then the output will be in the JSON format.

## Entity collections

The following code snippet shows how to retrieve a list of objects of a certain type using the query API.
The example also shows how the object size can be constrained to only the fields in a given view.

### API

```java
// Provide the Type and View 
settings.setEntityType(aggregateService.getDAS().getType(Task.class));
settings.setView(aggregateService.getView("TASKCHILDREN"));

// Returns a list of JSONObject instances
List<?> result = aggregateService.query(null, settings);

// Contained objects are not included
assert(result.size() == 1);
```

### Response

```json
{
   "detailedDescription":"",
   "displayName":"Setup DSL",
   "XOR.id":"1066e5e6-dd97-4b6d-820e-8d431e5401ae",
   "taskChildren":[
      {
         "detailedDescription":"",
         "displayName":"Task 1",
         "XOR.id":"cb651ed9-efea-42bf-b7da-6490b9fee2ed",
         "name":"TASK_1",
         "description":"This is the first child task",
         "id":"ff8080815b683aa9015b683ab5bf0001",
      }
   ],
   "name":"SETUP_DSL",
   "description":"Setup high-speed broadband internet using DSL technology",
   "id":"ff8080815b683aa9015b683ab5b20000",
}
```

## Entity by ID

The following example shows how to retrieve a single entity of a particular type by its id.

### API

```java
// Create the settings object
DataAccessService das = aggregateService.getDAS();
EntityType taskType = (EntityType)das.getType(Task.class);
Settings settings = new Settings();
settings.setView(aggregateService.getView("TASKCHILDREN"));

// Set the id on the query object
Task queryTask = new Task();
queryTask.setId(task.getId());

// Perform the read and extract the response
json = (JSONObject) aggregateManager.read(json, settings);
```

### Response

```json
{
   "XOR.type":"tools.xor.db.pm.Task",
   "quote":{
      "XOR.type":"tools.xor.db.pm.Quote",
      "price":99.99,
      "XOR.id":"d5e8203f-8f4c-4701-8ade-76456db6c9ec",
      "id":"ff8080815b6849f6015b684a01e00003",
      "version":0
   },
   "displayName":"Setup DSL",
   "XOR.id":"013f0399-a5e7-4461-becd-1e083d60f1d6",
   "taskChildren":[
      {
         "XOR.type":"tools.xor.db.pm.Task",
         "displayName":"First child",
         "XOR.id":"2242286b-5e40-4bc3-b924-078995894715",
         "name":"CHILD1",
         "description":"This is the first child of the task",
         "id":"ff8080815b6849f6015b684a01df0001",
         "version":0
      },
      {
         "XOR.type":"tools.xor.db.pm.Task",
         "displayName":"Second child",
         "XOR.id":"4a46b899-ff22-4965-b2ab-0969c805e9bb",
         "name":"CHILD2",
         "description":"This is the second child of the task",
         "id":"ff8080815b6849f6015b684a01e00002",
         "version":0
      }
   ],
   "name":"ROOT",
   "description":"Setup high-speed broadband internet using DSL technology",
   "id":"ff8080815b6849f6015b684a01d50000",
   "version":0,
   "XOR.type:taskChildren":"java.util.HashSet"
}
```  

Next, we shall look at some examples of data modification operations.

# Data Modification

The operations that modify data are create, update, merge and delete. They take input in the form of an external model and perform the operation. If the input is in the domain model form, then it can be converted to external form using the toExternal operation.

## Creating an Aggregate

One of the main benefits of using XOR is that a complex aggregate can be created by a single create operation.
Below is a example input payload representing the Task Aggregate.

### Input data

```json
{
   "displayName":"Setup DSL",
   "name":"SETUP_DSL",
   "description":"Setup high-speed broadband internet using DSL technology"
}
```

The java code snippet that creates this entity is:

```java
Settings settings = getSettings();
settings.setView(view);
settings.setEntityClass(Task.class);
Task task = (Task) aggregateService.create(inputJson, settings);
```

## Update a value

The XOR architecture allows to optimally change the value of a single entity. Just have a view that refers to that property only.

### Input data

```json
{
   "id":"iff8080815b686a14015b686ba6cc0000",
   "description":"Setup high-speed broadband internet using DSL technology"
}
```
Code snippet

```java
// Create a view with only the attribute field that needs to be updated
List<String> attributes = new ArrayList();
attributes.add("description");
AggregateView view = new AggregateView("DESC");
view.setAttributeList(attributes);
settings.setView(view);
settings.setEntityClass(Task.class);

aggregateService.update(inputJson, settings);
```

## Update a single valued association

Associations between entities can be updated optimally also. Just reference the name of the attribute or the attribute path in the view and provide the necessary information to identify the new value of the association.

In the example below the Task entity has a single valued association with another Task entity under the attribute name "auditTask". We would like to change the value of this auditTask field to make it to point to another auditTask instance. The natural key for the Task entity is the "name" field.

The way to do this is to refer to the key/id of the new auditTask and refer this attribute name in the view specification.

### Input data

```json
{
   "id":"ff8080815b6917cb015b6917dbba0000",
   "auditTask":{
      "name":"AUDITNEW"
   }
}
```

Code snippet

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
