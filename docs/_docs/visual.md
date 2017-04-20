---
layout: docs
title:  Data visualization
permalink: /docs/visual/
---

XOR allows you to generate a graph visual of both the Aggregate Type and object graphs.

## Type or State graph
This information is useful in understanding all the types involved in an aggregate rooted at a particular type. An aggregate represents all the composition relationships between the various types.

![](/img/stategraph.png)

<div class="note">
  <h5>Notice the number before the state name</h5>
  <p>The number represents the topological order between the different states. This information is useful in correctly saving/deleting an object comprised of more than one aggregate.</p>
</div>

The code needed to produce the above graph is:

```java
DataAccessService das = aggregateManager.getDAS();
Settings settings = das.settings().aggregate(Task.class).build();
TypeGraph sg = settings.getView().getTypeGraph((EntityType)settings.getEntityType());
settings.setGraphFileName("TaskStateGraph.png");
sg.generateVisual(settings);
```

## Object graph
This information is useful to get how the object is connected with other objects it contains. It gives an idea of the sparseness of a graph.
Below is an example object graph of a Task object generated using XOR's data generation framework.

![](/img/objectgraph.png)

The code used to generate the graph is the following:

```java
DataAccessService das = aggregateManager.getDAS();
Settings settings = das.settings().aggregate(Task.class).build();
settings.setEntitySize(EntitySize.LARGE);
TypeGraph sg = settings.getView().getTypeGraph((EntityType)settings.getEntityType());
settings.setSparseness(0.01f);
JSONObject task = (JSONObject)sg.generateObjectGraph(settings);

// Try and persist this now
settings.setGraphFileName("TaskGraph.png");
settings.setPostFlush(true);
aggregateManager.create(task, settings);
```
