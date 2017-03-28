---
layout: docs
title:  Meta API
permalink: /docs/meta/
---

The Meta API provides high level information on the types and attribute information present in the domain model. The meta API is accessed from the MetaModel object accessible from AggregateManager, the main entry point into XOR.

Some of the information methods in this API is described in the table below.

## Important Meta API

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
      <td><p><code>getViewList</code></p></td>
      <td><p>

          Returns a list of all the user defined views registered in XOR.

      </p></td>
    </tr>
    <tr>
      <td><p><code>getTypeList</code></p></td>
      <td><p>

          Returns a list of all the type names registered with XOR.

      </p></td>
    </tr>
    <tr>
      <td><p><code>getViewAttributes</code></p></td>
      <td><p>

          Return a list of all the attribute paths that comprise the view.

      </p></td>
    </tr>
    <tr>
      <td><p><code>getAggregateAttributes</code></p></td>
      <td><p>

          Returns a list of all the attributes in the given entity that comprises the aggregate rooted at the entity. This is depicted in a concise RegEx form.

      </p></td>
    </tr>
  </tbody>
</table>
</div>
