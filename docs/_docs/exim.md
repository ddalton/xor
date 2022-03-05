---
layout: docs
title: Export Import
permalink: /docs/exim/
---

In order to performance test an application, there needs to be a way to populate it with a large amount of data.
XOR allows the following data import/export options:

* Excel
* CSV

The data that is exported/imported can contain relationships and these are preserved faithfully during the import.
This mechanism works differently depending upon which mechanism is chosen.

## Excel
When an Entity is exported, it may contain other entities depending on which relationships are chosen in the view.
Each entity is captured in a separate Excel sheet. An example of the structure is:

```
Entity
Relationships
Sheet1
```

`Entity` represents the main sheet containing the root entity object being exported.
`Relationships` sheet contains the relationships and points to the sheet containing entities on the other side of the relationship.
`Sheet1` contains a sheet for the entity on the other side of the relationship.

## CSV
CSV export structure has exported information in a separate folder with the following files for the example above:

```
Entity.csv
Relationships.csv
Sheet1.csv
```

### API
The Export Import API is defined in the interface `ExportImport`.
This interface is implemented by the following two classes one for Excel and the other for CSV.

* ExcelExportImport
* CSVExportImport
