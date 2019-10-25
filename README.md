   get rid of DAS#populateNarrowedClass
   get rid of DAS#getNarrowedClass

   We should not do automatic type narrowing (downcast) - Only if the user explicitly states, should we do that.
   Due to the way we reconstitute - (subtype to parent type queries), the objects with the correct type are created



Test for mix SQL and OQL queries in a view reference and view
a) Test OQL and SQL - Need to add foreignKey for EXISTS or use Function with FREESTYLE
   - basic case done
   - can we support SQL and then OQL?
   - or is SQL only available in the leaf nodes?
   - inheritance, entityType etc...
   - In VO and POJO models
b) Test OQL and StoredProcedure

Temp table support
==================
If SingleDispatcher and the number of ids > 2000 (more than 2 calls needed)
Reason for SingleDispatcher is that the data can be accessed by other queries
For a parallel dispatcher the temp table data is not accessible
Then it is better to do a INSERT INTO <temp table> AS SELECT <GUID>, <Primary key columns> ... 
The GUID is part of the Query object
Use this temp table to do child query processing
Solves the issue of large dataset and also for Stored procedure data passing
 -- Ids for the current entity need to be populated (for subtypes)
 -- Ids for the collection elements needs to be populated (for parallel collection/inheritance on collection elements etc)

Bug - Why cannot child view with EntityType be expanded - JPAMutableJsonTest#readEmployeeNumber
    - AggregateView#expand currently returns if a child view has EntityType populated



Supporting the following helps for JDBC provider - since it makes the serial dispatcher for JDBC more efficient
=======================================

Add Query Diagram support for JDBC provider and allow it to analyze and optimize and come with a desired query order
and also modify SQL query to support that query order in a DB agnostic manner.
  We do this for JDBC as we do not have to worry about splitting for inheritance support
  TODO: remove this splitter for JDBC provider and open type (Needed for POJO types)
  - simple query tree
  - query tree with sub-queries (EXISTS and IN) - semi-join 
  - anti semi-join. Identify when an OUTER JOIN can be converted to a NOT EXISTS condition and evaluate the performance
** Provide feeature - enforce robust plan (Use techniques to control join order)
  Modify semi-join arrow by adding "none" to add spaces before arrow
  For example, nonenonenonenormal




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

Benchmarks
==========
Run OO7 BENCHMARK against XOR
http://oo7j.sourceforge.net
~/Downloads/oo7

poleposition - A bit simpler than OO7, but should be easier to add XOR.
~/Download/xor/pole
http://polepos.org/


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

   Details
   =======

0. Subtype controls - Needed if the data is duplicated on all the concrete types and not normalized
   options include
   - SUBCLASS NONE (no sub classes included)
   - SUBCLASS ANY (default behavior)
   - SUBCLASS a
   - SUBCLASS (a, b)
   - SUBCLASS NOT b 
   - SUBCLASS NOT (a, b)
  
1. Process ALIAS and NARROW functions - See JSON representation
  How is an alias field processed? Add a property to an open type
  StateTree#extend
    a) Alias test of an existing property e.g., 
       taskDetails t1
       taskDetails t2
    b) Alias test of a non-existing property e.g., root

2. SP multi query ordering

3. include/skip functionality

Pending PlainJDBCTest cases
===========================
Add mappings for following in DBTranslator
NCLOB
SMALLDECIMAL
SECONDDATE
ST_POINT
