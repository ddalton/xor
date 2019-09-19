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

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ReconstituteRecordVisitor;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.ClassUtil;
import tools.xor.util.InterQuery;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Goes through each QueryTree and executes it serially.
 */
public class SerialDispatcher implements QueryDispatcher
{
    private AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;
    private ObjectResolver resolver;
    private CallInfo callInfo;

    public SerialDispatcher(AggregateTree<QueryTree, InterQuery<QueryTree>> at, ObjectResolver resolver, CallInfo callInfo) {
        this.aggregateTree = at;
        this.resolver = resolver;
        this.callInfo = callInfo;
    }

    @Override public void execute ()
    {
        List<QueryTree> queries = new LinkedList<>();
        queries.addAll(aggregateTree.getRoots());
        Set<QueryTree> roots = new HashSet<>(queries);
        QueryTreeInvocation queryInvocation = new QueryTreeInvocation(queries);

        // execute queries
        executeQueriesBFS(queries, queryInvocation);

        try {
            // reconstitute results
            queries.addAll(aggregateTree.getRoots());
            while (!queries.isEmpty()) {
                QueryTree queryTree = queries.remove(0);
                reconstitutePreorderDFS(queryTree, roots, queryInvocation);
            }
        } catch (Exception e) {
            throw ClassUtil.wrapRun(e);
        }

        resolver.postProcess();
    }

    private void executeQueriesBFS(List<QueryTree> queries, QueryTreeInvocation queryInvocation) {
        while(!queries.isEmpty()) {
            QueryTree queryTree = queries.remove(0);

            Query query = queryTree.prepare(callInfo, resolver);
            if(query != null) {
                List records = query.getResultList(null, callInfo.getSettings());

                queryInvocation.start(aggregateTree, queryTree);
                Map<String, Object> previous = null;
                for (Object record : records) {
                    previous = queryTree.resolveField(
                        null, // Not reconstituting at this phase
                        (Object[])record,
                        previous,
                        queryInvocation);
                }
                queryInvocation.finish(aggregateTree, queryTree);

                // Now update the dependent queries
                for(InterQuery<QueryTree> outEdge: aggregateTree.getOutEdges(queryTree)) {
                    queryInvocation.resolveQuery(aggregateTree, outEdge);
                }

                // Rebuild the dependent queries
                // Now update the dependent queries
                for(InterQuery<QueryTree> outEdge: aggregateTree.getOutEdges(queryTree)) {
                    PersistenceOrchestrator po = callInfo.getSettings().getPersistenceOrchestrator();
                    Query childQuery = outEdge.getEnd().getQuery();
                    po.evaluateDeferred(childQuery, Query.getQueryType(childQuery), queryInvocation);
                }
            }

            queries.addAll(aggregateTree.getChildren(queryTree));
        }
    }

    private void reconstitutePreorderDFS (QueryTree queryTree,
                                          Set<QueryTree> roots,
                                          QueryTreeInvocation queryInvocation) throws
        Exception
    {
        boolean isRoot = roots.contains(queryTree);

        // pre-order traversal
        for (QueryTree childQuery : aggregateTree.getChildren(queryTree)) {
            reconstitutePreorderDFS(childQuery, roots, queryInvocation);
        }

        // Check if there are any results to process
        List<QueryTreeInvocation.RecordDelta> recordDeltas = queryInvocation.getRecordDeltas(queryTree);
        if(recordDeltas == null) {
            return;
        }

        // Process the results
        for (QueryTreeInvocation.RecordDelta delta : recordDeltas) {
            Object[] record = delta.getRecord();
            BusinessObject anchorObject = queryTree.getRootObject(
                record,
                (BusinessObject)callInfo.getOutput(),
                queryInvocation);

            Map<String, Object> propertyResult = delta.getPropertyResult();
            ReconstituteRecordVisitor collectionReconstitutor = new ReconstituteRecordVisitor();
            for (String propertyPath : delta.getChanged()) {
                // Set the value and create any intermediate objects if necessary
                anchorObject.reconstitute(
                    propertyPath,
                    propertyResult,
                    queryTree,
                    collectionReconstitutor,
                    queryInvocation);
            }
            String lcp = queryTree.getDeepestCollection(delta.getLCP());
            collectionReconstitutor.process(lcp);

            // Notify the resolver of the object
            resolver.notify(anchorObject, isRoot);
        }
    }
}
