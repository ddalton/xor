XOR - Fast object retrieval and persistence for JPA


1. Process ALIAS and NARROW functions
  How is an alias field processed? Add a property to an open type
  StateTree#extend

2. Test that uses 2 QueryPieces  (Subquery or IN clause) - QueryTreeInvocation.java
   for e.g., Task + children
   and another query for children (i.e., grand children)
   Evaluate how 2 query pieces are evaluated and the objects reconsituted


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
