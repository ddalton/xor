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

import java.util.List;

/**
 * Goes through each QueryTree and executes it serially.
 */
public class SerialDispatcher extends AbstractDispatcher
{
    public SerialDispatcher(AggregateTree<QueryTree, InterQuery<QueryTree>> at, ObjectResolver resolver, CallInfo callInfo) {
        super(at, resolver, callInfo);
    }

    /*
     * Execute queries using BFS traversal
     */
    @Override
    protected void executeQueries(List<QueryTree> queries, QueryTreeInvocation queryInvocation) {
        PersistenceOrchestrator po = callInfo.getSettings().getPersistenceOrchestrator();

        while(!queries.isEmpty()) {
            QueryTree queryTree = queries.remove(0);

            // At this point the actual Query object is
            // created from the query string that is part of the query tree
            Query query = queryTree.createQuery(po);

            executeQuery(this, query, queryTree, queryInvocation);

            queries.addAll(aggregateTree.getChildren(queryTree));
        }
    }
}
