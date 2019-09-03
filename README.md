Test inheritance join queries
skill property is not being selected in the OQL
Add DowncastSplitter - for OQL split also by downcast states

select person1_.UUID as col_0_0_, person1_.name as col_1_0_, person1_.displayName as col_2_0_, person1_.description as col_3_0_, person1_.iconUrl as col_4_0_, person1_.detailedDescription as col_5_0_, task0_.name as col_6_0_, task0_.UUID as col_7_0_, person2_.UUID as col_8_0_ 
from Task task0_ left outer join Person person1_ on task0_.ownedBy_UUID=person1_.UUID inner join Technician person1_1_ on person1_.UUID=person1_1_.UUID left outer join Person person2_ on task0_.ownedBy_UUID=person2_.UUID inner join Technician person2_1_ on person2_.UUID=person2_1_.UUID 
where task0_.UUID=? order by task0_.UUID
-- Check above query on a HANA database and after running test case - 
mvn test -Dtest=JPAQueryInheritanceTest#queryTaskSkill

Check why the above query returns 0 rows

Debug inheritance and parallel collection count
Test this for JDBC po

print the StateGraph
Unable to add unknown attribute to state graph: skill to state: OT3LFDWNPJ9POEPX 
Unable to add unknown attribute to state graph: skill to state: FFAWOR8ZEQ3U2QUM 

and the TREAT operator

                    owner
       T A S K  ————————————————> P E R S O N
                                       ^
                                       |
                                       |
                              T E C H N I C I A N — skill



SELECT p.skill FROM task t left join TREAT(t.owner AS Technician) p 

for this query to be created, “skill” attribute needs to be identified as being anchored on Technician

StateGraph - have a new method on SubtypeState. Get state based on attribute:
getState(String name)





b. EntityKey (id and Type). If Type api is used, it should always be based on root concrete type.
c. Subtype querying and json/state enhancement


6. Paging test - tokens and parallel collections

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
   Parallelizing them will be the next step

   How do splits and query filters interact. Need to ensure the stitching filters out the object correctly.

   Alias interquery flag?? - see #1 (JSON representation)

3. Multi-column foreign key test
4. Entity filter test. Also need to test it in split scenario. See #2.
5. Inheritance test - see #1 (JSON representation)
7. Test same entity type but on different anchors and ensure it brings different shapes


HSQLDB testing -
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.3/hsqldb-2.3.3.jar org.hsqldb.util.DatabaseManagerSwing

   Details
   =======

0. Type narrowing of a property - see JSON representation
   NarrowHandler(options)
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
