---
layout: docs
title: Views
permalink: /docs/view/
---

XOR simplifies the access to the data using the concept of a view.

## What is an XOR view?
A view is a list of attributes that represents a subset of the entity.

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

## How to define a view?
By default XOR views are defined in a file called `AggregateViews.xml` located anywhere in the application classpath.
Views can also be defined programmatically.
Below is a definition of the view we had just seen earlier.

```
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

## Regular Expressions
Attributes can also be specified using Regular Expression syntax. This makes for a concise way of representing complex aggregate slices.

In the example below, two equivalent views are depicted. The first uses the direct syntax and the second uses RegEx syntax. 
In this view we want to retrieve some information about the task and its first level children and dependant tasks.


![](/img/taskchildren.png)


### Direct representation

```
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

### RegEx representation

```
<AggregateViews>
    <aggregateView>
        <name>TASKANDCHILDREN</name>
        <attributeList>(taskChildren.|dependants.){0,1}(description|id|version|displayName|detailedDescription|name)</attributeList>
    </aggregateView>
</AggregateViews>
```

The reason this mechanism is helpful is because the Meta API depicts the contents of an aggregate using RegEx. This information can then be used to build the views containing the desired information.
