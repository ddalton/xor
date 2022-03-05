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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tools.xor.CallInfo;
import tools.xor.Settings;
import tools.xor.service.DataStore;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.InterQuery;

/**
 * Goes through each QueryTree and executes it serially.
 */
public class ParallelDispatcher extends AbstractDispatcher implements Callback
{
    private final static int QUERY_POOL_SIZE;
    static {
        int poolSize = 4;
        if (ApplicationConfiguration.config().containsKey(Constants.Config.QUERY_POOL_SIZE)) {
            poolSize = ApplicationConfiguration.config().getInt(Constants.Config.QUERY_POOL_SIZE);
            if(poolSize < 1) {
                poolSize = 4;
            }
        }

        QUERY_POOL_SIZE = poolSize;
    }      

    private static ExecutorService qe = Executors.newFixedThreadPool(QUERY_POOL_SIZE);
    private CountDownLatch latch;

    private void validate(AggregateTree<QueryTree, InterQuery<QueryTree>> at) {
        // Check that a stored procedure does not populate the temp table
        // as that temp table data cannot be seen by a different thread
        for(QueryTree vertex: at.getVertices()) {
            if(vertex.getView() != null && vertex.getView().isTempTablePopulated()) {
                throw new RuntimeException("The view creates data that cannot be seen from a different session");
            }
        }
    }

    public ParallelDispatcher(AggregateTree<QueryTree, InterQuery<QueryTree>> at, ObjectResolver resolver, CallInfo callInfo) {
        super(at, resolver, callInfo);

        validate(at);
        this.latch = new CountDownLatch(at.getVertices().size());
    }

    public void complete(QueryTree queryTree, QueryTreeInvocation queryTreeInvocation) {
        latch.countDown();

        // Execute the child queries
        for(InterQuery<QueryTree> edge: aggregateTree.getOutEdges(queryTree)) {
            QueryTree child = edge.getEnd();
            CallbackTask ct = new CallbackTask(new QueryTreeProcessor(child, queryTreeInvocation, this), this);
            qe.submit(ct);
        }
    }

    private static class QueryTreeProcessor implements Runnable {

        private final QueryTree queryTree;
        private final QueryTreeInvocation queryInvocation;
        private final AbstractDispatcher dispatcher;

        public QueryTreeProcessor(QueryTree queryTree, QueryTreeInvocation queryTreeInvocation, AbstractDispatcher dispatcher) {
            this.queryTree = queryTree;
            this.queryInvocation = queryTreeInvocation;
            this.dispatcher = dispatcher;
        }

        @Override public void run ()
        {
            Settings settings = dispatcher.callInfo.getSettings();

            // Ensure we have initialized for DB access for the current thread
            settings.getAggregateManager().configure(settings);

            DataStore po = settings.getDataStore();
            po.initForQuery();

            Query query = queryTree.createQuery(po);

            executeQuery(
                dispatcher,
                query,
                queryTree,
                queryInvocation);
        }

        public QueryTree getQueryTree() {
            return this.queryTree;
        }

        public QueryTreeInvocation getQueryInvocation() {
            return this.queryInvocation;
        }
    }

    private static class CallbackTask implements Runnable {

        private final Runnable task;
        private final Callback callback;
        private final QueryTree queryTree;
        private final QueryTreeInvocation queryTreeInvocation;

        CallbackTask(QueryTreeProcessor task, ParallelDispatcher callback) {
            this.task = task;
            this.callback = callback;
            this.queryTree = task.getQueryTree();
            this.queryTreeInvocation = task.getQueryInvocation();
        }

        public void run() {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                callback.complete(queryTree, queryTreeInvocation);
            }
        }

    }

    /*
     * Execute queries using BFS traversal
     */
    @Override
    protected void executeQueries(List<QueryTree> queries, QueryTreeInvocation queryTreeInvocation) {

        // Start with executing the root queries
        while(!queries.isEmpty()) {
            QueryTree queryTree = queries.remove(0);
            CallbackTask ct = new CallbackTask(new QueryTreeProcessor(queryTree, queryTreeInvocation, this), this);
            qe.submit(ct);
        }

        try {
            // Wait until all queries have completed
            // TODO: implement timeout to get out of a stuck query
            latch.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
