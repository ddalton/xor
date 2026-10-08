# Custom queries and tuning

By default XOR generates the queries for a view from the ORM mapping. When a generated query performs
poorly, the view can carry its own query instead. The view's attribute list stays the same, so callers
receive the same shape and no code changes. A DBA can tune the query in XML alone.

All examples on this page are from `src/test/resources/AggregateViews.xml`.

## Options

| Element | Query language | Columns mapped | Typical use |
| --- | --- | --- | --- |
| `<userOQLQuery>` | OQL (JPQL with JPA) | By position | Better joins or fetch strategy while staying portable |
| `<nativeQuery>` | SQL | By position or by column alias | Database specific SQL, hints, a different join order |
| `<storedProcedure>` with `<action>READ</action>` | A procedure call | By position or by column label | Logic that already lives in the database |

A view with any of these is a *custom* view (`AggregateView.isCustom()`). When a custom view is referenced
from another view, it runs as a separate child query. See [Views](views.md#composition-inlined-or-separate-query).

> **Note**
> Native SQL and stored procedures bypass the ORM session. Call `settings.setPreFlush(true)` when the query
> must see changes made earlier in the same transaction, as the tests do.

## User OQL

```xml
<aggregateView>
    <name>BASICINFO_OQL</name>
    <attributeList>[BASICINFO]</attributeList>
    <userOQLQuery>
        <selectClause>
            <![CDATA[SELECT id,
                            name,
                            displayName,
                            description,
                            iconUrl,
                            detailedDescription
                       FROM tools.xor.db.base.Person]]>
        </selectClause>
        <function type="FREESTYLE">
            <args>WHERE id = :ID_</args>
        </function>
    </userOQLQuery>
</aggregateView>
```

The selected values must follow the order of the expanded attribute list: `[BASICINFO]` expands to `id`,
`name`, `displayName`, `description`, `iconUrl`, `detailedDescription`. The `WHERE id = :ID_` function is
applied when the input object has an id, which XOR binds to `ID_`.

### Paging and scrolling

`FREESTYLE` functions with an `include` attribute are only added when the caller passes a parameter with
that name. `BASICINFO_OQL_SORT` supports both offset paging and keyset scrolling:

```xml
<aggregateView>
    <name>BASICINFO_OQL_SORT</name>
    <attributeList>[BASICINFO]</attributeList>
    <userOQLQuery>
        <selectClause>
            <![CDATA[SELECT id,
                            name,
                            displayName,
                            description,
                            iconUrl,
                            detailedDescription
                       FROM tools.xor.db.pm.Task]]>
        </selectClause>
        <function type="FREESTYLE">
            <args>WHERE id = :ID_</args>
        </function>
        <function type="FREESTYLE" include="page">
            <args> ORDER BY id ASC</args>
        </function>
        <function type="FREESTYLE" include="scroll">
            <args>WHERE name > :ORDER_BY_startName AND id > :ORDER_BY_startId ORDER BY name, id</args>
        </function>
    </userOQLQuery>
</aggregateView>
```

Offset paging, from `SwaggerDataModelTest`:

```java
Map<String, Object> userParams = new HashMap<>();
userParams.put("page", null);
settings.setParams(userParams);
settings.setOffset(25);
settings.setLimit(25);
List<?> secondPage = amSwagger.query(null, settings);
```

Scrolling binds the entries of the next token to parameters prefixed with `ORDER_BY_`:

```java
userParams.put("scroll", null);
settings.setParams(userParams);
settings.setLimit(25);

Map<String, Object> nextToken = new HashMap<>();
nextToken.put("startName", "NAME_10035");
nextToken.put("startId", "ID_10035");
settings.setNextToken(nextToken);
List<?> next = amSwagger.query(null, settings);
```

## Native SQL

```xml
<aggregateView>
    <name>BASICINFO_NATIVE</name>
    <attributeList>[BASICINFO]</attributeList>
    <primaryKeyAttribute>id</primaryKeyAttribute>
    <nativeQuery>
        <parameterList name="ID_" />
        <selectClause>
            <![CDATA[SELECT UUID,
                            NAME,
                            DISPLAYNAME,
                            DESCRIPTION,
                            ICONURL,
                            DETAILEDDESCRIPTION
                       FROM Person]]>
        </selectClause>
        <function scope="ROOT" type="FREESTYLE">
            <args>WHERE UUID = ?</args>
        </function>
        <function type="FREESTYLE" scope="NOTROOT">
            <args>WHERE UUID IN (^PLACEHOLDER^)</args>
        </function>
    </nativeQuery>
</aggregateView>
```

How the SQL is assembled (`QueryFromSQL`):

1. Start with `selectClause`.
2. Append each applicable `FREESTYLE` function, in `position` order.
3. Positional `?` markers consume the `parameterList` entries in order. Here `ID_` is bound to the id of
   the input object.

### Function attributes

| Attribute | Values | Meaning |
| --- | --- | --- |
| `type` | `FREESTYLE` | Text appended to the query. User queries only use `FREESTYLE` |
| `scope` | `ANY` (default), `ROOT`, `NOTROOT` | `ROOT`: only when the view is the top-level query. `NOTROOT`: only when it runs as a child of another query |
| `include` | a parameter name | Only when the caller passes this parameter |
| `position` | integer | Order of the function in the query |

A function is also skipped when one of its named parameters has no value, so optional filters need no
special handling.

### Reserved parameters

| Parameter | Bound to |
| --- | --- |
| `ID_` | The identifier of the input object |
| `ORDER_BY_<name>` | Entry `<name>` of `Settings.getNextToken()` |
| `^PLACEHOLDER^` | Not a parameter: replaced with `:PARENT_INLIST_1, :PARENT_INLIST_2, ...`, the ids from the parent query |
| `PARENT_INVOCATION_ID_` | Invocation id of the parent query, for joining with the query join table |
| `INVOCATION_ID_` | Invocation id of this query, for a stored procedure that fills the query join table |
| `LAST_PARENT_ID_` | The last id read by the parent query, for scrolling child queries |

### A native query as a child

`BASICINFO_NATIVE_TASK` is the same pattern on `Task`, plus an optional scrolling condition:

```xml
<aggregateView>
    <name>BASICINFO_NATIVE_TASK</name>
    <attributeList>[BASICINFO]</attributeList>
    <primaryKeyAttribute>id</primaryKeyAttribute>
    <nativeQuery>
        <parameterList name="ID_" />
        <selectClause>
            <![CDATA[SELECT UUID,
                            NAME,
                            DISPLAYNAME,
                            DESCRIPTION,
                            ICONURL,
                            DETAILEDDESCRIPTION
                       FROM Task]]>
        </selectClause>
        <function scope="ROOT" type="FREESTYLE">
            <args>WHERE UUID = ?</args>
        </function>
        <function type="FREESTYLE" scope="NOTROOT">
            <args>WHERE UUID IN (^PLACEHOLDER^)</args>
        </function>
        <function type="FREESTYLE">
            <args> AND NAME > :ORDER_BY_startName ASC AND UUID > :ORDER_BY_startId ASC</args>
        </function>
    </nativeQuery>
</aggregateView>

<aggregateView>
    <name>TASKCHILDRENMIX</name>
    <attributeList>[BASICINFO]</attributeList>
    <attributeList>taskChildren.[BASICINFO_NATIVE_TASK]</attributeList>
</aggregateView>
```

Querying `TASKCHILDRENMIX` runs a generated query for the root tasks and their `taskChildren` ids, then this
native query for the children with the ids in the IN list. [Views](views.md#example-parent-oql-child-native-sql)
traces it step by step.

To join through the query join table instead of an IN list, reference `XOR_QUERY_JOIN_` and
`:PARENT_INVOCATION_ID_` in the `NOTROOT` function, as `BASICINFO_NATIVE_TASK_TEMP` does. The table has to be
created first with `DataStore.createQueryJoinTable(null)`, and the serial dispatcher must be used.

## Extra columns: entity type, list index, map key

A user query may return system columns besides the attributes. Declare them with `<augmenter>`:

| Augmenter | Purpose |
| --- | --- |
| `TYPE_` | The entity type name of the row, for queries over an inheritance hierarchy |
| `INDEX_`, `<path>.INDEX_` | The list index of a list element, e.g. `dependants.INDEX_` |
| `KEY_`, `<path>.KEY_` | The key of a map entry, e.g. `subProjects.KEY_` |

Root-level system columns are named `TYPE_`, `INDEX_` and `KEY_`. Columns of a nested collection use the
full path of the collection, and can be labeled with an alias like any other column, e.g.
`tt.DEP_SEQ AS "dependants.INDEX_"`.

When columns are mapped by position, augmenter columns come after the attribute columns. Querying
`BASICINFO_NATIVE_TYPE` returns `Employee` objects for rows that are employees:

```xml
<aggregateView>
    <name>BASICINFO_NATIVE_TYPE</name>
    <attributeList>[BASICINFO]</attributeList>
    <nativeQuery>
        <augmenter>TYPE_</augmenter>
        <parameterList name="ID_" />
        <selectClause>
            <![CDATA[SELECT p.uuid,
                            name,
                            displayname,
                            description,
                            iconurl,
                            detaileddescription,
                            CASE p.UUID WHEN e.UUID THEN 'tools.xor.db.base.Employee' ELSE 'tools.xor.db.base.Person' END AS TYPE_
                       FROM Person p
                       LEFT JOIN Employee e
                         ON p.uuid = e.uuid]]>
        </selectClause>
        <function type="FREESTYLE">
            <args>WHERE p.UUID = ?</args>
        </function>
    </nativeQuery>
</aggregateView>
```

### Lists: `TASKDEP_NATIVE`

List elements are placed in their collection by their `INDEX_` value (`tools.xor.ListPlacement`). A tuned
native query may therefore return the rows in any order, and repeated rows caused by joins do not duplicate
elements, so a DBA can choose the join order and sorting freely. Without an index column the elements keep
the order of the rows. `TASKDEP_NATIVE` deliberately sorts by the list index in descending order:

```xml
<aggregateView>
    <name>TASKDEP_NATIVE</name>
    <attributeList>[BASICINFO]</attributeList>
    <attributeList>dependants.[VERYBASIC]</attributeList>
    <primaryKeyAttribute>id</primaryKeyAttribute>
    <nativeQuery>
        <augmenter>dependants.INDEX_</augmenter>
        <parameterList name="ID_" />
        <selectClause>
            <![CDATA[SELECT t.UUID AS id,
                            t.NAME AS name,
                            t.DISPLAYNAME AS displayName,
                            t.DESCRIPTION AS description,
                            t.ICONURL AS iconUrl,
                            t.DETAILEDDESCRIPTION AS detailedDescription,
                            tt.DEP_SEQ AS "dependants.INDEX_",
                            d.UUID AS "dependants.id",
                            d.NAME AS "dependants.name",
                            d.DISPLAYNAME AS "dependants.displayName",
                            d.DESCRIPTION AS "dependants.description"
                       FROM Task t
                       LEFT JOIN TASK_TASK tt ON tt.TASK_UUID = t.UUID
                       LEFT JOIN Task d ON d.UUID = tt.DEPENDANTS_UUID]]>
        </selectClause>
        <function scope="ROOT" type="FREESTYLE">
            <args>WHERE t.UUID = ? ORDER BY tt.DEP_SEQ DESC</args>
        </function>
    </nativeQuery>
</aggregateView>
```

The `dependants` list is still returned in index order.

### Maps: `SUBPROJECTS_NATIVE`

The map key comes from the `<path>.KEY_` column. Here the subprojects are keyed by name:

```xml
<aggregateView>
    <name>SUBPROJECTS_NATIVE</name>
    <attributeList>[BASICINFO]</attributeList>
    <attributeList>subProjects.[VERYBASIC]</attributeList>
    <primaryKeyAttribute>id</primaryKeyAttribute>
    <nativeQuery>
        <augmenter>subProjects.KEY_</augmenter>
        <parameterList name="ID_" />
        <selectClause>
            <![CDATA[SELECT p.UUID AS id,
                            p.NAME AS name,
                            p.DISPLAYNAME AS displayName,
                            p.DESCRIPTION AS description,
                            p.ICONURL AS iconUrl,
                            p.DETAILEDDESCRIPTION AS detailedDescription,
                            s.UUID AS "subProjects.id",
                            s.NAME AS "subProjects.name",
                            s.DISPLAYNAME AS "subProjects.displayName",
                            s.DESCRIPTION AS "subProjects.description",
                            s.NAME AS "subProjects.KEY_"
                       FROM Project p
                       LEFT JOIN PROJECT_PROJECT pp ON pp.PROJECT_UUID = p.UUID
                       LEFT JOIN Project s ON s.UUID = pp.SUBPROJECTS_UUID]]>
        </selectClause>
        <function scope="ROOT" type="FREESTYLE">
            <args>WHERE p.UUID = ? ORDER BY s.NAME DESC</args>
        </function>
    </nativeQuery>
</aggregateView>
```

With a JSON type mapper a map is returned as a JSON object keyed by the string form of the map key.

> **Note**
> Writing maps from JSON (`create`, `update`) is not supported yet.

## Stored procedures

A view can be filled by a stored procedure with action `READ`:

```xml
<aggregateView>
    <name>TESTSELECT_SP1</name>
    <attributeList>ROOTID</attributeList>
    <attributeList>GR_USERS</attributeList>
    <attributeList>GR_UNIQUENAME</attributeList>
    <attributeList>GR_CREATED</attributeList>
    <attributeList>GR_MODIFIED</attributeList>
    <primaryKeyAttribute>ROOTID</primaryKeyAttribute>
    <storedProcedure>
        <name>getselect</name>
        <action>READ</action>
        <parameterList name="idcol" mode="IN" position="1"
            type="INTEGER" defaultValue="0" />
        <callString>call testselect1(?)</callString>
    </storedProcedure>
</aggregateView>
```

| Element or attribute | Meaning |
| --- | --- |
| `<name>`, `<action>` | Procedure name and the operation it serves. Queries use `READ` |
| `<callString>` | The JDBC call |
| `<parameterList>` | A parameter: `name` or `attribute`, `type` (a `java.sql.Types` name), `mode` (`IN`, `OUT`, `INOUT`), `position`, `defaultValue` |
| `<implicit>` | `true` when the call returns result sets directly instead of through parameters |
| `<multiple>` | `true` when the procedure returns several result sets |
| `resultPosition` attribute on a view or child view | Which result set fills that view |
| `tempTablePopulated` attribute on the view | The procedure fills the query join table itself, using `INVOCATION_ID_` |

A stored procedure that runs as a child receives its parent's ids through the query join table:

```xml
<storedProcedure>
    <name>getselect</name>
    <action>READ</action>
    <parameterList name="PARENT_INVOCATION_ID_" mode="IN" type="VARCHAR" />
    <callString>call testselect2(?)</callString>
</storedProcedure>
```

> **Note**
> Under JPA, stored procedure statements are created by the configured `PersistenceUtil`. See
> [Getting started](getting-started.md#using-plain-jdbc).

## Mapping columns to attributes

By default the columns of a native query or stored procedure are mapped to the view attributes by position, so
they need to be in the same order as the `attributeList`. Alternatively, label each column with the attribute
it populates using a column alias. The columns can then be in any order:

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

* Labels are matched to the attribute paths ignoring case. An attribute path containing a `.` needs a quoted
  alias.
* The columns are mapped by name only if every attribute has exactly one matching column and every column
  matches an attribute. Otherwise they are mapped by position.
* System columns are labeled the same way, using the path of the collection or entity they belong to, e.g.,
  `"taskChildren.INDEX_"` for a list index, `"taskChildren.KEY_"` for a map key and `TYPE_` for the entity
  type.
* XOR reports an error if the query returns the wrong number of columns, or when mapping by position, if a
  column is labeled with an attribute at a different position, which usually means two columns have been
  swapped.

`BASICINFO_NATIVE_ALIASED` returns its columns in a different order than `[BASICINFO]`:

```xml
<aggregateView>
    <name>BASICINFO_NATIVE_ALIASED</name>
    <attributeList>[BASICINFO]</attributeList>
    <primaryKeyAttribute>id</primaryKeyAttribute>
    <nativeQuery>
        <parameterList name="ID_" />
        <selectClause>
            <![CDATA[SELECT DETAILEDDESCRIPTION AS "detailedDescription",
                            DESCRIPTION AS description,
                            NAME AS name,
                            ICONURL AS iconUrl,
                            UUID AS id,
                            DISPLAYNAME AS displayName
                       FROM Person]]>
        </selectClause>
        <function scope="ROOT" type="FREESTYLE">
            <args>WHERE UUID = ?</args>
        </function>
    </nativeQuery>
</aggregateView>
```

The checks catch common mistakes. `BASICINFO_NATIVE_SWAPPED` selects `DISPLAYNAME` before `NAME`; since the
labels match attribute names at other positions, the query fails with a message that the columns of the
query for view `BASICINFO_NATIVE_SWAPPED` appear to be *out of order*. `BASICINFO_NATIVE_MISSING_COLUMN` omits
`DETAILEDDESCRIPTION` and fails with *returned 5 columns, but 6 columns are expected*. Both messages list
the expected order and the query. The mapping is implemented in `tools.xor.view.ColumnMapping`.

Previous: [Views](views.md) | Next: [Operations](operations.md)
