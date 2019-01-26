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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.Constants;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.view.expression.AscHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates the literal OQL QueryString for each QueryPiece in a QueryTree.
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

    private QueryTree<QueryPiece, InterQuery<QueryPiece>> queryTree;
    private BusinessObject entity; // Used to get id, in future for Query By Example

    public QueryBuilder(QueryTree queryTree) {
        this(queryTree, null);
    }

    public QueryBuilder(QueryTree queryTree, BusinessObject entity) {
        this.queryTree = queryTree;
        this.entity = entity;
    }

    public QueryTree<QueryPiece, InterQuery<QueryPiece>> getQueryTree() {
        return this.queryTree;
    }

    public BusinessObject getEntity() {
        return this.entity;
    }

    /**
     * Constructs the OQL query for each QueryPiece and populates it with it.
     * We do it in a BFS traversal.
     */
    public void construct(Settings settings) {
        List<QueryPiece> queries = new LinkedList<>();
        queries.add(queryTree.getRoot());

        while(!queries.isEmpty()) {
            QueryPiece qp = queries.remove(0);
            queries.addAll(this.queryTree.getChildren(qp));

            // construct the query and set it one the qp
            construct(settings, qp);
        }
    }

    /**
     * Get the correct Builder strategy object for the QueryPiece if present, else get it
     * for the view.
     *
     * @param qp for which the builder strategy object is returned
     * @param view fallback to view if the QueryPiece is not provided
     * @param builder responsible for constructing the queries
     * @return builder startegy object
     */
    public static QueryBuilderStrategy getBuilderStrategy(QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp, View view, QueryBuilder builder) {
        view = qp != null ? qp.getView() : view;

        if(view.getStoredProcedure(AggregateAction.READ) != null) {
            return new QueryFromSP(view);
        } else if(view.getNativeQuery() != null) {
            return new QueryFromSQL(view);
        } else if(view.getUserOQLQuery() != null) {
            return new QueryFromOQL(view);
        } else {
            if(qp != null) {
                return new QueryFromFragments(qp, builder);
            }
        }

        return null;
    }

    /**
     * Construct the OQL query for a QueryPiece, and initialize the QueryPiece with it.
     *
     * @param settings provided by the user
     * @param qp QueryPiece for which we need to construct a query
     */
    public void construct(Settings settings, QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {

        QueryBuilderStrategy strategy = getBuilderStrategy(qp, null, this);

        Query query = strategy.construct(settings);
        qp.setQuery(query);
    }
}
