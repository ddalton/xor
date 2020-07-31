/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

import java.util.LinkedList;
import java.util.List;

import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.Settings;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;

/**
 * Generates the literal OQL QueryString for each QueryTree in an AggregateTree.
 * Binds it with the provided parameters.
 * Two types of queries are possible depending upon the type of object resolution desired:
 * 1. Shared
 * 2. Distinct
 * @see tools.xor.view.ObjectResolver.Type
 *
 */

public class QueryBuilder
{
    public static final String SELECT_CLAUSE = "SELECT ";
    public static final String COMMA_DELIMITER = ", ";
    public static final String AS_CLAUSE = " AS ";

    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;
    private BusinessObject entity; // Used to get id, in future for Query By Example

    public QueryBuilder(AggregateTree aggregateTree) {
        this(aggregateTree, null);
    }

    public QueryBuilder(AggregateTree aggregateTree, BusinessObject entity) {
        this.aggregateTree = aggregateTree;
        this.entity = entity;
    }

    public AggregateTree<QueryTree, InterQuery<QueryTree>> getAggregateTree () {
        return this.aggregateTree;
    }

    public BusinessObject getEntity() {
        return this.entity;
    }

    /**
     * Constructs the OQL query for each QueryTree and populates it with it.
     * We do it in a BFS traversal.
     *
     * Query construction is done after the QueryTree(s) have been created,
     * including the necessary splits.
     * 
     * @param settings object
     */
    public void construct(Settings settings) {

        List<QueryTree> queries = new LinkedList<>();
        queries.addAll(aggregateTree.getRoots());

        while(!queries.isEmpty()) {
            QueryTree queryTree = queries.remove(0);
            queries.addAll(this.aggregateTree.getChildren(queryTree));

            // construct the query and set it on the qp
            construct(settings, queryTree);
        }
    }

    /**
     * Get the correct Builder strategy object for the QueryTree if present, else get it
     * for the view.
     *
     * @param queryTree for which the builder strategy object is returned
     * @param view fallback to view if the QueryTree is not provided
     * @param builder responsible for constructing the queries
     * @param aggregateTree containing queries for the view
     * @return builder startegy object
     */
    public static QueryBuilderStrategy getBuilderStrategy(QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree, View view, QueryBuilder builder, AggregateTree aggregateTree) {
        view = queryTree != null ? queryTree.getView() : view;

        if(view.getStoredProcedure(AggregateAction.READ) != null || view.getResultPosition() != null) {
            return new QueryFromSP(view, queryTree, aggregateTree);
        } else if(view.getNativeQuery() != null) {
            return new QueryFromSQL(view, queryTree, aggregateTree);
        } else if(view.getUserOQLQuery() != null) {
            return new QueryFromOQL(view, queryTree, aggregateTree);
        } else {
            if(queryTree != null) {
                return new QueryFromFragments(queryTree, builder);
            }
        }

        return null;
    }

    /**
     * Construct the OQL query for a QueryTree, and initialize the QueryTree with it.
     *
     * @param settings provided by the user
     * @param queryTree QueryTree for which we need to construct a query
     */
    public void construct(Settings settings, QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree) {

        assert(queryTree != null);

        QueryBuilderStrategy strategy = getBuilderStrategy(queryTree, null, this, this.aggregateTree);

        QueryHandle handle = strategy.construct(settings);
        queryTree.setQueryHandle(handle);
    }
}
