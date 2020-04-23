support paging and scrolling


What is the need for getSourceClass and getTargetClass?
Why do we need isExternal and isDomain?
Why do we need createExternalType?
why immutable?
why isOpen?
why isToExternal?

getEntityKey and getSurrogateKey methods need to be moved to AbstractType and made static





TODO: test with a complex SP, that retrieves multiple levels of an entity
and a custom child SQL, connection at a multi-part anchor e.g, a.b


This complex test should also be performed with OQL->SQL and SQL->SP and SP->SP

Test with inheritance and collection combination

eg. group -> privategroup
    users

also multi-lever
    users -> organization -> departments -> heads



NOTE - If using temp table, then the query needs to be run using a single JDBC connection to avoid data visibility issues
1. Temp table support - Add test cases
   - [DONE] setup temp table in Before annotation
   - [DONE] drop temp table in After annotation
   - [DONE] Basic test (OQL -> SQL) - Use view TASKCHILDRENMIXTEMP - refer to previous test using view TASKCHILDRENMIX
     - test the insert is working
     - test the join is working
     - test the type selection is working (explicitly specified in SQL)
   - [DONE] Call stored procedure (SP)
   - [DONE] Advanced test #2 (SP -> SQL) 
   - [DONE] test #2 (SQL -> SP) - Use TEMP table
   - [DONE] Advanced test #3 (SP -> SP) - NOT populated TEMP table in first SP (less efficient)
   - [DONE] Advanced test #4 (SP -> SP) - populated TEMP table in first SP (more efficient)
   - [DONE] Advanced test #5 (OQL -> SP) - populates TEMP table in XOR
   - [DONE] Advanced test #6 (Single SP, multiple view) - the single SP returns multiple result sets
   - Ensure we rollback so the temp table is cleared
   - Test with single id to SP - avoid using TEMP table in this case
2. Alias and narrow support in the json query for AggregateView - Alias and narrow support does not make sense in TraversalView


Bug - Why cannot child view with EntityType be expanded - JPAMutableJsonTest#readEmployeeNumber
    - AggregateView#expand currently returns if a child view has EntityType populated




0. Parallel dispatcher implementation
   a) use aliases to create a wide fan-out aggregatetree
   b) use inheritance to create a wide fan-out aggregate tree - Check SimpleRecord example
   Test with JPA and ensure it queries the committed values
1. Explore replacing IN (<subquery>) with EXISTS (<correlated_subquery>)
   also remove any unnecessary tables from the join in the subquery,
   unless it participates in the critical path or has a predicate clause
   Supports multi-column 
2. Duplicate child entries - new test
3. Doubly nested child query tet
4. TO_ONE child query test
6. Subtype querying and json/state enhancement
7. Paging test - tokens and parallel collections
8. Collection of embedded objects

Fix narrow call by replacing it with the ability to resolve the object based on the subtypes and join condition
- see JSON representation


1. Refactor to use JSONObject to represent view and not list of properties.
   Allows for rich expression of the query.
   StateGraph construction should be based off JSON.
   Filters should be added to StateGraph so the queries can reflect them and not get them from the settings object

2. Test subquery
   Add limit and next token functionality to the QueryTree
   How do splits and query filters interact. Need to ensure the stitching filters out the object correctly.

3. Multi-column foreign key test
4. Entity filter test. Also need to test it in split scenario. See #2.
7. Test same entity type but on different anchors and ensure it brings different shapes


HSQLDB testing -
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.3/hsqldb-2.3.3.jar org.hsqldb.util.DatabaseManagerSwing
