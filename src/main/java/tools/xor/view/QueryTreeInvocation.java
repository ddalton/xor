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
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.SurrogateEntityKey;
import tools.xor.Type;
import tools.xor.util.InterQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Used to hold temporary results during the execution of a QueryTree
 */
public class QueryTreeInvocation
{
    public static final int MAX_INLIST_SIZE = 999;
    public static final int OFFSET = 1;

    private Map<QueryTree, Set> parentIdList; // Return the ids needed for a consuming Query
                                              // Can be userkey, if it is not composite, else
                                              // subquery is the only option supported for composite key.

    private Map<Query, String> resolvedQuery; // The Query.INTERQUERY_JOIN_PLACEHOLDER is replaced with
                                              // the appropriate query string

    private Map<Query, Set> idList;           // ids needed for the QueryFragment.PARENT_INLIST parameter
                                              // If a query does not have an entry here after resolveQuery
                                              // has been processed, that means it is a SUBQUERY join till
                                              // the root node

    private Map<String, List<BusinessObject>> objectsByPath; // Used for stitching child objects
                                                             // The parent objects are obtained by getting
                                                             // the full path from the source fragment of the InterQuery edge

    private Map<EntityKey, BusinessObject> queryObjects; // Unique object per id and path

    private Map<QueryTree, List<RecordDelta>> recordDeltas;

    private Map<InterQuery, QueryVisitor> visitors; // used during a QueryTree's resolveField calls
    private Map<String, QueryVisitor> visitorsByPath;

    public QueryTreeInvocation(List<QueryTree> rootQueries) {
        // These fields will concurrently be updated/accessed if using ParallelDispatcher
        this.parentIdList = new ConcurrentHashMap<>();
        this.resolvedQuery = new ConcurrentHashMap<>();
        this.idList = new ConcurrentHashMap<>();
        this.visitors = new ConcurrentHashMap<>();
        this.visitorsByPath = new ConcurrentHashMap<>();
        this.objectsByPath = new ConcurrentHashMap<>();
        this.queryObjects = new ConcurrentHashMap<>();
        this.recordDeltas = new ConcurrentHashMap<>();

        // Initialize with the root queries
        //for(QueryTree queryTree: rootQueries) {
        //    resolvedQuery.put(queryTree.getQuery(), queryTree.getQuery().getQueryString());
        //}
    }

    public static class RecordDelta {
        private Set<String> changed; // the fields that have new information
        private Map<String, Object> propertyResult; // information from result set keyed by full path
        private String lcp;
        private Object[] record;

        public RecordDelta(Set<String> changed, Map<String, Object> propertyResult, String lcp, Object[] record) {
            this.changed = changed;
            this.propertyResult = propertyResult;
            this.lcp = lcp;
            this.record = record;
        }

        public Set<String> getChanged() {
            return this.changed;
        }

        public Map<String, Object> getPropertyResult() {
            return this.propertyResult;
        }

        public String getLCP() {
            return this.lcp;
        }

        public Object[] getRecord() {
            return this.record;
        }
    }

    /**
     * Update the query string for the child query
     *
     * @param edge for which the query string needs to be updated for the edge end
     */
    public void resolveQuery(InterQuery<QueryTree> edge) {

        QueryTree queryTree = edge.getEnd();
        InterQuery parentEdge = edge;

        // since query is resolved in a BFS manner, we only need to join with the immediate parent
        InterQuery.JoinType joinType = getJoinType(parentEdge);

        if(parentEdge != null) {
            idList.put(queryTree.getQuery(), parentIdList.get(parentEdge.getStart()));
        }

        String oqlString = queryTree.getQuery().getQueryString();
        if (joinType == InterQuery.JoinType.INLIST) {

            Set inlistvalues = idList.get(queryTree.getQuery());
            int size = inlistvalues.size() >= MAX_INLIST_SIZE ? MAX_INLIST_SIZE : inlistvalues.size();

            // This is simple replace
            oqlString = oqlString.replaceFirst(
                Pattern.quote(Query.INTERQUERY_JOIN_PLACEHOLDER),
                getParentInListBindString(size, queryTree.getQuery()));
        }
        else {
            oqlString = oqlString.replaceFirst(
                Pattern.quote(Query.INTERQUERY_JOIN_PLACEHOLDER),
                deriveSubquery(edge, getResolvedQuery(edge.getStart().getQuery())));

        }
        resolvedQuery.put(queryTree.getQuery(), oqlString);
    }

    private String deriveSubquery(InterQuery edge, String oql) {
        // We need to select only the parent id from the original parent oql
        // So we first split the query around the FROM clause
        // then prepend the select clause for the parent id

        StringBuilder subquery = new StringBuilder("SELECT ");
        subquery.append(edge.getSource().getId()).append(oql.substring(oql.indexOf(" FROM ")));

        return subquery.toString();
    }

