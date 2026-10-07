---
layout: docs
title: Performance
permalink: /docs/performance/
---

We saw earlier in the <a href="../view/">Views Guide</a> how to define an XOR view. In this section we will build upon this to see how we can improve the performance of loading view specific data.

Using the same example as before we will assume that the view loading through the ORM is slow. We would like to now make this faster. XOR provides a few approaches.

* OQL (Object Query Language) query
* Native SQL query
* Stored Procedure Query

Below is an example of using a native query:

```xml
<AggregateViews>
    <aggregateView>
        <name>TASKDETAILSID</name>
        <attributeList>id</attributeList>
        <attributeList>name</attributeList>
        <attributeList>description</attributeList>
        <attributeList>taskDetails.id</attributeList>
        <nativeQuery>
            <selectClause>
                <![CDATA[SELECT t.UUID,
                                t.NAME,
                                t.DESCRIPTION,
				td.UUID
                           FROM Task t, TaskDetails td
			  WHERE t.UUID = td.UUID]]>
            </selectClause>
        </nativeQuery>
    </aggregateView>
</AggregateViews>
```

The advantage is that the application code does not have to be changed and the performance is boosted automatically.

### Mapping columns to attributes

By default the columns of a native query or stored procedure are mapped to the view attributes by position, so they need to be in the same order as the `attributeList`.
Alternatively, label each column with the attribute it populates using a column alias. The columns can then be in any order:

```xml
<selectClause>
    <![CDATA[SELECT td.UUID AS "taskDetails.id",
                    t.NAME AS name,
                    t.DESCRIPTION AS description,
                    t.UUID AS id
               FROM Task t, TaskDetails td
              WHERE t.UUID = td.UUID]]>
</selectClause>
```

* Labels are matched to the attribute paths ignoring case. An attribute path containing a `.` needs a quoted alias.
* The columns are mapped by name only if every attribute has exactly one matching column and every column matches an attribute. Otherwise they are mapped by position.
* System columns are labeled the same way, using the path of the collection or entity they belong to, e.g., `"taskChildren.INDEX_"` for a list index, `"taskChildren.KEY_"` for a map key and `TYPE_` for the entity type.
* XOR reports an error if the query returns the wrong number of columns, or when mapping by position, if a column is labeled with an attribute at a different position, which usually means two columns have been swapped.
