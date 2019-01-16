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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.ExtendedProperty;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.Constants;
import tools.xor.util.InterQuery;
import tools.xor.util.IntraQuery;
import tools.xor.view.expression.AscHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates the literal OQL QueryString for each QueryPiece in a QueryTree.
 * Binds it with the provided parameters.
 * Two types of queries are possible depending upon the type of object resolution desired:
 * 1. Shared
 * 2. Distinct
 * @see tools.xor.view.ObjectResolver.Type
 *
 */

public class QueryBuilder
{
    public static final String SELECT_CLAUSE = "SELECT ";
    public static final String COMMA_DELIMITER = ", ";
    public static final String AS_CLAUSE = " AS ";

    private QueryTree<QueryPiece, InterQuery<QueryPiece>> queryTree;
    private boolean             addIdentifier;
    List<Function> consolidatedFunctions = new LinkedList<>();

    public QueryBuilder(QueryTree queryTree) {
        this.queryTree = queryTree;
    }

    /**
     * Constructs the OQL query for each QueryPiece and populates it with it.
     * We do it in a BFS traversal.
     */
    public void construct(Settings settings) {
        List<QueryPiece> queries = new LinkedList<>();
        queries.add(queryTree.getRoot());

        while(!queries.isEmpty()) {
            QueryPiece qp = queries.remove(0);
            queries.addAll(qp.getChildren(qp));

            // construct the query and set it one the qp
            construct(settings, qp);
        }
    }

