---
layout: docs
title:  Data visualization
permalink: /docs/visual/
---

XOR allows you to generate a graph visual of both the Aggregate Type and object graphs.

## Type or State graph
This information is useful in understanding all the types involved in an aggregate rooted at a particular type. An aggregate represents all the composition relationships between the various types.

![](/img/stategraph.png)

The code needed to produce the above graph is:

```
Settings settings = new Settings();
DataAccessService das = aggregateManager.getDAS();
EntityType taskType = (EntityType)das.getType(Task.class);

settings.setEntityType(taskType);
settings.init(das.getShape());
StateGraph sg = settings.getView().getStateGraph(taskType);
settings.setGraphFileName("TaskStateGraph.png");
sg.generateVisual(settings);
```

## Object graph
This information is useful to get how the object is connected with other objects it contains. It gives an idea of the sparseness of a graph.
Below is an example object graph of a Task object generated using XOR's data generation framework.

![](/img/objectgraph.png)

The code used to generate the graph is the following:

```
DataAccessService das = aggregateManager.getDAS();

EntityType taskType = (EntityType)das.getType(Task.class);
AggregateView view = das.getView(taskType).copy();
Settings settings = new Settings();
settings.setView(view);
settings.setEntityType(taskType);
settings.setEntitySize(EntitySize.LARGE);

settings.init(das.getShape());
StateGraph sg = settings.getView().getStateGraph(taskType);
settings.setSparseness(0.01f);
JSONObject task = (JSONObject)sg.generateObjectGraph(settings);

// Try and persist this now
settings.setGraphFileName("TaskGraph.png");
settings.setPostFlush(true);
aggregateManager.create(task, settings);
```
