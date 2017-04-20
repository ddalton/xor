---
layout: docs
title: Views
permalink: /docs/view/
---

XOR simplifies the access to the data using the concept of a view.

## Built-in views
XOR generates views for the Basic and Aggregate scopes of all entities.

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>API name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>getBaseView</code></p></td>
      <td><p>

          Returns all the simple attribute values for the entity, i.e., the association relationships are not traversed.

      </p></td>
    </tr>
    <tr>
      <td><p><code>getView</code></p></td>
      <td><p>

          Returns all the values in the aggregate. The composition relationships are traversed with this API.
          The association relationships are not traversed. These relationships have to be explicitly requested using the
          AssociationSetting API.

      </p></td>
    </tr>
  </tbody>
</table>
</div>

The code to retrieve the built-in view for the Basic scope is:

```java
DataAccessService das = aggregateService.getDAS();
EntityType personType = (EntityType) das.getType(Person.class);
AggregateView view = das.getBaseView(personType);
```

The code to retrieve the built-in view for the Aggregate scope is:

```java
DataAccessService das = aggregateService.getDAS();
EntityType personType = (EntityType) das.getType(Person.class);
AggregateView view = das.getView(personType);
```

## Custom views

A custom view can either restrict/enhance the scope of an entity.

### Scope restriction

A restricted view is a list of attributes that represents a subset of the entity.

The diagram below shows an example of a One-to-One relationship. When the task object
is accessed, the TaskDetails object might be also accessed depending on the fetch policy.
Most often it is lazing accessed, leading to an extra SQL.

![](/img/onetoone.png)

One can avoid this by explicitly requesting information only on the desired fields. Let us assume that the comments field is very large and we want to skip loading just this field. We can achieve this in XOR by creating a view with the following fields:

```
id
name
description
taskDetails.id
```

This is the simplest explanation for a view. This concept applies to both read and update operations.
For update, only the fields in the view get updated, the rest of the fields are unchanged.

By default XOR custom views are defined in a file called `AggregateViews.xml` located anywhere in the application classpath.
Views can also be defined programmatically.
Below is a definition of the view we had just seen earlier.

```xml
<AggregateViews>
    <aggregateView>
        <name>TASKDETAILSID</name>
        <attributeList>id</attributeList>
        <attributeList>name</attributeList>
        <attributeList>description</attributeList>
        <attributeList>taskDetails.id</attributeList>
    </aggregateView>
</AggregateViews>
```

### Regular Expressions
Attributes can also be specified using Regular Expression syntax. This makes for a concise way of representing complex aggregate slices.

In the example below, two equivalent views are depicted. The first uses the direct syntax and the second uses RegEx syntax. 
In this view we want to retrieve some information about the task and its first level children and dependant tasks.


![](/img/taskchildren.png)

Expressed using object notation

```xml
<AggregateViews>
    <aggregateView>
        <name>TASKANDCHILDREN</name>
        <attributeList>id</attributeList>
        <attributeList>name</attributeList>
        <attributeList>description</attributeList>
        <attributeList>version</attributeList>
        <attributeList>displayName</attributeList>
        <attributeList>detailedDescription</attributeList>
        <attributeList>taskChildren.id</attributeList>
        <attributeList>taskChildren.name</attributeList>
        <attributeList>taskChildren.description</attributeList>
        <attributeList>taskChildren.version</attributeList>
        <attributeList>taskChildren.displayName</attributeList>
        <attributeList>taskChildren.detailedDescription</attributeList>
        <attributeList>dependants.id</attributeList>
        <attributeList>dependants.name</attributeList>
        <attributeList>dependants.description</attributeList>
        <attributeList>dependants.version</attributeList>
        <attributeList>dependants.displayName</attributeList>
        <attributeList>dependants.detailedDescription</attributeList>
    </aggregateView>
</AggregateViews>
```

The same example, but the object notation is represented concisely using RegEx

```xml
<AggregateViews>
    <aggregateView>
        <name>TASKANDCHILDREN</name>
        <attributeList>(taskChildren.|dependants.){0,1}(description|id|version|displayName|detailedDescription|name)</attributeList>
    </aggregateView>
</AggregateViews>
```

The reason this mechanism is helpful is because the Meta API depicts the contents of an aggregate using RegEx. This information can then be used to build the views containing the desired information.

### Scope extension

The scope of a view can also be extended either programmatically or directly using object notation

#### Extending using Domain class

An aggregate can be extended to include all relationships from the types comprising the Aggregate entity to another entity of a particular domain class.
In the example below the Task aggregate is extended to also include the Person aggregate in its scope.

Note, the extension only occurs if there is atleast one relationship in the Task aggregate that refers to a Person type, otherwise there is no change in scope.
All relationships are automatically added.

```java
DataAccessService das = aggregateManager.getDAS();
Settings settings = das.settings().base(Task.class)
	.expand(new AssociationSetting(Person.class))
	.build();
```

#### Extending using relationship name

Alternatively, a scope can be extended in a more fine-grained way by specifying the desired relationships that need to be included in the view.

```java
settings.expand( new AssociationSetting("assignedTo"));	
```

In this example if assignedTo refers to a Person type, then we say here that we only want the assignedTo relationship and not any other relationships that refer to the Person type.
