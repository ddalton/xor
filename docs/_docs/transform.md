---
layout: docs
title:  Transform
permalink: /docs/transform/
---

XOR promotes loose coupling between persistence and JSON/External models.
This can either be achieved using a static model that mirrors the persistence model or achieved dynamically using a model that supports dynamic model construction such as JSON.

All the persistence operations such as create, update, read etc support data transformation between the external and the persistence models.
This transformation is accomplished using the TypeMapper interface. XOR comes built-in with a few TypeMappers.
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

## Benefits of this architecture

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
