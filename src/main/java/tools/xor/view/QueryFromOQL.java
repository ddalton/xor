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

import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator;

public class QueryFromOQL implements QueryBuilderStrategy
{
    private final View view;
    private final QueryPiece queryPiece;
    private final QueryTree queryTree;

    // TODO: construct QueryFields and set the QueryPiece with it
    // Will use QueryPiece#generateFields but modified to handle augmenter

    public QueryFromOQL (View view, QueryPiece queryPiece, QueryTree queryTree)
    {
        this.view = view;
        this.queryPiece = queryPiece;
        this.queryTree = queryTree;
    }

    @Override public Query construct (Settings settings)
    {
        /* We first need to build the SQL from the following
         *   selectClause
         *   freestyle functions
         */
        OQLQuery oqlQuery = view.getUserOQLQuery();
        StringBuilder queryString = new StringBuilder(oqlQuery.getSelectClause());

        queryString.append(
            QueryStringHelper.getFilterClause(
                settings,
                oqlQuery.getFunction()));

        Query query = settings.getPersistenceOrchestrator().getQuery(
            queryString.toString(),
            PersistenceOrchestrator.QueryType.OQL,
            oqlQuery,
            settings);

        // Initialized the selected columns
        oqlQuery.deriveColumns(this.queryPiece, query, settings, this.queryTree, this.view);

        return query;
    }
}
