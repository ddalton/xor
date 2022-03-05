---
layout: docs
title: Architecture
permalink: /docs/architecture/
---

XOR is a layer that sits between the application code and the ORM.
The main motivation in creating XOR was the difficulty in solving performance issues with an ORM.
<div class="note info">
  <h5>Incrementally add XOR functionality into your code</h5>
  <p>XOR can be very useful to target a poorly performing piece of ORM code. The more complex your data model is, the more benefits you will achieve using XOR.</p>
</div>

Since the ORM hides a lot of the generated SQL from the developer, it becomes very difficult to fine tune a badly performing query. With XOR, this information can be explicit defined, allowing a DBA to fine tune it further if necessary.

The diagram below shows where XOR fits in the code stack.

![](/img/XORarch.png)

XOR provides many operations to work on entity objects. The scope of the operation is defined by the view.
The default scope is the aggregate scope. Scope is defined in terms of relationships.

## OO relationships and aggregate

When working with a complex domain model, there are combination of the following relationships:
* Inheritance
* Association
* Aggregation
* Composition

The Java language has constructs to explicitly model Inheritance and Association relationships.
Aggregation and Composition relationships need to be modelled in the business logic of the application.

ORM does help in this regard and in JPA, composition relationships are captured using the "cascade"
attribute on a relationship. 

### Aggregate
XOR uses this composition relationship to define the scope of an Aggregate.
So when a view for a given EntityType is obtained, it refers to the Aggregate scope. The aggregate scope includes all inheritance and composition relationships. One can inspect the aggregate scope by generating the StateGraph for an entity.

## Scopes

XOR is based on the concept of scope. A scope is formally defined using the StateGraph.
The implementation of a scope is a view.

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Scope</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>Basic</code></p></td>
      <td><p>

          Returns all the simple properties of the entity. No relationships are followed.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Aggregate</code></p></td>
      <td><p>

          Returns the entity and all its dependent objects, i.e., the objects that will be deleted with the root entity is deleted.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Custom</code></p></td>
      <td><p>

          A slice of the aggregate and also any additional relationships can be explicitly specified. This the scope can either be larger or smaller than the aggregate scope.

      </p></td>
    </tr>
  </tbody>
</table>
</div>

Views are generated automatically for both the Basic and Aggregate scopes for an entity.

## Data transformation

The key architecture principle by which XOR achieves is goals of simplicity is with the concept of interacting with an External and Domain model.

The Domain model represents the persistence model that has knowledge of the entities and the relationships between them. This is also the code where the business logic is embedded.

The External model represents the model by which clients interact with the application. The clients could either be applications working with REST API or the UI code. These clients do not need access to the domain model classes and only need the data. This data is typically captured using various models such as:
* Value object model
* Data Transfer Object model
* JSON model

XOR supports all of the above external models. It supports a domain model based on JPA and Ariba AML.

There is a distinct responsibility held by each of these 2 models. The domain model is tasked with processing the business logic and also managing the interaction with the ORM. 

The external model does not need to deal with persistence and also does not need to have knowledge or be dependent on any libraries used by the persistence model. This way any ORM specific issues do not leak into the UI or the client code.

The diagram below gives a high level overview of how XOR processes a create/update operation. 

![](/img/transform.png)

This architecture promotes loose coupling between the persistence and UI/external interface.

All the persistence operations such as create, update, read etc support data transformation between the external and the persistence models.
This transformation is implemented using the TypeMapper interface. XOR comes built-in with a few TypeMappers.
Users can add a custom TypeMapper implementation if none of the built-in mappers are sufficient.

## Built-in TypeMappers

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>TypeMapper name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>DTOTypeMapper</code></p></td>
      <td><p>

          Represents the TypeMapper for value object model that ends with the suffix "VO".
          For example if the domain class name is Address then the VO model class name
          is AddressVO.

      </p></td>
    </tr>
    <tr>
      <td><p><code>DefaultTypeMapper</code></p></td>
      <td><p>

        This TypeMapper uses the domain model Java classes to copy information from
        and to the Domain model classes. This is typically used if one does not already
        have a value object/DTO model.

      </p></td>
    </tr>
    <tr>
      <td><p><code>MutableJsonTypeMapper</code></p></td>
      <td><p>

        The external model is dynamically built using a mutable JSON implementation.
        The JSON implementation that is used is org.json.JSONObject.

      </p></td>
    </tr>
    <tr>
      <td><p><code>ExcelJsonCreationStrategy</code></p></td>
      <td><p>

        This is a further refinement of the MutableJsonTypeMapper and adds some
        meta information in each JSON object such as the object type information and
        the XOR identifier. This is useful to resolve cyclic references.

      </p></td>
    </tr>
    <tr>
      <td><p><code>ImmutableJsonTypeMapper</code></p></td>
      <td><p>

        This is another JSON TypeMapper that uses the javax.json.JsonObject implementation.
        This is not as flexible as the MutableJsonTypeMapper, but is available if such an
        implementation is desired.

      </p></td>
    </tr>
  </tbody>
</table>
</div>

### Benefits of the dual model approach

This dual model architecture is especially useful to solve common issues typically faced with ORM:
* Domain class dependencies<br/>
The domain objects typically have a lot of business logic that hard code dependencies to libraries.
This can be a problem during serialization/de-serialization of the domain objects. For this reason, an external model is desired.
If there is no explicit external model defined, then a JSON mapping can be utilized by using the MutableJsonTypeMapper provider.
* Lazy initialization exception<br/>
This happens when a lazy attribute is accessed after the session is closed. This can be avoided by explicitly specifying the needed pieces of the information in an XOR view beforehand.
The view will then ensure that only the parts that will be accessed after the session is closed, needs to be initialized. The added side-effect is that it improves performance, as not
all the attributes of the domain object need to be initialized.
* Quickly provision a REST api<br/>
Using the JSON type mapper provider, a REST API can quickly be developed over the domain model.
This helps to speed up prototyping of the domain model.


## Simplifying Operations

XOR does a topological sort on the entities and the relationships between them. The reason this is important is that this helps to automate the order in which entities need to be saved or deleted. The user does not have to hardcode the order and this makes the code more robust to changes.

Consider the example below depicting the relationship between two aggregates. The head attribute in Department is a required field. This means that the department object cannot be saved unless the head object is saved first.

![](/img/required.png)

In JPA the following code is necessary to save the Department object sucessfully without throwing an exception:

```java
@PersistenceContext
EntityManager entityManager;

Head h = new Head();
h.setName("Isaac Newton");
entityManager.persist(h);

Department d = new Department();
d.setName("Mathematics");
d.setHead(h);
entityManager.persist(d);
```

If there are large number of such relationships, then this order of persist operations need to be hardcoded. This also makes the code brittle and prone to needing more maintenance.

Using XOR, this can be simplified with just one operation:

```java
Head h = new Head();
h.setName("Isaac Newton");

Department d = new Department();
d.setName("Mathematics");
d.setHead(h);

DataAccessService das = aggregateManager.getDAS();
Settings settings = das.settings().base(Department.class)
	.expand(new AssociationSetting(Head.class))
	.build();

aggregateManager.create(d, settings);
```
