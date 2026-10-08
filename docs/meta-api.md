# Meta API

The Meta API answers questions about the model at runtime: which types and views exist, what a view
contains, and what an aggregate consists of. It is useful for tooling, for generic UIs, and as a starting
point when writing views.

Get it from the `AggregateManager`:

```java
MetaModel mm = aggregateManager.getMetaModel();
```

| Method | Returns |
| --- | --- |
| `getViewList()` | Names of all registered views, sorted |
| `getTypeList()` | Names of all types in the shape, sorted |
| `getEntityNames()` | Names of the entity types only |
| `getEntityProperties(String entityName)` | Property names of an entity type |
| `getExpandedNaturalKey(String entityName)` | The natural key attributes of an entity type |
| `getViewAttributes(String viewName)` | The attribute paths of a view, after view references are expanded |
| `getAggregateAttributes(String entityName)` | The attributes of the aggregate rooted at the entity, in the compact regular expression form that [views](views.md#regular-expression-attributes) accept |

From `DefaultAggregatePaths`:

```java
MetaModel mm = aggregateManager.getMetaModel();

List<String> viewList = mm.getViewList();
List<String> attrs = mm.getViewAttributes("TASKCHILDREN");
List<String> aggregate = mm.getAggregateAttributes(Task.class.getName());
```

Previous: [Import and export](import-export.md) | Next: [Visualization](visualization.md)
