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

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import tools.xor.AbstractTypeMapper;
import tools.xor.BusinessObject;
import tools.xor.EntityKey;
import tools.xor.SurrogateEntityKey;
import tools.xor.Type;
import tools.xor.util.InterQuery;

/**
 * Used to hold temporary results during the execution of a QueryTree
 */
public class QueryTreeInvocation
{
    public static final int MAX_INLIST_SIZE = 999;
    public static final int OFFSET = 1;

    private Map<QueryFragment, Set> parentIdList; //   Return the ids needed for a consuming Query
                                               // Can be userkey, if it is not composite, else
                                               // subquery is the only option supported for composite key.
                                               //   A QueryTree can have different sets of parentIdList since
                                               // 2 InterQuery edges might have different source fragments
    private Map<QueryTree, Object> lastParentId; // useful for result scrolling functionality

    private Map<Query, Set> idList;           // ids needed for the QueryFragment.PARENT_INLIST parameter
                                              // If a query does not have an entry here after resolveQuery
                                              // has been processed, that means it is a SUBQUERY join till
                                              // the root node

    private Map<String, List<BusinessObject>> objectsByPath; // Used for stitching child objects
                                                             // The parent objects are obtained by getting
                                                             // the full path from the source fragment of the InterQuery edge

    private Map<EntityKey, BusinessObject>    queryObjects; // Unique object per id and path
    private Map<QueryTree, List<RecordDelta>> recordDeltas;
    private Map<QueryFragment, QueryVisitor>  visitors; // used during a QueryTree's resolveField calls
    private Map<String, QueryVisitor>         visitorsByPath;
    private Map<QueryTree, String>            invocationIds; // Safe to use QueryTree as we use a copy of the AggregateTree

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();


    public QueryTreeInvocation() {
        // These fields will concurrently be updated/accessed if using ParallelDispatcher
        this.parentIdList = new ConcurrentHashMap<>();
        this.lastParentId = new ConcurrentHashMap<>();
        this.idList = new ConcurrentHashMap<>();
        this.visitors = new ConcurrentHashMap<>();
        this.visitorsByPath = new ConcurrentHashMap<>();
        this.objectsByPath = new ConcurrentHashMap<>();
        this.queryObjects = new ConcurrentHashMap<>();
        this.recordDeltas = new ConcurrentHashMap<>();
        this.invocationIds = new ConcurrentHashMap<>();
    }

    public String getOrCreateInvocationId (QueryTree queryTree)
    {
        if(!invocationIds.containsKey(queryTree)) {
            // generate a GUID invocation id that is 128 bits in length in base64 format
            UUID uuid = UUID.randomUUID();
            byte[] bytes = getBytesFromUUID(uuid);
            invocationIds.put(queryTree, BASE64_URL_ENCODER.encodeToString(bytes));
        }

        return invocationIds.get(queryTree);
    }

    public String getInvocationId (QueryTree queryTree) {
        return invocationIds.get(queryTree);
    }
    
    public Object getLastParentId(QueryTree queryTree) {
        return this.lastParentId.get(queryTree);
    }    
    
    public Set getParentIds(InterQuery edge) {
        return this.parentIdList.get(edge.getSource());
    }       

    public static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
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
        InterQuery<QueryTree> parentEdge = edge;

        // since query is resolved in a BFS manner, we only need to join with the immediate parent
        InterQuery.JoinType joinType = getJoinType(parentEdge);

        if(parentEdge != null) {
            idList.put(queryTree.getQuery(), getParentIds(parentEdge));
        }

        String queryString = queryTree.getQuery().getQueryString();
        if (joinType == InterQuery.JoinType.INLIST) {

            Set inlistvalues = idList.get(queryTree.getQuery());
            int size = inlistvalues.size() >= MAX_INLIST_SIZE ? MAX_INLIST_SIZE : inlistvalues.size();

            // This is simple replace
            queryString = queryString.replaceFirst(
                Pattern.quote(Query.INTERQUERY_JOIN_PLACEHOLDER),
                getParentInListBindString(size, queryTree.getQuery()));
        }
        else if(joinType == InterQuery.JoinType.JOINTABLE) {
            // do nothing
            // either INSERT the parent ids or
            // INSERT using the parent query
            // TODO: update the fact that the rows were inserted
        } else {
            queryString = queryString.replaceFirst(
                Pattern.quote(Query.INTERQUERY_JOIN_PLACEHOLDER),
                deriveSubquery(edge, edge.getStart().getQuery().getQueryString()));

        }
        queryTree.getQuery().setQueryString(queryString);
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

    private InterQuery.JoinType getJoinType(InterQuery<QueryTree> edge) {
        InterQuery.JoinType joinType = InterQuery.JoinType.INLIST;

        if(!parentIdList.containsKey(edge.getSource())) {
            throw new RuntimeException("Child query can only be invoked after parent query has returned");
        }

        // TODO: Enable subquery/exists only in a restricted setup
        // 1. child and all the ancestor queries are of the same type (e.g., SQL/OQL)
        // 2. A new parent query needs to be built where we don't need to pull in additional information
        // 3. The NativeQuery and OQLQuery objects need to a have a primarykey attribute to identify the primary key
        //    to be used for EXISTS correlation
        //if(parentIdList.get(edge.getSource()).size() > MAX_INLIST_SIZE) {
        //    joinType = InterQuery.JoinType.SUBQUERY;
        //}

        QueryTree childQuery = edge.getEnd();
        // first check that the child query is either
        // 1. SQL
        // 2. Stored procedure query
        // and if (1) then check that it references the query join table
        if(QueryJoinAction.needsQueryJoinTable(childQuery)) {
            joinType = InterQuery.JoinType.JOINTABLE;
        }

        return joinType;
    }

    public static class QueryVisitor {
        // This is a list because to support scrolling we need to
        // know the last id
        List ids = new LinkedList<>();

        public void addId(Object id) {
            if(id != null) {
                ids.add(id);
            }
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
            // Multiple InterQuery edges can emanate from the same QueryFragment source
            if(!visitors.containsKey(outgoing.getSource())) {
                QueryVisitor visitor = new QueryVisitor();
                visitors.put(outgoing.getSource(), visitor);
                visitorsByPath.put(outgoing.getSource().getIdField().getFullPath(), visitor);
            }
        }
    }

    public void finish(AggregateTree<QueryTree, InterQuery<QueryTree>> at, QueryTree qt) {
        // Loop through each outgoing edge and process the results
        for(InterQuery outgoing: at.getOutEdges(qt)) {
            // check if it has already been processed, since multiple InterQuery edges
            // can start from the same QueryFragment source
            QueryFragment source = outgoing.getSource();
            if(visitors.containsKey(source)) {
                QueryVisitor visitor = visitors.get(source);
                parentIdList.put(source, new HashSet<>(visitor.ids));
                if(visitor.ids.size() > 0) {
                    lastParentId.put(qt, visitor.ids.get(visitor.ids.size()-1));
                }

                // Now we no longer need this visitor as it has been processed
                visitors.remove(source);
            }
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
        if (id != null) {
            EntityKey key = new SurrogateEntityKey(id, AbstractTypeMapper.getSurrogateKeyTypeName(bo.getType()), path);
            BusinessObject existing = queryObjects.get(key);

            if (existing != bo) {
                queryObjects.put(key, bo);
            }
        }
    }

    public BusinessObject getQueryObject(String path, Object idValue, Type type) {
        EntityKey key = new SurrogateEntityKey(idValue, AbstractTypeMapper.getSurrogateKeyTypeName(type), path);        
        
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
