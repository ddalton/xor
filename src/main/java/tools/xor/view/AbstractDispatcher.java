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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ReconstituteRecordVisitor;
import tools.xor.service.DataStore;
import tools.xor.util.ClassUtil;
import tools.xor.util.InterQuery;

/**
 * Goes through each QueryTree and executes it serially.
 */
public abstract class AbstractDispatcher implements QueryDispatcher
{
    protected AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;
    protected ObjectResolver resolver;
    protected CallInfo callInfo;

    public AbstractDispatcher(AggregateTree<QueryTree, InterQuery<QueryTree>> at, ObjectResolver resolver, CallInfo callInfo) {
        this.aggregateTree = at;
        this.resolver = resolver;
        this.callInfo = callInfo;
    }

    abstract protected void executeQueries(List<QueryTree> queries, QueryTreeInvocation queryInvocation);

    @Override public void execute ()
    {
        List<QueryTree> queries = new LinkedList<>();
        queries.addAll(aggregateTree.getRoots());
        QueryTreeInvocation queryInvocation = new QueryTreeInvocation();

        // execute queries
        // Done in a top-down traversal of the AggregateTree starting at the root(s)
        executeQueries(queries, queryInvocation);

        /*
         * The reconstitution rules are:
         * 1. Convert NFA to DFA
         *    i.e., process all QueryTrees that are the subtype in an inheritance relationship
         *          first, so all unlabelled edges are processed
         * 2. Perform top-down processing from the root of the aggregate tree
         *    on the remaining relationships (labelled edges)
         */
        Reconstitutor reconstitutor = new Reconstitutor(this.aggregateTree,
            resolver,
            callInfo,
            queryInvocation);
        aggregateTree.reconstitute(reconstitutor);

        resolver.postProcess();
    }

    private static class Reconstitutor implements ReconstituteVisitor {

        protected AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree;
        protected ObjectResolver resolver;
        protected CallInfo callInfo;
        protected Set<QueryTree> roots;
        protected QueryTreeInvocation queryInvocation;

        public Reconstitutor(AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree,
                             ObjectResolver resolver,
                             CallInfo callInfo,
                             QueryTreeInvocation queryTreeInvocation) {
            this.aggregateTree = aggregateTree;
            this.resolver = resolver;
            this.callInfo = callInfo;
            this.queryInvocation = queryTreeInvocation;

            roots = new HashSet<>();
            roots.addAll(aggregateTree.getRoots());
        }

        @Override public void visit (Object node, boolean isSubtype)
        {
            QueryTree queryTree = (QueryTree) node;
            boolean isRoot = roots.contains(queryTree);

            // Check if there are any results to process
            List<QueryTreeInvocation.RecordDelta> recordDeltas = queryInvocation.getRecordDeltas(queryTree);
            if(recordDeltas == null) {
                return;
            }

            try {
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
            catch (Exception e) {
                throw ClassUtil.wrapRun(e);
            }
        }
    }

    /*
     * This is a static method since this is also called from the ParallelDispatcher static inner class
     */
    /**
     * This is a static method since this is also called from the ParallelDispatcher static inner class
     * It is responsible for executing the query. The object construction is done in a different step -
     * reconstitution
     * 
     * @param disptacher serial or parallel 
     * @param query to be executed
     * @param queryTree modelling the query
     * @param queryInvocation holding the results of the query executions
     * @return true if execution was successful, false otherwise
     */
    protected static boolean executeQuery (AbstractDispatcher disptacher,
                                        Query query,
                                        QueryTree queryTree,
                                        QueryTreeInvocation queryInvocation) {

        AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = disptacher.aggregateTree;
        CallInfo callInfo = disptacher.callInfo;
        ObjectResolver resolver = disptacher.resolver;

        InterQuery parentEdge = null;
        if(query != null) {
            // get the parent edge if present
            Iterator<InterQuery<QueryTree>> iter = aggregateTree.getInEdges(queryTree).iterator();
            if (iter.hasNext()) {
                parentEdge = iter.next();

                DataStore po = callInfo.getSettings().getDataStore();
                
                // At this point we check if we need to proceed further depending on whether the parent
                // query produced any results
                Set parentIds = queryInvocation.getParentIds(parentEdge);
                if(parentIds == null || parentIds.isEmpty()) {
                    return false;
                }
                queryInvocation.resolveQuery(parentEdge);

                if(query.isDeferred()) {
                    query.setQueryString(query.extractParameters());
                }
                po.evaluateDeferred(query, Query.getQueryType(query), queryInvocation);
            }
        }

        List records =  null;
        queryTree.prepare(callInfo, resolver, queryInvocation, parentEdge);
        View view = queryTree.getView();
        if(view instanceof AggregateView) {
            records = ((AggregateView)view).getResults();
        }
        if(query != null && records == null) {
            records = query.getResultList(queryTree.getView(), callInfo.getSettings());
        }

        // Check if this is a single column result
        if(records.size() > 0) {
            if(!records.get(0).getClass().isArray()) {
                throw new RuntimeException("Was the identifier column forgotten to be added to the subtype query?");
            }
        }

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

        // execute actions
        List<Action> actions = queryTree.getActions();
        for(Action action: actions) {
            action.execute(disptacher, queryInvocation, callInfo.getSettings().getDataStore());
        }
        
        return true;
    }
}
