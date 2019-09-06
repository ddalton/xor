the stitch functionality needs to work on same objects but on differnt paths.
So path should include container unique id - How???
1. We enhance ancestorpath to contain type information to handle subtypes
2. We support simpleancestorpath for backwards compatibility
3. We perform stitching by key objects based on ancestorpath. The same key on the ancestor path might have multiple values, if the path represents
   collections on different parts of the graph and contain the same objects.

Now where are these temporary results saved? to be used later for stitching

Implement BFSTraversal for FragmentBuilder:178

ancestorPath - should have subtype information
      for e.g.,    a|com.Type1:b|c|d|e
              owner|Technician:skill
QueryTree#findFragment - Needs to also take in type to get the subtype





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
