---
layout: docs
title:  Data Generation
permalink: /docs/generation/
---

One of the main benefits of using XOR is the ability to generate data.
The view plays an important role here as it defines the scope of the data being generated.

There are three main dimensions to data generation:
1. Specifying Domain values for primitive types
2. Controlling collection sparseness for toMany relationships
3. Specifying subtype for toOne relationship to entity having subtypes


## Domain values

Data generation by default populates an entity with random data. But this aspect can be controlled with the use of domain values.

Domain values is a mechanism in XOR, where the user can specify the range/choice of values upon which the value of a field is based upon. There are two main reasons why this is desired:
* Entity object needs to be shared 
* Meaningful entity data that ensures business logic validation compliance

### Shared entity references

When data is generated, entities are created using values specified in the Domain values file. The reference to entities are based upon the business key. The business key value is taken from Domain values input. Now when the Aggregate is saved, all references to the entity with the same business key for that type now points to the same entity. 

This allows an aggregate to more faithfully represent the layout of a real aggregate in the database. 

The Domain values file can be generated using the export mechanism and later edited with small modifications.

### Business logic validation compliance

If data is randomly generated, then it could violate the business logic validation for that entity.Domain values helps to solve this issue nicely.

### Domain values file specification

The Domain values file is an Excel file and closely mirrors the format of an Aggregate Excel export file. The differences will be outlined below:

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Sheet name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>Info</code></p></td>
      <td><p>

          This Excel sheet describes all the entities referenced in the aggregate(s) and information about its attributes.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Domain Types</code></p></td>
      <td><p>

          This is the sheet, entity type mapping and has two columns. 
          The first "Sheet name" refers to the name of the sheet in which the Domain values for a particular entity type is described. The second column "Entity Type" refers to the name of the entity type.
          If there are n entities described, then this sheet will contain n rows, with each row specifying the corresponding sheet for that entity type.

      </p></td>
    </tr>
    <tr>
      <td><p><code>Sheet1..n</code></p></td>
      <td><p>

          Describes the domain values for a particular entity type. There are 3 main aspects to the information described here in the Excel sheet..
          The first is the header row. This row contains a list of all the attributes for which we have the domain values specified.
          The second is the row relating to the Generator class name. This can either be a built in Java Generator or a user defined Generator.
          The third aspect is the arguments to the Generator specified in the remaining columns. Generators that deal with min and max values need only 2 rows of information. Whereas Generators that deal with a set of values will have a variable number of rows of information. More details on the Generator is described later in the document.

      </p></td>
    </tr>
  </tbody>
</table>
</div>

#### Creating the domain values file

The easiest way to create this file is to follow the steps below:

* Export the desired aggregate
* Rename the "Relationships" sheet to "Domain types" and remove the "Relationship" column
* Remove any sheets for which we do want to specify the domain values. Also be sure to remove the corresponding rows from the "Domain types" sheet.
* Remove any columns in the remaining sheets for which we do not want to specify the domain values.
* For the remaining entity type sheets insert a new row under the header row and specify the appropriate generator java class name.
* Adjust the values accordingly in the remaining rows.

### Built-in generators

XOR comes with a set of built-in generators. But the user can add their own versions if needed. Refer to the java doc of tools.xor.generator.Generator for more details. You could extend from DefaultGenerator to create your own version.

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Generator class name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>tools.xor.generator.Range</code></p></td>
      <td><p>

          Generates a value between a min and max. 
          The first cell under the generator name holds the min value and the next one below holds the max value.
          The max value is optional.

      </p></td>
    </tr>
    <tr>
      <td><p><code>tools.xor.generator.DateRange</code></p></td>
      <td><p>

          Specifies the date range in in ISO8601 format. For example: 2017-01-01T09:45:00.000UTC
          The first cell holds the minimum value and the next one below holds the max value. The max value is optional. 

      </p></td>
    </tr>
    <tr>
      <td><p><code>tools.xor.generator.Choices</code></p></td>
      <td><p>

          Specify a list of values under the generator name.
          The generator will randomly pick a value from this list. 

      </p></td>
    </tr>
    <tr>
      <td><p><code>tools.xor.generator.LinkedChoices</code></p></td>
      <td><p>

          Specify a list of values under the generator name. The number of values between all the LinkedChoices columns need to be the same.
          The generator will randomly pick a value from this list for the first LinkedChoices column and any succeeding LinkedChoices column will pick the value on the same row chosen as the first one. This is the linked functionality.

          This generator is useful for modelling unique keys as an entity needs to have the same set of unique values when generated.

      </p></td>
    </tr>
  </tbody>
</table>
</div>

## Collection sparseness

The data generation related to collections is what decides how big the object graph is. The collection sparseness is a global value and can be overridden on a per relationship basis. The sparseness value is a factor of the max number of objects in the object graph. So the maximum value is 1. 

For example, if a large graph is being generated, then for a sparseness value of 0.1f, the collection can have anywhere from 1-100 elements in the collection. The number of elements is a random number within this range.

![](/img/taskchildren.png)

Using the above model, the following code has the global value is configured as 0.2f and the dependants collection relationship set to 0.1f.

```java
Settings settings = new Settings();
settings.setSparseness(0.2f);
settings.getCollectionSparseness().put("dependants", 0.1f);
```

## Subtype selection

If the persistence model has inheritance then the data generation has to be given the ability to create such subtypes.
The subtypes are randomly chosen when creating such objects. This can be controlled used the following built-in generators:

<div class="mobile-side-scroller">
<table>
  <thead>
    <tr>
      <th>Generator class name</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><p><code>tools.xor.generator.BoundedSubType</code></p></td>
      <td><p>

          Use this generator if the objects need to be bounded to a particular subtype and its subtypes.

      </p></td>
    </tr>
    <tr>
      <td><p><code>tools.xor.generator.SubTypeChoices</code></p></td>
      <td><p>

          Use this generator if the objects need to be constrained to a random subset of the subtypes.

      </p></td>
    </tr>
  </tbody>
</table>
</div>
