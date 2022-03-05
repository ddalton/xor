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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.service.DataStore;
import tools.xor.util.Constants;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.view.expression.AscHandler;

public class QueryFromFragments implements QueryBuilderStrategy
{
    private final QueryTree queryTree;
    private final QueryBuilder builder;

    public QueryFromFragments (QueryTree queryTree, QueryBuilder builder)
    {
        this.queryTree = queryTree;
        this.builder = builder;
    }

    @Override public QueryHandle construct(Settings settings)
    {
        // Generate the fields if needed
        this.queryTree.generateFields(settings, builder.getAggregateTree());

        if(queryTree.getVertices().size() == 0) {
            return null;
        }

        List<Function> consolidatedFunctions = QueryStringHelper.getQueryTreeFunctions(
            settings,
            this.queryTree);

        StringBuilder oql = new StringBuilder(constructOQL(settings));
        queryTree.setSelectString(oql.toString());
        oql.append(buildWhereClause(settings, consolidatedFunctions));
        oql.append(buildOrderClause(settings, consolidatedFunctions));

        final Logger vb = LogManager.getLogger(Constants.Log.VIEW_BRANCH);
        if(vb.isDebugEnabled()) {
            vb.debug("OQL of view [" + this.queryTree.getView().getName() + "] => " + oql.toString());
        }

        QueryHandle handle = new QueryHandle(oql.toString(),
            DataStore.QueryType.OQL,
            null);

        // Initialized the selected columns
        handle.setColumns(this.queryTree.getSelectedColumns());

        return handle;
    }

    private String constructOQL(Settings settings) {

        DataStore po = settings.getDataStore();
        QueryTree<QueryFragment, IntraQuery<QueryFragment>> qp = this.queryTree;

        // SELECT clause
        StringBuilder OQL = new StringBuilder(QueryBuilder.SELECT_CLAUSE);
        for(QueryField field: qp.getFields()) {
            if(!QueryBuilder.SELECT_CLAUSE.equals(OQL.toString())) {
                OQL.append(QueryBuilder.COMMA_DELIMITER);
            }
            OQL.append(
                Settings.doSQL(po) ?
                    field.getSQL() :
                    field.getOQL(po.getQueryCapability()));
        }

        // FROM clause
        // The fragment's entityType should be of the correct type, so no narrowing is needed
        EntityType entityType = qp.getRoot().getEntityType();
        OQL.append(" FROM " + entityType.getName() + QueryBuilder.AS_CLAUSE + qp.getRoot().getAlias() + " " + po.getPolymorphicClause(entityType));

        // Now iterate through the fragments and process all the fragments that are OUTER JOINED
        // with the fragment currently being processed
        // If there are any open content properties, then we need to explicitly join them, so they
        // are queued for later processing
        List<IntraQuery<QueryFragment>> children = new LinkedList<>();
        children.addAll(sortJoins(qp.getOutEdges(qp.getRoot())));
        while(!children.isEmpty()) {
            IntraQuery<QueryFragment> child = children.remove(0);
            OQL.append(child.getJoinClause(qp, po));

            // Keep processing this child
            children.addAll(0, qp.getOutEdges(child.getEnd()));
        }

        if(qp.getView() != null && qp.getView().getJoin() != null) {
            for(Join join: qp.getView().getJoin()) {
                if(join.getEntity() != null) {
                    OQL.append(", " + join.getEntity());
                }
            }
        }

        return OQL.toString();
    }

    private List<IntraQuery<QueryFragment>> sortJoins(Collection<IntraQuery<QueryFragment>> childEdges) {
        List<IntraQuery<QueryFragment>> result = new LinkedList<>();

        for(IntraQuery edge: childEdges) {
            if(edge.getProperty().isOpenContent()) {
                // add to end
                result.add(edge);
            } else {
                // add to beginning
                result.add(0, edge);
            }
        }

        return result;
    }

    private String buildWhereClause(Settings settings, List<Function> consolidatedFunctions) {
        StringBuilder queryString = new StringBuilder();

        checkAndAddFilters(queryString, settings, consolidatedFunctions);
        checkAndAddId(queryString);
        checkAndAddInterQueryJoinPlaceholder(queryString);
        checkAndAddChunkStart(settings, queryString, consolidatedFunctions);
        checkAndAddOpenPropertyJoins(queryString);

        return queryString.toString();
    }

    private void checkAndAddInterQueryJoinPlaceholder (StringBuilder queryString)
    {
        QueryTree<QueryFragment, IntraQuery<QueryFragment>> qp = this.queryTree;
        if(builder.getAggregateTree().getInEdges(qp).size() > 0) {
            InterQuery<QueryTree> edge = builder.getAggregateTree().getInEdges(qp).iterator().next();
            QueryTree parent = edge.getStart();

            // Since we are constructing an OQL query, the parent query should also be of the same type
            if (edge != null && parent.getQueryHandle() != null && parent.getQueryHandle().getQueryType() == DataStore.QueryType.OQL) {
                addWhereStep(queryString);
                queryString.append(qp.getRoot().getId()).append(" IN ( ").append(Query.INTERQUERY_JOIN_PLACEHOLDER).append(
                    ")");
            }
        }
    }

