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

import tools.xor.CallInfo;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.InterQuery;

import java.util.LinkedList;
import java.util.List;

/**
 * Goes through each QueryTree and executes it.
 * The walk is done in a BFS manner.
 */
public class SerialDispatcher implements QueryDispatcher
{
    @Override public void execute (AggregateTree<QueryTree, InterQuery<QueryTree>> at, ObjectResolver resolver, CallInfo callInfo)
    {
        List<QueryTree> queries = new LinkedList<>();
        queries.addAll(at.getRoots());

        QueryTreeInvocation queryInvocation = new QueryTreeInvocation();
        while(!queries.isEmpty()) {
            QueryTree queryTree = queries.remove(0);

            Query query = queryTree.prepare(callInfo, resolver);
            if(query != null) {
                List records = query.getResultList(null, callInfo.getSettings());
                queryInvocation.start(at, queryTree);
                resolver.processRecords(records, queryTree, callInfo, queryInvocation);
                queryInvocation.finish(at, queryTree);

                // Now update the dependent queries
                for(InterQuery<QueryTree> outEdge: at.getOutEdges(queryTree)) {
                    queryInvocation.resolveQuery(at, outEdge);
                }

                // Rebuild the dependent queries
                // Now update the dependent queries
                for(InterQuery<QueryTree> outEdge: at.getOutEdges(queryTree)) {
                    PersistenceOrchestrator po = callInfo.getSettings().getPersistenceOrchestrator();
                    Query childQuery = outEdge.getEnd().getQuery();
                    po.evaluateDeferred(childQuery, Query.getQueryType(childQuery), queryInvocation);
                }
            }

            queries.addAll(at.getChildren(queryTree));
        }

        resolver.postProcess();
    }
}