    /**
     * Construct the OQL query for a QueryPiece, and initialize the QueryPiece with it.
     *
     * @param settings provided by the user
     * @param qp QueryPiece for which we need to construct a query
     */
    public void construct(Settings settings, QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {

        // Consolidate the user supplied filters and the filters
        // defined on the view
        List<Function> temp = new LinkedList<>();
        for(Function function : settings.getAdditionalFunctions()) {
            Function narrowedFunction = function.copy();
            temp.add(narrowedFunction);
        }
        if(queryTree.getView() != null && queryTree.getView().getFunction() != null) {
            temp.addAll(queryTree.getView().getFunction());
        }

        // We populate only those filters for while all the attributes can be found in
        // the QueryPiece
        consolidatedFunctions = new LinkedList<>();
        for(Function function : temp) {
            if(function.normalize(qp)) {
                consolidatedFunctions.add(function);
            }
        }
        Collections.sort(consolidatedFunctions);


        qp.generateFields(settings, this.queryTree);

        // The following steps are not necessary if a custom query is being used
        StringBuilder oql = new StringBuilder(constructOQL(settings.getPersistenceOrchestrator(), qp));
        oql.append(buildWhereClause(settings, qp));
        oql.append(buildOrderClause(qp, settings.getPersistenceOrchestrator()));

        final Logger vb = LogManager.getLogger(Constants.Log.VIEW_BRANCH);
        if(vb.isDebugEnabled()) {
            vb.debug("OQL of view [" + this.queryTree.getView().getName() + "] => " + oql.toString());
        }

        Query query = settings.getPersistenceOrchestrator().getQuery(
            oql.toString(),
            PersistenceOrchestrator.QueryType.OQL,
            null,
            settings);
        qp.setQuery(query);
    }

    private String constructOQL(PersistenceOrchestrator po, QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {

        // SELECT clause
        StringBuilder OQL = new StringBuilder(SELECT_CLAUSE);
        for(QueryField field: qp.getFields()) {
            if(!SELECT_CLAUSE.equals(OQL.toString())) {
                OQL.append(COMMA_DELIMITER);
            }
            OQL.append(field.getOQL(po.getQueryCapability()));
        }

        // FROM clause
        // The fragment's entityType should be of the correct type, so no narrowing is needed
        Class<?> entityClass = qp.getRoot().getEntityType().getInstanceClass();
        OQL.append(" FROM " + entityClass.getName() + AS_CLAUSE + qp.getRoot().getAlias() + " " + po.getPolymorphicClause(entityClass));

        // Now iterate through the fragments and process all the fragments that are OUTER JOINED
        // with the fragment currently being processed
        // If there are any open content properties, then we need to explicitly join them, so they
        // are queued for later processing
        List<IntraQuery<QueryFragment>> children = new LinkedList<>();
        children.addAll(sortJoins(qp.getOutEdges(qp.getRoot())));
        while(!children.isEmpty()) {
            IntraQuery<QueryFragment> child = children.remove(0);
            OQL.append(child.getJoinClause(po));

            // Keep processing this child
            children.addAll(0, qp.getOutEdges(child.getEnd()));
        }

        if(queryTree.getView() != null && queryTree.getView().getJoin() != null && queryTree.getView().getJoin().getEntity() != null)
            OQL.append(", " + queryTree.getView().getJoin().getEntity());

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

    protected String buildWhereClause(Settings settings, QueryPiece qp) {
        StringBuilder result = new StringBuilder();

        checkAndAddFilters(result, settings);
        checkAndAddId(result, qp);
        checkAndAddChunkStart(settings, result);
        checkAndAddOpenPropertyJoins(result, qp);

        return result.toString();
    }

    protected void checkAndAddFilters(StringBuilder result, Settings settings) {
        Map<String, Object> userParams = settings.getParams();
        Map<String, Object> normParams = new HashMap<>();

		Map<String, Parameter> parameterMap = new HashMap<String, Parameter>();
		for(Parameter parameter: queryTree.getParameter()) {
            parameterMap.put(parameter.name, parameter);
        }

		for(Function function : consolidatedFunctions) {

			// Order By filters are handled separately
			if(function.isOrderBy()) {
				continue;
			}

			// Filter is skipped
			if(!function.isFilterIncluded(userParams, normParams, parameterMap))
				continue;

			addWhereStep(result);
			result.append(function.getQueryString());
		}

		// Set parameters referred from native query or join clause
		if(queryTree.getView() != null && queryTree.getView().getJoin() != null) {
			// Expression
			addWhereStep(result);

			for(Parameter parameter: queryTree.getView().getJoin().getParameter()) {
				if(userParams.containsKey(parameter.name)) {
					normParams.put(parameter.name, userParams.get(parameter.name));
					result.append(parameter.expression);
				}
			}
		}
    }


    protected void checkAndAddId(StringBuilder result, QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {

        if(addIdentifier) {
            addWhereStep(result);

            result.append( qp.getRoot().getId() );
            result.append( " = :" + QueryProperty.ID_PARAMETER_NAME);
        }
    }

    private void addWhereStep(StringBuilder result) {
        if(result.length() > 0)
            result.append(" AND ");
        else
            result.append(" WHERE ");
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
     * @param queryString the current query string that has been built so far
     */
    protected void checkAndAddChunkStart(Settings settings, StringBuilder queryString) {
        if(settings == null) {
            return;
        }

        Map<String, Object> nextToken = settings.getNextToken();
        if(nextToken == null || nextToken.isEmpty()) {
            return;
        }

        List<Function> orderBy = new LinkedList<Function>();
        for(Function function : this.consolidatedFunctions) {
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
                queryString.append( getDirection(f) + ":" + QueryProperty.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
            }

            if(i > 0) {
                queryString.append( " OR ( " + getEqualOrderByExp(i-1, orderBy) );
                queryString.append( " AND " + f.getNormalizedName() + getDirection(f) + ":" + QueryProperty.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
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
            result.append( f.getNormalizedName() + " = : " + QueryProperty.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
        }

        return result.toString();
    }

    private void checkAndAddOpenPropertyJoins(StringBuilder result, QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp) {
        StringBuilder whereFragment = new StringBuilder("");

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
			addWhereStep(result);
			result.append(whereFragment);
		}
    }

    protected String buildOrderClause( QueryPiece<QueryFragment, IntraQuery<QueryFragment>> qp, PersistenceOrchestrator po) {
        StringBuilder result = new StringBuilder();

        Map<Integer, String> orderClauses = new TreeMap<>();
        for(QueryField field: qp.getFields()) {
			if(field.getPath().endsWith(QueryProperty.LIST_INDEX_ATTRIBUTE)) {
				String columnString = field.getOQL(po.getQueryCapability());
				orderClauses.put(field.getAttributeLevel(), columnString);
			}
		}

		// filter order clauses
		StringBuilder filterOrderBy = new StringBuilder();
		for(Function function : this.consolidatedFunctions) {
			if(function.isOrderBy()) {
				if(filterOrderBy.length() > 0)
					filterOrderBy.append(COMMA_DELIMITER);
				filterOrderBy.append(function.getQueryString());
			}
		}

		// Append any ORDER BY clauses
		if(orderClauses.size() > 0 || filterOrderBy.length() > 0) {
			for(String indexColumn: orderClauses.values())
				result.append( (result.length() > 0) ? COMMA_DELIMITER + indexColumn : indexColumn );

			if(filterOrderBy.length() > 0)
				result.append((result.length() > 0) ? COMMA_DELIMITER + filterOrderBy : filterOrderBy);

			result.insert(0, " ORDER BY ");
		}

        return result.toString();
    }
}
