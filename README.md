XOR - Light weight ORM
  XOR is a light weight ORM, that can work directly against a relational database.
  It helps to quickly prototype a Single page application developed using javascript.

  The flow is:
  1. Develop the schema in an RDBMS with the necessary tables, indexes and foreign key constraints
  2. Configure XOR with this database schema
  3. Deploy it against a servlet container and expose a REST endpoint to it

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

2) Querying -
   Support querying using OQL
   Create JDBCProvider to convert OQL to SQL queries

3) A special _PARENT_ property/relationship is created for foreign keys between the primary keys of 2 tables



1. Process ALIAS and NARROW functions
  How is an alias field processed? Add a property to an open type
  StateTree#extend
    a) Alias test of an existing property e.g., ALIASEXISTING
       taskDetails t1
       taskDetails t2
    b) Alias test of a non-existing property e.g., root

2. Test that uses 2 QueryPieces  (Subquery or IN clause) - QueryTreeInvocation.java
   for e.g., Task + children
   and another query for children (i.e., grand children)
   Evaluate how 2 query pieces are evaluated and the objects reconsituted
   - Alias interQuery flag set to true

3. QueryTree reconstitution from View with children
   with children that are fragments and disjoint queries
   inline and named child views (fragments)

4. SP multi query ordering

5. include/skip functionality


parent
QueryType {
  basedOn: null
  a: t1
  b: t2
  c: t3
}

split into ->

QueryType {
 basedOn: t1
 a: t1
}

QueryType {
 basedOn: t2
 a: t2
}

QueryType {
 basedOn: t3
 a: t3
}