    public void initInList(Query query) {
        if(idList.containsKey(query)) {
            Set inlistvalues = idList.get(query);

            if(inlistvalues.size() <= MAX_INLIST_SIZE) {
                // Set the parameters for the IN list
                int start = OFFSET;
                Iterator iter = inlistvalues.iterator();
                while (iter.hasNext()) {
                    query.setParameter(QueryFragment.PARENT_INLIST + start++, iter.next());
                }
            } else {
                query.processLargeInList(inlistvalues);
            }
        }
    }

    private String getParentInListBindString(int count, Query query) {
        StringBuilder result = new StringBuilder();

        // We always give a name for the parameter since we are building the query
        if(query.isOQL() || query.isSQL()) {
            int i = OFFSET;
            for(; i < count; i++) {
                result.append(":").append(QueryFragment.PARENT_INLIST + i).append(", ");
            }
            result.append(":").append(QueryFragment.PARENT_INLIST + i);
        } else {
            throw new RuntimeException("Cannot call getParentInListBindString on a Stored Procedure");
        }

        return result.toString();
    }

    private InterQuery.JoinType getJoinType(InterQuery edge) {
        InterQuery.JoinType joinType = InterQuery.JoinType.INLIST;

        if(!parentIdList.containsKey(edge.getStart())) {
            throw new RuntimeException("Child query can only be invoked after parent query has returned");
        }

        // TODO: Enable subquery/exists only in a restricted setup
        // 1. child and all the ancestor queries are of the same type (e.g., SQL/OQL)
        // 2. A new parent query needs to be built where we don't need to pull in additional information
        // 3. The NativeQuery and OQLQuery objects need to a have a primarykey attribute to identify the primary key
        //    to be used for EXISTS correlation
        //if(parentIdList.get(edge.getStart()).size() > MAX_INLIST_SIZE) {
        //    joinType = InterQuery.JoinType.SUBQUERY;
        //}

        return joinType;
    }

    public String getResolvedQuery(Query query) {
        if(!resolvedQuery.containsKey(query)) {
            return query.getQueryString();
        }
        return resolvedQuery.get(query);
    }

    public static class QueryVisitor {
        InterQuery<QueryTree> outgoingEdge;
        Set ids;

        public QueryVisitor(InterQuery<QueryTree> outgoingEdge) {
            this.outgoingEdge = outgoingEdge;
            this.ids = new HashSet<>();
        }

        public void addId(Object id) {
            ids.add(id);
        }
    }

    public void visit(String path, Object value) {
        if(visitorsByPath.containsKey(path)) {
            visitorsByPath.get(path).addId(value);
        }
    }

    public void start(AggregateTree<QueryTree, InterQuery<QueryTree>> at, QueryTree qt) {
        // Loop through each outgoing edge and create a visitor for them
        for(InterQuery outgoing: at.getOutEdges(qt)) {
            QueryVisitor visitor = new QueryVisitor(outgoing);
            visitors.put(outgoing, visitor);
            visitorsByPath.put(outgoing.getSource().getIdField().getFullPath(), visitor);
        }
    }

    public void finish(AggregateTree<QueryTree, InterQuery<QueryTree>> at, QueryTree qt) {
        // Loop through each outgoing edge and process the results
        for(InterQuery outgoing: at.getOutEdges(qt)) {
            QueryVisitor visitor = visitors.get(outgoing);
            parentIdList.put((QueryTree)outgoing.getStart(), visitor.ids);

            // Now we no longer need this visitor as it has been processed
            visitors.remove(outgoing);
        }
    }

    public void visit(String path, BusinessObject bo) {
        path = QueryFragment.extractAnchorPath(path);

        List<BusinessObject> bos = objectsByPath.get(path);
        if(bos == null) {
            bos = new LinkedList<>();
            objectsByPath.put(path, bos);
        }
        bos.add(bo);

        Object id = bo.getIdentifierValue();
        // Currently we only support tracking objects with surrogate key
        if(id != null) {
            Type type = ((EntityType)bo.getType()).getRootEntityType();
            EntityKey key = new SurrogateEntityKey(id, type.getName(), path);
            BusinessObject existing = queryObjects.get(key);
            assert(existing == null);
            queryObjects.put(key, bo);
        }
    }

    /*
    public BusinessObject findQueryObject(EntityKey key) {
        return queryObjects.get(key);
    }
    */

    public BusinessObject getQueryObject(String path, Object idValue, Type type) {
        type = ((EntityType)type).getRootEntityType();
        EntityKey key = new SurrogateEntityKey(idValue, type.getName(), path);

        return queryObjects.get(key);
    }

    public void addRecordDelta(QueryTree queryTree, Set<String> changed, Map<String, Object> propertyResult, String lcp, Object[] record) {
        List<RecordDelta> deltas = recordDeltas.get(queryTree);
        if(deltas == null) {
            deltas = new LinkedList<>();
            recordDeltas.put(queryTree, deltas);
        }
        deltas.add(new RecordDelta(changed, propertyResult, lcp, record));
    }

    public List<RecordDelta> getRecordDeltas(QueryTree queryTree) {
        return this.recordDeltas.get(queryTree);
    }
}
