XOR - Fast object retrieval and persistence for JPA

0. open types?? No JPA support. Just query DB using JDBC and create schema based on foreign keys and table names
   Need to provide table name to entity name mapping and
   Column name to property name mapping
   No inheritance support

   Helps with testing - especially aliases
   and also expands scope of what it can support.

   JDBCType - contains table name, needs to extend EntityType

   JDBCQuery - join syntax has two containment behavior (CASCADE DELETE)
   1. If the foreign key is between 2 tables connecting their primary keys
      then that relationship represents an inheritance relationship
      NOTE: A foreign key between two primary keys is supported on both
            Oracle and HANA
   2. Else it represents a containment relationship between 2 entities


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
