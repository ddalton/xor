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

The diagram below shows where ORM fits in the code stack.

![](/img/XORarch.png)

XOR provides many operations to work on entity objects. The scope of the operation is defined by the view.
The default scope is the aggregate scope. Scope is defined in terms of relationships.

## OO relationships

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
