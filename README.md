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
   open types?? No JPA support. Just query DB using JDBC and create schema based on foreign keys and table names
   Need to provide table name to entity name mapping and
   Column name to property name mapping
   No inheritance support

   Helps with testing - especially aliases
   and also expands scope of what it can support.

   Details
   =======
   JDBC Persistence -
   Collection update/add/delete

   Querying -
   Support querying using OQL
   Create JDBCProvider to convert OQL to SQL queries

   extract Domain java object from JSON
   call save on a java object - extract JSON from domain and persist using version column
   Need to pass the original objects, so a snapshot can be taken and the original version identified.

   3. A special _PARENT_ property/relationship is created for foreign keys between the primary keys of 2 tables



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
