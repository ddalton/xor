Fix narrow call by replacing it with the ability to resolve the object based on the subtypes and join condition


Pending PlainJDBCTest cases
===========================
foreign key inverse relationship name
1. Deeply nested attribute. For e.g., a.b.c.d.e.f
   Test how the join works in these cases
   Make sure the Query auto-adds the join columns needed for reconstitution.
2. Ensure that the absolute path is used to create the entity key and not relative path
   Currently the tree split occurs from the root node. This is wasteful.
   We need to split at the point where we need and adjust the anchor path to both relative and
   absolute:
   relative path - Needed for resolving objects for the new queries
   absolute path - Needed for creating the entity keys to stich the results of the split queries together

   The methods getRelativePath() and getAbsolutePath() need to replace the getFullPath()

   Maybe create 2 split strategies:
   1. At root
        getRelativePath - will return the full path
        getAbsolutePath - will return the full path
   2. At fork
        getRelativePath - will return the path from the QueryTree root
        getAbsolutePath - will return the path from the AggregateTree root

   Test both strategies and also the stitch functionality
   Parallelizing them will be the next step

   How do splits and query filters interact. Need to ensure the stitching filters out the object correctly.

   Alias interquery flag??

3. Multi-column foreign key test
4. Entity filter test. Also need to test it in split scenario. See #2.
5. Inheritance test
6. Paging test
7. Test same entity type but on different anchors and ensure it brings different shapes
8. Batch test of UPDATE/INSERT/DELETE
9. Version check or snapshot values check if version column not present.
10. For troubleshooting turn off batching


XOR - Light weight ORM
  Provides OOTB Object model over a relational database.
  Facilitates complex object queries and efficient object saves.
  Useful for rapid prototyping (RAD) as a Java model is avoided.
  i.e., development speed is accelerated by having a 2-tier development model but with a 3-tier
  deployment model.

HSQLDB testing -
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.3/hsqldb-2.3.3.jar org.hsqldb.util.DatabaseManagerSwing

0. Testing enhancement - The below feature assists with testing various data models without 
     an explicit Java model hierarchy 

   Helps with testing - especially aliases
   and also expands scope of what it can support.

   Details
   =======
1) JDBC Persistence -
   the getId of JDBCPersistenceOrchestrator should refer to the original object passed by the user.
   The original object is used to populate the persistent object cache. All original objects should have ids. Else it is an error.
   if the id of a modified object cannot be found, it is considered as a transient object and will be created.

   Create
   - Iterate through the BusinessObjects in the object creator
   - If the object is a transient instance then it needs to be created.
     NOTE: A transient instance might have a user provided id. If not, the id generator associated with its type needs to be used
   - sort all the transient instances
   - Create INSERT statements

   Update/Delete
   - JDBCObjectPersister takes the Action objects and creates the UPDATE and DELETE statements

   Two ways of executing the above set of statements:
   1. Batching using explicit SQL - addBatch(sql) dates are specified using CAST('str', datetime) etc... 
         strings are validated and embedded apostrophe's are escaped accordingly, i.e., replace a single ' with ''
   2. Batching using prepared statement

   The advantage of #1 is that it reduces the number of round trips
   The advantage of #2 is that it can more effectively use the SQL cache.

2) A special _PARENT_ property/relationship is created for foreign keys between the primary keys of 2 tables


0. Type narrowing of a property
   NarrowHandler(options)
   options include
   - SUBCLASS NONE (no sub classes included)
   - SUBCLASS ANY (default behavior)
   - SUBCLASS a
   - SUBCLASS (a, b)
   - SUBCLASS NOT b 
   - SUBCLASS NOT (a, b)
  
1. Process ALIAS and NARROW functions
  How is an alias field processed? Add a property to an open type
  StateTree#extend
    a) Alias test of an existing property e.g., ALIASEXISTING
       taskDetails t1
       taskDetails t2
    b) Alias test of a non-existing property e.g., root

2. QueryTree reconstitution from View with children
   with children that are fragments and disjoint queries
   inline and named child views (fragments)

3. SP multi query ordering

4. include/skip functionality

