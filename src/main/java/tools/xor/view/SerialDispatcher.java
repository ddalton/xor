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
 * Goes through each QueryPiece and executes it.
 * The walk is done in a BFS manner.
 */
public class SerialDispatcher implements QueryDispatcher
{
    @Override public void execute (QueryTree<QueryPiece, InterQuery<QueryPiece>> qt, ObjectResolver resolver, CallInfo callInfo)
    {
        QueryPiece qp = qt.getRoot();
        List<QueryPiece> queries = new LinkedList<>();
        queries.add(qp);

        QueryTreeInvocation queryInvocation = new QueryTreeInvocation();
        while(!queries.isEmpty()) {
            qp = queries.remove(0);

            Query query = qp.prepare(callInfo, resolver);
            if(query != null) {
                List records = query.getResultList(null, callInfo.getSettings());
                queryInvocation.start(qt, qp);
                resolver.processRecords(records, qp, callInfo, queryInvocation);
                queryInvocation.finish(qt, qp);

                // Now update the dependent queries
                for(InterQuery<QueryPiece> outEdge: qt.getOutEdges(qp)) {
                    queryInvocation.resolveQuery(qt, outEdge);
                }

                // Rebuild the dependent queries
                // Now update the dependent queries
                for(InterQuery<QueryPiece> outEdge: qt.getOutEdges(qp)) {
                    PersistenceOrchestrator po = callInfo.getSettings().getPersistenceOrchestrator();
                    Query childQuery = outEdge.getEnd().getQuery();
                    po.evaluateDeferred(childQuery, Query.getQueryType(childQuery), queryInvocation);
                }
            }

            queries.addAll(qt.getChildren(qp));
        }

        resolver.postProcess();
    }
}
