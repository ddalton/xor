in HanaPerf2Test#testSP2 check why the SQL returns only 500 rows

Focus areas
===========
Reconstitution rules (AbstractDispatcher):
1. Convert NFA to DFA
   i.e., process all inheritance target querytrees first, so all
   unlabelled edges are processed
2. Perform top-down processing from the root of the aggregate tree

For step (2) to work properly, all InterQuery edges need to have
a proper source fragment.
Even for Custom views, additional QueryFragment objects need to be created.
They do not have to be attached to the root fragment of custom querytrees
as they are only there to assist in holding the visitor objects to help
with object reconstitution.


CLARITY
-=====
Need clarity on how the InterQuery edge is specified and how 
the custom query rules fit in

So in the example, below for SP-SQL invocation between GROUP and USERS 
using the interquery edge "users".
The child view name is "users" and since the owner is the root query fragment,
no additonal query fragment needs to be created.
The Interquery fragment named "users" will be created connection this queryfragment
and the root query fragment for the SQL query tree.

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
   - Advanced test #2 (SP -> SQL) ( Worked on in test HanaPerf2Test#testSP2) - ERROR: Duplicate record identified, please enhance the view to distinguish this duplicate record,

TESTING....
mvnDebug -DforkMode=never test -Dtest=HanaPerf2Test

test with view modification below
and remove view name "users"

		<name>USERS_DETAIL</name>
		<attributeList>ROOTID</attributeList>
		<attributeList>users.ROOTID</attributeList>
		<attributeList>users.CUS_ORGANIZATION</attributeList>
		<attributeList>users.CUS_UNIQUENAME</attributeList>
		<primaryKeyAttribute>ROOTID</primaryKeyAttribute>


   - Advanced test #3 (SP -> SP) - populated TEMP table in first SP (more efficient)
   - Advanced test (OQL -> SP) - populates TEMP table in XOR
   - Ensure we rollback so the temp table is cleared
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
