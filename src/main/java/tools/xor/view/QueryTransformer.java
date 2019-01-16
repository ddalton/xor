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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator.QueryType;
import tools.xor.util.InterQuery;

/**
 *
 * The QueryTransformer currently flattens the QueryTree and works off ColumnMeta and the attributePath.
 * This needs to be refactored and built directly from the root QueryPiece of the original
 * QueryTree instance.
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
 * The QueryTree data structure is a tree of a tree
 * QueryTree -> QueryPiece -> QueryFragment
 *
 * QueryTree
 *   A QueryTree consists of a tree of QueryPiece nodes connected by InterQuery edges.
 *   It represents how all the data in a view can be retrieved just using queries.
 *
 * QueryPiece
 *   A QueryPiece consists of a tree of QueryFragment nodes connected by IntraQuery edges.
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
 *   This is an Edge in a tree of QueryPiece nodes that shows how 2 QueryPiece instances
 *   are connected.
 *   It might have additional details on how the dependent query needs to be executed with
 *   information provided by the depended upon query. For example, either through a sub-query
 *   or an IN list expression.
 *
 * IntraQuery
 *   Describes the foreign key relationship between 2 entities.
 *   It is an Edge in a tree of QueryFragment nodes that form a QueryPiece.
 *
 *
 * Algorithm:
 * ----------
 * 1. QueryFragment builder
 *    Initially the QueryTree consists of all the property paths.
 *    It is made up of a single QueryPiece, that contains a tree data structure
 *    made up of QueryFragment nodes and InterQuery edges for all the property paths.
 *    REF:
 *     FragmentBuilder
 * 2. QueryTree modification for efficient queries
 *    This is analyzed and the necessary QueryPiece instances created.
 *    REF:
 *      CartesianJoinSplitter,
 *      NestedJoinSplitter,
 *      LoopSplitter
 * 3. QueryTree modification for user functionality
 *    We modify the query to account for user features such as:
 *      AliasSplitter
 *      QueryTrimmer
 * 4. Function
 *    Add necessary functions to the QueryPiece and the placeholders in the QueryFragments
 *      setParameters
 * 5. Validation
 *    Based on the user input like ordering and paging, if it spans across
 *    queries then we need to error out while suggesting how the user can rectify the issue.
 *    REF:
 *      SortValidator
 *      PageValidator
 * 6. Execution
 *    Then each QueryPiece has its query built and executed in the order specified by
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
 * AliasSplitter
 * -------------
 * This class creates a new QueryFragment with the alias name.
 * A single property can be represented with multiple fragments if there are multiple aliases.
 *
 * QueryTrimmer
 * ------------
 * Based on the skip and include filters, the copy of the QueryTree is trimmed before it
 * is executed
 *
 * LoopSplitter
 * ------------
 * If a loop is detected, then it is the responsibility of the client to provide a
 * LoopResolver for that edge.
 * The loop resolver can be specified for the property in the join edge by the following API:
 * property.addLoopResolver(resolver)
 * The resolver has the following API:
 *   String unrollTo(int level)
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
 * Generates the literal OQL QueryString from a QueryPiece.
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
 * Uses a QueryPiece with an IntraQuery edge that contains alias information,
 * useful for queries.
 * Also, IntraQuery instances can have a custom implementation of the generated OQL query string.
 *
 * In the below example the IntraQuery instances contain the alias, name and other information:
 *
 * QueryPiece
 *   _path
 *
 *     S1[E0]                     S2[E1]
 *    -------------               --------
 *   | name        | ----------> | count  |
 *   | description |   details    --------
 *    -------------
 *
 *  _path is null for the root QueryTree, but is the ancestor path for the InterQuery edge
 *  where this QueryPiece attaches. All input property names are normalized using this (made
 *  relative).
 *
 * S1 and S2 are instance of the QueryFragment class. E0 and E1 are query aliases.
 *
 * This QueryPiece produces the following OQL query:
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
 * e0.getOuterJoin() -> "LEFT OUTER JOIN class2 E1 ON E1.id = E0.details"
 *
 * QueryFragment s2;
 * s2.getSelectList() -> "E1.count"
 *
 * The modified QueryPiece (with IntraQuery edges) will select additional columns to help with
 * stitching results from different QueryPiece instances (broken for optimization reasons)
 * into the desired object.
 * These properties will be marked as not fetched.
 *
 * QueryPiece.ColumnMeta { position, fetch, name }
 *
 * ColumnMeta is used to reconstruct the result object from the ResultSet.
 * If this QueryPiece represents the target of an InterQuery edge, then it would contain the
 * information populated from the ResultSet to resolve the foreign key to the source of this edge.
 *
 * PAGING
 * ======
 * When pageColumn information is provided, we validate that the column indeed exists
 * in the QueryTree before adding this information into the query
 *
 * SORTING
 * =======
 * When ordering information is provided, we validate that the column indeed exists in the
 * QueryTree before adding this information into the query
 *
 * FILTERING
 * =========
 * Add the necessary WHERE conditions based on the provided filters
 *
 * Logging
 * =======
 * print - Print both the QueryPiece graph structure and the related OQL fragments produced by
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
		//		First check for a StoredProcedure query, then a SQL query 
		//		When retrieving the QueryBuilder instance, registered SQL queries are given preference over HQL/JPQL queries.
		//
		//		It is the responsibility of the user to ensure that the registered SQL is ANSI compliant 
		//		and can work with all the databases that the user uses.
		
		if(view != null && view.getStoredProcedure() != null) {
			StoredProcedure querySP = view.getStoredProcedure(settings.getAction());

			if(querySP != null) {
				return settings.getPersistenceOrchestrator().getQuery(
					querySP.getName(),
					QueryType.SP,
					querySP,
					settings);
			}
		}

		if(view != null && view.getNativeQuery() != null) {
			String queryString = view.getNativeQuery().getQueryString();
			queryString.replaceAll("[\n\r]", "");
			return settings.getPersistenceOrchestrator().getQuery(queryString, QueryType.SQL, view.getNativeQuery(), settings);
		}

		// User OQL
		if(view != null && view.getUserOQLQuery() != null) {
			String oqlString = view.getUserOQLQuery().getQueryString();
			return settings.getPersistenceOrchestrator().getQuery(oqlString, QueryType.OQL, view.getUserOQLQuery(), settings);
		}

		// System OQL
		QueryTree<QueryPiece, InterQuery<QueryPiece>> queryTree = new QueryTree(view);
		QueryBuilder qb = new QueryBuilder(queryTree);
		qb.construct(settings);

		return queryTree.getRoot().getQuery();
	}
}
