---
layout: docs
title:  Query and modification
permalink: /docs/operations/
---

XOR allows efficient data querying and update of entities.
The supported operations are:

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Operation</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>Create</code></p></td>
      <td><p>

          Used to create a new entity. All dependent objects of this entity will be created and not fetched from the database. 
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Update</code></p></td>
      <td><p>

          Updates or creates a new entity. Always checks the database if an object with the given id/key is present and if so loads that entity and updates it.
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Merge</code></p></td>
      <td><p>

          Same behavior as update operation. The only difference is with collection handling. Obsolete entries will be deleted in the update operation, whereas it will not be removed in the merge operation.
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Clone</code></p></td>
      <td><p>

          Makes a deep copy of a persistent entity. The copy scope can be defined by a XOR view specification.
          Returns a persistent managed entity.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Read</code></p></td>
      <td><p>

          Reads an entity according to an XOR view specification. 
          A variant of the read API is the query API. Refer to AggregateManager for more details on this API.        

          The read operation returns a non-persistent managed object, i.e., an object of an external type such as JSON.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Delete</code></p></td>
      <td><p>

          Delete an entity and all its dependant objects.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Load</code></p></td>
      <td><p>

          Load a persistent entity from the database. The object returned is a persistent managed entity.

      </p></td>
    </tr>
  </tbody>
</table>
</div>
