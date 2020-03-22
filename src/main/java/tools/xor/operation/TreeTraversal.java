/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.operation;

/**
 * Operations that work on a QueryTree instance.
 * Uses BFS (Breadth First Search) to navigate and execute the queries from the QueryTree.
 *
 * The main difference between TreeTraversal and GraphTraversal operations is the fact
 * that GraphTraversal supports recursive queries.
 * TreeTraversal can support recursive queries if they are unrolled. For example,
 * a recursive relationship that needs to be queried to a depth of 3 will be unrolled as
 * a.a.a
 * Because of this restriction, we can optimize TreeTraversal operations to execute
 * faster than GraphTraversal operations.
 * We can also optimize GraphTraversal operations to execute faster if the underlying
 * DB supports it and we create a custom SQL for the named view utilizing that DB optimization.
 * 
 * There are two ways the RequestSlice nodes of a QueryTree are constructed:
 * 1. LEAF_GROUP partitioning
 *    We flatten the request by fully qualifying a property starting from the root
 *    and then we divide the list into multiple RequestSlice instances so that a cartesian product
 *    is avoided.
 * 2. STATE_TREE partitioning
 *    We use the StateTree as a guide to create the RequestSlice
 *    This typically creates more RequestSlice instances, but is easier to assemble the result. 
 * 
 * Takes an AggregateView as input and creates a QueryTree instance from it.
 * It then proceeds to execute this query and assemble the result.
 * There are 2 ways the assembly is done:
 * 1. Flattened result. Currently only LEAF_GROUP supports this.
 * 2. Nested result shaped in the form of the AggregateView's StateTree
 *    Both LEAF_GROUP and STATE_TREE support this result structure.
 * 
 * The other difference is that TreeTraversal has lower overhead since
 * there is no copying to a DTO. The object reconstituted from the query result
 * is directly returned to the user.
 * 
 * @author Dilip Dalton
 *
 */
public abstract class TreeTraversal extends AbstractOperation {
}
