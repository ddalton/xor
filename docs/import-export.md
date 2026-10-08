# Import and export

XOR exports an aggregate, including its relationships, to Excel or CSV, and imports it back with every
relationship restored. This is useful to copy realistic data between environments, to seed performance
tests, and to create [domain values](data-generation.md#domain-values-from-excel) workbooks.

> **Note**
> Excel support uses Apache POI, which XOR declares with `provided` scope. Add `org.apache.poi:poi` and
> `org.apache.poi:poi-ooxml` to your application to use it.

## API

| `AggregateManager` method | Format | Implemented by |
| --- | --- | --- |
| `exportAggregate(String filePath, Object entity, Settings)` | Excel workbook | `ExcelExportImport` |
| `importAggregate(String filePath, Settings)` | Excel workbook, loaded from the classpath | `ExcelExportImport` |
| `exportCSV(String folder, Object entity, Settings)` | Folder of CSV files | `CSVExportImport` |
| `importCSV(String folder, Settings)` | Folder of CSV files, loaded from the classpath | `CSVExportImport` |
| `exportDenormalized(OutputStream, Settings)` | One flat sheet with the result of a denormalized query | `ExcelExportImport` |
| `importDenormalized(InputStream, Settings)` | One flat sheet | `ExcelExportImport` |

Both classes implement `tools.xor.service.exim.ExportImport`, which defines `exportAggregate` and
`importAggregate`. The scope of an export is the view in the settings, extended by any association
settings.

## Excel format

Each entity type of the aggregate gets its own sheet:

| Sheet | Content |
| --- | --- |
| `Info` | A description of every entity type in the export and its attributes, with a legend |
| `Entity` | The root entity |
| `Relationships` | Which sheet holds the objects at the other end of each relationship, e.g. `Sheet1` for `tools.xor.db.pm.Task:taskChildren` |
| `Sheet1`, `Sheet2`, ... | The related entities |
| `Overflow` | Only when needed: values too long for an Excel cell |

Besides the attribute columns, each sheet carries bookkeeping columns: `XOR.id` (an identifier within the
file), `XOR.type` (the entity type), `XOR.type:<collection>` (the collection class) and `|XOR|<relationship>`
(the `XOR.id` of the referenced object). These keep shared and cyclic references intact on import.

### Values larger than a cell

An Excel cell holds at most 32,767 characters. A longer value, such as a Base64 encoded BLOB, is written to
a row of the `Overflow` sheet, split across as many cells as needed, and the cell itself stores a reference
to that row (`XOR.overflow:<row>`). On import the reference is resolved and the value reassembled
(`ExcelExportImport.writeOverflow` and `ExcelExportImport.getStringCellValue`).

### Example

From `DefaultMutableJson`:

```java
Settings settings = getSettings();
settings.expand(new AssociationSetting(TaskDetails.class));
settings.setEntityClass(Task.class);
Task task = (Task) aggregateService.create(json, settings);

aggregateService.exportAggregate("taskExcel.xlsx", task, settings);
```

```java
Settings settings = getSettings();
settings.expand(new AssociationSetting(TaskDetails.class));
List result = (List) aggregateService.importAggregate("taskOneChild.xlsx", settings);
Task task = (Task) result.get(0);
```

For large exports set `excel.streaming=true` in `xor.properties` to write with POI's streaming workbook.

## CSV format

A CSV export is a folder with one file per sheet of the Excel format:

```text
Entity.csv
Relationships.csv
Sheet1.csv
Sheet2.csv
```

`src/test/resources/bulk` is an example. Its `Relationships.csv` maps `Sheet1` to
`tools.xor.db.pm.Task:taskChildren` and `Sheet2` to `tools.xor.db.pm.Task:taskDetails`.

```java
aggregateService.exportCSV("taskcsv/", task, settings);
```

```java
Settings settings = new Settings();
settings.setEntityType(aggregateService.getDataModel().getShape().getType(Task.class));
settings.init(aggregateService.getDataModel().getShape());
aggregateService.importCSV("bulk/", settings);
```

For loading table data directly over JDBC, with foreign key lookups and generated columns, use the
[CSV loader](data-generation.md#loading-csv-files-with-the-csv-loader) instead.

Previous: [Data generation](data-generation.md) | Next: [Meta API](meta-api.md)
