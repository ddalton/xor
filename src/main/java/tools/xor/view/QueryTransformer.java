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

package tools.xor.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.util.InterQuery;

/**
 *
 * The QueryTransformer currently flattens the AggregateTree and works off ColumnMeta and the attributePath.
 * This needs to be refactored and built directly from the root QueryTree of the original
 * AggregateTree instance.
 * This approach gives the developer more flexibility in customizing the query and making it
 * more powerful (support hierarchical queries) and reliable (easier to test).
 *
 * DESIGN
 * ======
 *
 * Before we look at the design let us get familiar with the terminology used.
 *
 * Glossary:
 * ---------
 * The AggregateTree data structure is a tree of a tree
 * AggregateTree - QueryTree - QueryFragment
 *
 * AggregateTree
 *   A AggregateTree consists of a tree of QueryTree nodes connected by InterQuery edges.
 *   It represents how all the data in a view can be retrieved just using queries.
 *
 * QueryTree
 *   A QueryTree consists of a tree of QueryFragment nodes connected by IntraQuery edges.
 *   It represents a complete query that can be executed on a relational DB.
 *
 * QueryFragment
 *   A QueryFragment consists of all the pieces of information the user would like to
 *   retrieve from an entity.
 *   Each QueryFragment represents a distinct entity in the path.
 *   Embedded entities do not form a separate fragment but are coalesced into the owning entity.
 *
 * InterQuery
 *   It shows the dependency between 2 queries.
 *   This is an Edge in a tree of QueryTree nodes that shows how 2 QueryTree instances
 *   are connected.
 *   It might have additional details on how the dependent query needs to be executed with
 *   information provided by the depended upon query. For example, either through a sub-query
 *   or an IN list expression.
 *
 * IntraQuery
 *   Describes the foreign key relationship between 2 entities.
 *   It is an Edge in a tree of QueryFragment nodes that form a QueryTree.
 *
 *
 * Algorithm:
 * ----------
 * 1. QueryFragment builder
 *    Initially the AggregateTree consists of all the property paths.
 *    It is made up of a single QueryTree, that contains a tree data structure
 *    made up of QueryFragment nodes and InterQuery edges for all the property paths.
 *    REF:
 *     FragmentBuilder
 *       Should also take care of creating fragments for aliases, i.e., it needs
 *       to add QueryType for the root if needed and add the necessary external properties
 *       to the AggregateTree if needed.
 * 2. AggregateTree modification for efficient queries
 *    This is analyzed and the necessary QueryTree instances created.
 *    REF:
 *      CartesianJoinSplitter,
 *      NestedJoinSplitter,
 *      LoopSplitter
 * 3. AggregateTree modification for user functionality
 *    We modify the query to account for user features such as:
 *      QueryTrimmer
 * 4. Function
 *    Add necessary functions to the QueryTree and the placeholders in the QueryFragments
 *      setParameters
 * 5. Validation
 *    Based on the user input like ordering and paging, if it spans across
 *    queries then we need to error out while suggesting how the user can rectify the issue.
 *    REF:
 *      SortValidator
 *      PageValidator
 * 6. Execution
 *    Then each QueryTree has its query built and executed in the order specified by
 *    its dependency in the tree of InterQuery edges.
 *    REF:
 *      QueryBuilder
 *      QueryDispatcher
 *        SerialDispatcher
 *        ParallelDispatcher
 *      ObjectResolver
 *
 * NOTE: Each step should have its own unit tests
 *
 *
 * QueryTrimmer
 * ------------
 * Based on the skip and include filters, the copy of the AggregateTree is trimmed before it
 * is executed
 *
 * QueryConsolidator
 * -----------------
 * If there are multiple inline (child views) and named views (view references), then
 * the QueryTree constructed from them might have a single fragment in most cases. If so,
 * they can be rolled into the parent view.
 * Also, the functions will need to be rolled as well.
 *
 * SortValidator
 * -------------
 * Checks to see that the ordering clause does not span properties belonging to different queries.
 *
 * PageValidator
 * -------------
 * Checks to see that the paging clause does not span properties belonging to different queries.
 *
 * QueryBuilder
 * ------------
 * Generates the literal OQL QueryString from a QueryTree.
 * Binds it with the provided parameters.
 * Two types of queries are possible:
 *
 * 1. Objects with same ids are shared. The result will be a graph of information.
 *    To allow the identification of shareable objects, additional information will need to
 *    be fetched such as the id of the objects.
 * 2. Objects with same ids are not shared. This is faithful to what the customer requested.
 *    The data for the same object might occur multiple times in the result.
 *    The result becomes a tree of information.
 *
 * QueryDispatcher
 * ---------------
 * Based on the ordering specified by the InterQuery edges, the queries are executed using
 * the information from the depended upon query if applicable.
 *
 * A SerialDispatcher is used to execute the queries sequentially. Useful if they should all
 * belong to the same transaction.
 *
 * A ParallelDispatcher is used to execute all queries in the same level of the tree
 * in parallel. Useful if the data needs to be retrieved immediately and consistency can be
 * relaxed.
 *
 * ObjectResolver
 * --------------
 * Interface implemented by the appropriate query operation
 * For e.g.,
 * QueryOperation - Takes the ResultSet results and builds an Object graph out of it.
 * DenormalizedQueryOperation - Takes the result set and builds a table out of it, repeating the
 *  values of the parent object
 *
 * EXAMPLE
 * =======
 *
 *
 * Uses a QueryTree with an IntraQuery edge that contains alias information,
 * useful for queries.
 * Also, IntraQuery instances can have a custom implementation of the generated OQL query string.
 *
 * In the below example the IntraQuery instances contain the alias, name and other information:
 *
 * QueryTree
 *   _path
 *
 *     S1[E0]                     S2[E1]
 *    -------------               --------
 *   | name        | ---------- | count  |
 *   | description |   details    --------
 *    -------------
 *
 *  _path is null for the root AggregateTree, but is the ancestor path for the InterQuery edge
 *  where this QueryTree attaches. All input property names are normalized using this (made
 *  relative).
 *
 * S1 and S2 are instance of the QueryFragment class. E0 and E1 are query aliases.
 *
 * This QueryTree produces the following OQL query:
 *
 * SELECT E0.name, E0.description, E1.count FROM
 *   class1 E0 LEFT OUTER JOIN class2 E1 ON E1.id = E0.details
 *
 * There is a lot going on in this query. Let us break it down:
 * Both the IntraQuery and the QueryFragment produce OQL fragments. This information is taken
 * and the desired OQL query is built with additional information,
 * such as Ordering, paging and filters.
 *
 * Some of the APIs are:
 * IntraQuery e0;
 * e0.getOuterJoin() - "LEFT OUTER JOIN class2 E1 ON E1.id = E0.details"
 *
 * QueryFragment s2;
 * s2.getSelectList() - "E1.count"
 *
 * The modified QueryTree (with IntraQuery edges) will select additional columns to help with
 * stitching results from different QueryTree instances (broken for optimization reasons)
 * into the desired object.
 * These properties will be marked as not fetched.
 *
 * QueryTree.ColumnMeta { position, fetch, name }
 *
 * ColumnMeta is used to reconstruct the result object from the ResultSet.
 * If this QueryTree represents the target of an InterQuery edge, then it would contain the
 * information populated from the ResultSet to resolve the foreign key to the source of this edge.
 *
 * PAGING
 * ======
 * When pageColumn information is provided, we validate that the column indeed exists
 * in the AggregateTree before adding this information into the query
 *
 * SORTING
 * =======
 * When ordering information is provided, we validate that the column indeed exists in the
 * AggregateTree before adding this information into the query
 *
 * FILTERING
 * =========
 * Add the necessary WHERE conditions based on the provided filters
 *
 * Logging
 * =======
 * print - Print both the QueryTree graph structure and the related OQL fragments produced by
 *        the query tree
 *
 *
 *
 *
 * 
 * */


public class QueryTransformer
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public Query constructDML(View view, Settings settings) {

		Query userQuery = getUserQuery(view, settings);
		if(userQuery != null) {
			return userQuery;
		}

		// System OQL
		AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = new AggregateTree(view);
		new FragmentBuilder(aggregateTree).build((EntityType)settings.getEntityType());
		QueryBuilder qb = new QueryBuilder(aggregateTree);
		qb.construct(settings);

		return aggregateTree.getRoot().createQuery(settings.getDataStore());
	}

	public static Query getUserQuery(View view, Settings settings) {
		//		First check for a StoredProcedure query, then a SQL query
		//		When retrieving the QueryBuilder instance, registered SQL queries are given preference over HQL/JPQL queries.
		//
		//		It is the responsibility of the user to ensure that the registered SQL is ANSI compliant
		//		and can work with all the databases that the user uses.

		QueryBuilderStrategy strategy = QueryBuilder.getBuilderStrategy(null, view, null, null);
		if(strategy != null) {
			return strategy.construct(settings).create(settings.getDataStore());
		}

		return null;
	}
}
