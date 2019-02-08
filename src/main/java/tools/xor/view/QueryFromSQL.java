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

import java.util.LinkedList;
import java.util.List;

public class QueryFromSQL implements QueryBuilderStrategy
{
    private final View view;
    private final QueryPiece queryPiece;
    private final QueryTree queryTree;

    public QueryFromSQL (View view, QueryPiece queryPiece, QueryTree queryTree)
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
        NativeQuery nativeQuery = view.getNativeQuery();
        StringBuilder queryString = new StringBuilder(nativeQuery.getSelectClause());

        List<BindParameter> relevantParams = new LinkedList<>();
        queryString.append(
            QueryStringHelper.getFilterClause(
                settings,
                nativeQuery.getFunction(),
                nativeQuery.getParameterList(),
                relevantParams));

        Query query = settings.getPersistenceOrchestrator().getQuery(
            queryString.toString(),
            PersistenceOrchestrator.QueryType.SQL,
            nativeQuery,
            settings);

        query.updateParamMap(relevantParams);

        // Initialized the selected columns
        nativeQuery.deriveColumns(this.queryPiece, query, settings, this.queryTree, this.view);

        return query;
    }
}
