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

import tools.xor.util.InterQuery;

import java.util.HashSet;
import java.util.Iterator;
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
    private static final int MAX_INLIST_SIZE = 999;
    private static final int OFFSET = 1;

    private Map<QueryTree, Set> parentIdList; // Return the ids needed for a consuming Query
                                           // Can be userkey, if it is not composite, else
                                           // subquery is the only option supported for composite key.

    private Map<Query, String> resolvedQuery; // The Query.INTERQUERY_JOIN_PLACEHOLDER is replaced with
                                              // the appropriate query string

    private Map<Query, Set> idList; // ids needed for the QueryFragment.PARENT_INLIST parameter
                                     // If a query does not have an entry here after resolveQuery
                                     // has been processed, that means it is a SUBQUERY join till
                                     // the root node

    private Map<InterQuery, QueryVisitor> visitors; // used during a QueryTree's resolveField calls
    private Map<String, QueryVisitor> visitorsByPath;

    public QueryTreeInvocation(List<QueryTree> rootQueries) {
        // These fields will concurrently be updated/accessed if using ParallelDispatcher
        this.parentIdList = new ConcurrentHashMap<>();
        this.resolvedQuery = new ConcurrentHashMap<>();
        this.idList = new ConcurrentHashMap<>();
        this.visitors = new ConcurrentHashMap<>();
        this.visitorsByPath = new ConcurrentHashMap<>();

        // Initialize with the root queries
        for(QueryTree queryTree: rootQueries) {
            resolvedQuery.put(queryTree.getQuery(), queryTree.getQuery().getQueryString());
        }
    }

    /**
     * Update the query string for the child query
     *
     * @param edge for which the query string needs to be updated for the edge end
     */
    public void resolveQuery(AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree, InterQuery<QueryTree> edge) {

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

            // This is simple replace
            oqlString = oqlString.replaceFirst(
                Pattern.quote(Query.INTERQUERY_JOIN_PLACEHOLDER),
                getParentInListBindString(inlistvalues.size(), queryTree.getQuery()));
        }
        else {
            oqlString = oqlString.replaceFirst(
                Pattern.quote(Query.INTERQUERY_JOIN_PLACEHOLDER),
                deriveSubquery(edge, resolvedQuery.get(edge.getStart().getQuery())));

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

            // Set the parameters for the IN list
            Iterator iter = inlistvalues.iterator();
            int start = OFFSET;
            while(iter.hasNext()) {
                query.setParameter(QueryFragment.PARENT_INLIST + start++, iter.next());
            }
        }
    }

    private String getParentInListBindString(int count, Query query) {
        StringBuilder result = new StringBuilder();

        // We always give a name for the parameter since we are building the query
        /*
        if(query.isSQL()) {
            while(count-- > 1) {
                result.append("?, ");
            }
            result.append("?");
        } else
        */
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

        if(parentIdList.get(edge.getStart()).size() > MAX_INLIST_SIZE) {
            joinType = InterQuery.JoinType.SUBQUERY;
        }

        return joinType;
    }

    public String getResolvedQuery(Query query) {
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
}
