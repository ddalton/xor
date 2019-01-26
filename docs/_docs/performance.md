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