    /*
     * In the future this can be expanded to more fields in the entity, similiar
     * to Query By Example, if the id is not present.
     */
    private void checkAndAddId(StringBuilder queryString) {
        QueryTree<QueryFragment, IntraQuery<QueryFragment>> qp = this.queryTree;

        // Do this only for the root query piece and if the id is provided
        if(builder.getAggregateTree().getRoot() == qp && builder.getEntity() != null) {
            if(builder.getEntity().getIdentifierValue() != null) {
                addWhereStep(queryString);
                queryString.append(qp.getRoot().getId() + " = :" + QueryFragment.ID_PARAMETER_NAME);
            }
        }
    }

    protected void checkAndAddFilters(StringBuilder queryString, Settings settings, List<Function> consolidatedFunctions) {

        for(Function function : consolidatedFunctions) {

            // Order By filters are handled separately
            if(function.isOrderBy() || !function.isRelevant()) {
                continue;
            }
            if(!Function.doProcess(function, settings)) {
                continue;
            }

            addWhereStep(queryString);
            queryString.append(function.getQueryString());
        }
    }

    private void addWhereStep(StringBuilder queryString) {
        if(queryString.length() > 0)
            queryString.append(" AND ");
        else
            queryString.append(" WHERE ");
    }

    /*
     * refers to the columns used in the order by clause.
     * There needs to be atleast one unique column for this to work property.
     * If there are multiple columns then the last column should be a unique column
     *
     * Examples:
     * 1. Sort on unique column A
     *    Then the pageColumn will refer to this unique column A
     *
     * 2. If we sort on non-unique column B
     *    We add the id column as the second column
     *    so we sort on B, I
     *    and the query becomes
     *    AND ( B > pageColumn.B OR (B = pageColumn.B AND I > pageColumn.I))
     *
     *
     * @param settings user provided settings
     * @param selectClause the current query string that has been built so far
     */
    protected void checkAndAddChunkStart(Settings settings, StringBuilder queryString, List<Function> consolidatedFunctions) {
        if(settings == null) {
            return;
        }

        Map<String, Object> nextToken = settings.getNextToken();
        if(nextToken == null || nextToken.isEmpty()) {
            return;
        }

        List<Function> orderBy = new LinkedList<Function>();
        for(Function function : consolidatedFunctions) {
            if(function.isOrderBy()) {
                orderBy.add(function);
            }
        }

        addWhereStep(queryString);
        queryString.append( " ( ");
        for(int i = 0; i < orderBy.size(); i++) {
            Function f = orderBy.get(i);
            // AND ( B > pageColumn.B OR (B = pageColumn.B AND I > pageColumn.I))

            if(i == 0) {
                queryString.append( f.getNormalizedName() );
                queryString.append( getDirection(f) + ":" + QueryFragment.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
            }

            if(i > 0) {
                queryString.append( " OR ( " + getEqualOrderByExp(i-1, orderBy) );
                queryString.append( " AND " + f.getNormalizedName() + getDirection(f) + ":" + QueryFragment.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
                queryString.append( " ) ");
            }
        }
        queryString.append( " ) ");
    }

    private String getDirection(Function function) {
        if(function.functionHandler instanceof AscHandler) {
            return " > ";
        } else {
            return " < ";
        }
    }

    private String getEqualOrderByExp(int endPos, List<Function> orderBy) {
        // 	Create an equals condition for all the orderBy fields until endPos

        StringBuilder result = new StringBuilder("");
        for(int i = 0; i <= endPos; i++) {
            Function f = orderBy.get(i);
            if(i > 0) {
                result.append(" AND ");
            }
            result.append( f.getNormalizedName() + " = : " + QueryFragment.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
        }

        return result.toString();
    }

    private void checkAndAddOpenPropertyJoins(StringBuilder queryString) {
        StringBuilder whereFragment = new StringBuilder("");

        QueryTree<QueryFragment, IntraQuery<QueryFragment>> qp = this.queryTree;
        for(IntraQuery<QueryFragment> edge: qp.getOpenContentJoins()) {

            ExtendedProperty extendedProperty = (ExtendedProperty) edge.getProperty();

            // Represents the source type of the foreign key relationship
            // The source type is reversed based on whether this is a TO_ONE or TO_MANY relationship
            Map<String, String> keyFields = extendedProperty.getKeyFields();

            String sourceAlias = null;
            String targetAlias = null;

            if(extendedProperty.getRelationshipType() == RelationshipType.TO_ONE) {
                sourceAlias = edge.getStart().getAlias();
                targetAlias = edge.getEnd().getAlias();
            } else if(extendedProperty.getRelationshipType() == RelationshipType.TO_MANY) {
                sourceAlias = edge.getEnd().getAlias();
                targetAlias = edge.getStart().getAlias();
            }

            if(sourceAlias != null && targetAlias != null) {

                for(Map.Entry<String, String> entry: keyFields.entrySet()) {
                    if(whereFragment.length() > 0) {
                        whereFragment.append(" AND ");
                    }
                    whereFragment
                        .append(sourceAlias + Settings.PATH_DELIMITER + entry.getKey())
                        .append(" = ")
                        .append(targetAlias + Settings.PATH_DELIMITER + entry.getValue());
                }
            }
        }
        if(whereFragment.length() > 0) {
            addWhereStep(queryString);
            queryString.append(whereFragment);
        }
    }

    private List<String> getOrderClauses(QueryFragment queryFragment) {
        List<String> orderClauses = new LinkedList<>();
        for(String name: queryFragment.getPrimaryKeyFieldNames()) {
            orderClauses.add(queryFragment.getAlias() + Settings.PATH_DELIMITER + name);
        }

        return orderClauses;
    }

    private void populateOrder(QueryFragment fragment, Map<String, List<String>> fragmentOrder) {
        QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree = this.queryTree;

        if(queryTree.getParent(fragment) == null) {
            // this is the root node
            fragmentOrder.put(fragment.getAlias(), getOrderClauses(fragment));
        }

        boolean foundCollection = false;
        for(IntraQuery<QueryFragment> outEdge: queryTree.getOutEdges(fragment)) {
            if(outEdge.getProperty().isMany()) {
                if(!outEdge.getEnd().getEntityType().isDataType()) {
                    if(foundCollection) {
                        throw new RuntimeException("Querying on parallel collections is not supported. Was this query processed through CartesianJoinSplitter?");
                    }
                    foundCollection = true;
                    QueryFragment collectionElement = outEdge.getEnd();
                    fragmentOrder.put(collectionElement.getAlias(), getOrderClauses(collectionElement));
                    populateOrder(collectionElement, fragmentOrder);
                }
            } else {
                populateOrder(outEdge.getEnd(), fragmentOrder);
            }
        }
    }

    private String buildOrderClause(Settings settings, List<Function> consolidatedFunctions) {

        DataStore po = settings.getDataStore();
        QueryTree<QueryFragment, IntraQuery<QueryFragment>> queryTree = this.queryTree;

        // We need ORDER BY clauses for all collection properties so that they are
        // clubbed together.
        // Starting from the root to the most deeply nested.
        // There is only one path since the query would have undergone CartesianJoin splitting.

        // Map of the fragment alias to its primary key property names
        Map<String, List<String>> fragmentOrder = new LinkedHashMap<>();

        // Starting from the root we walk down the collection path
        QueryFragment root = queryTree.getRoot();

        // TODO: Run this through CartesionJoinSplitter for existing OQL tests
        // TODO: so this check can be removed
        if(Settings.doSQL(po)) {
            populateOrder(root, fragmentOrder);
        }
        // TODO: temp - add root for now to get by some tests
        fragmentOrder.put(root.getAlias(), getOrderClauses(root));

        // At this point all the collections are by default sorted by their primary key
        // values
        // This is now overridden with the following

        for (QueryField field : queryTree.getFields()) {
            if (field.getPath().endsWith(QueryFragment.LIST_INDEX_ATTRIBUTE)) {
                // We have to sort all the collection objects
                // so this collection is clubbed together
                List<String> indexOrder = new LinkedList<>();
                String alias = field.getQueryFragment().getAlias();

                indexOrder.add(field.getOQL(po.getQueryCapability()));
                fragmentOrder.put(alias, indexOrder);
            }
        }

            // filter order clauses
        Map<String, List<String>> overridden = new HashMap<>();
        for (Function function : consolidatedFunctions) {
            if (function.isOrderBy()) {
                String name = function.getNormalizedName();
                if(name.indexOf(Settings.PATH_DELIMITER) != -1) {
                    // The first part is the alias
                    String alias = name.substring(0, name.indexOf(Settings.PATH_DELIMITER));
                    List<String> orderClauses = new LinkedList<>();
                    if(overridden.containsKey(alias)) {
                        orderClauses = overridden.get(alias);
                    } else {
                        overridden.put(alias, orderClauses);
                    }
                    orderClauses.add(function.getQueryString());
                }
            }
        }

        // Override the default values where applicable
        for(Map.Entry<String, List<String>> entry: overridden.entrySet()) {
            if(fragmentOrder.containsKey(entry.getKey())) {
                fragmentOrder.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder filterOrderBy = new StringBuilder();
        for(Map.Entry<String, List<String>> entry: fragmentOrder.entrySet()) {
            if(filterOrderBy.length() > 0) {
                filterOrderBy.append(QueryBuilder.COMMA_DELIMITER);
            }

            String orderString = entry.getValue().stream().collect(Collectors.joining(QueryBuilder.COMMA_DELIMITER));
            filterOrderBy.append(orderString);
        }
        if(filterOrderBy.length() > 0) {
            filterOrderBy.insert(0, " ORDER BY ");
        }

        return filterOrderBy.toString();
    }
}
