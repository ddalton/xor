/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.PersistenceOrchestrator.QueryType;
import tools.xor.service.QueryCapability;
import tools.xor.util.Constants;
import tools.xor.view.expression.AscFunctionExpression;

/**
 *
 * The QueryTransformer currently flattens the QueryTree and works off ColumnMeta and the attributePath.
 * This needs to be refactored and built directly from the root QueryPiece of the original
 * QueryTree instance.
 * This approach gives the developer more flexibility in customizing the query and making it
 * more powerful (support hierarchical queries) and reliable (easier to test).
 *
 * DESIGN
 * ======
 *
 * Before we look at the design let us get familiar with the terminology used.
 *
 * Glossary:
 * ---------
 * The QueryTree data structure is a tree of a tree
 * QueryTree -> QueryPiece -> QueryFragment
 *
 * QueryTree
 *   A QueryTree consists of a tree of QueryPiece nodes connected by InterQuery edges.
 *   It represents how all the data in a view can be retrieved just using queries.
 *
 * QueryPiece
 *   A QueryPiece consists of a tree of QueryFragment nodes connected by IntraQuery edges.
 *   It represents a complete query that can be executed on a relational DB.
 *
 * QueryFragment
 *   A QueryFragment consists of all the pieces of information the user would like to
 *   retrieve from an entity.
 *   Each QueryFragment represents a distinct entity in the path.
 *   Embedded entities do not form a separate fragment but are coalesced into the owning entity.
 *
 * InterQuery
 *   It shows the dependency between 2 queries.
 *   This is an Edge in a tree of QueryPiece nodes that shows how 2 QueryPiece instances
 *   are connected.
 *   It might have additional details on how the dependent query needs to be executed with
 *   information provided by the depended upon query. For example, either through a sub-query
 *   or an IN list expression.
 *
 * IntraQuery
 *   Describes the foreign key relationship between 2 entities.
 *   It is an Edge in a tree of QueryFragment nodes that form a QueryPiece.
 *
 *
 * Algorithm:
 * ----------
 * 1. QueryFragment builder
 *    Initially the QueryTree consists of all the property paths.
 *    It is made up of a single QueryPiece, that contains a tree data structure
 *    made up of QueryFragment nodes and InterQuery edges for all the property paths.
 *    REF:
 *     FragmentBuilder
 * 2. Split into efficient queries
 *    This is analyzed and the necessary QueryPiece instances created.
 *    REF:
 *      CartesianJoinSplitter,
 *      NestedJoinSplitter,
 *      LoopSplitter
 * 3. Filter
 *    Add necessary filters to the QueryPiece and the placeholders in the QueryFragments
 *      setParameters
 * 3. Validation
 *    Based on the user input like ordering and paging, if it spans across
 *    queries then we need to error out while suggesting how the user can rectify the issue.
 *    REF:
 *      SortValidator
 *      PageValidator
 * 4. Execution
 *    Then each QueryPiece has its query built and executed in the order specified by
 *    its dependency in the tree of InterQuery edges.
 *    REF:
 *      QueryBuilder
 *      QueryDispatcher
 *        SerialDispatcher
 *        ParallelDispatcher
 *      ObjectResolver
 *
 * NOTE: Each step should have its own unit tests
 *
 *
 * AliasSplitter
 * -------------
 * This class creates a new QueryFragment with the alias name.
 * A single property can be represented with multiple fragments if there are multiple aliases.
 *
 * LoopSplitter
 * ------------
 * If a loop is detected, then it is the responsibility of the client to provide a
 * LoopResolver for that edge.
 * The loop resolver can be specified for the property in the join edge by the following API:
 * property.addLoopResolver(resolver)
 * The resolver has the following API:
 *   String unrollTo(int level)
 *
 * SortValidator
 * -------------
 * Checks to see that the ordering clause does not span properties belonging to different queries.
 *
 * PageValidator
 * -------------
 * Checks to see that the paging clause does not span properties belonging to different queries.
 *
 * QueryBuilder
 * ------------
 * Generates the literal OQL QueryString from a QueryPiece.
 * Binds it with the provided parameters.
 * Two types of queries are possible:
 *
 * 1. Objects with same ids are shared. The result will be a graph of information.
 *    To allow the identification of shareable objects, additional information will need to
 *    be fetched such as the id of the objects.
 * 2. Objects with same ids are not shared. This is faithful to what the customer requested.
 *    The data for the same object might occur multiple times in the result.
 *    The result becomes a tree of information.
 *
 * QueryDispatcher
 * ---------------
 * Based on the ordering specified by the InterQuery edges, the queries are executed using
 * the information from the depended upon query if applicable.
 *
 * A SerialDispatcher is used to execute the queries sequentially. Useful if they should all
 * belong to the same transaction.
 *
 * A ParallelDispatcher is used to execute all queries in the same level of the tree
 * in parallel. Useful if the data needs to be retrieved immediately and consistency can be
 * relaxed.
 *
 * ObjectResolver
 * --------------
 * Takes the ResultSet results and builds an Object graph out of it.
 * When a query for a QueryPiece is built, all the ids/natural keys for objects in the source
 * of an InterQuery edge needs to be also retrieved.
 *
 * EXAMPLE
 * =======
 *
 *
 * Uses a QueryPiece with an IntraQuery edge that contains alias information,
 * useful for queries.
 * Also, IntraQuery instances can have a custom implementation of the generated OQL query string.
 *
 * In the below example the IntraQuery instances contain the alias, name and other information:
 *
 * QueryPiece
 *   _path
 *
 *     S1[E0]                     S2[E1]
 *    -------------               --------
 *   | name        | ----------> | count  |
 *   | description |   details    --------
 *    -------------
 *
 *  _path is null for the root QueryTree, but is the ancestor path for the InterQuery edge
 *  where this QueryPiece attaches. All input property names are normalized using this (made
 *  relative).
 *
 * S1 and S2 are instance of the QueryFragment class. E0 and E1 are query aliases.
 *
 * This QueryPiece produces the following OQL query:
 *
 * SELECT E0.name, E0.description, E1.count FROM
 *   class1 E0 LEFT OUTER JOIN class2 E1 ON E1.id = E0.details
 *
 * There is a lot going on in this query. Let us break it down:
 * Both the IntraQuery and the QueryFragment produce OQL fragments. This information is taken
 * and the desired OQL query is built with additional information,
 * such as Ordering, paging and filters.
 *
 * Some of the APIs are:
 * IntraQuery e0;
 * e0.getOuterJoin() -> "LEFT OUTER JOIN class2 E1 ON E1.id = E0.details"
 *
 * QueryFragment s2;
 * s2.getSelectList() -> "E1.count"
 *
 * The modified QueryPiece (with IntraQuery edges) will select additional columns to help with
 * stitching results from different QueryPiece instances (broken for optimization reasons)
 * into the desired object.
 * These properties will be marked as not fetched.
 *
 * QueryPiece.ColumnMeta { position, fetch, name }
 *
 * ColumnMeta is used to reconstruct the result object from the ResultSet.
 * If this QueryPiece represents the target of an InterQuery edge, then it would contain the
 * information populated from the ResultSet to resolve the foreign key to the source of this edge.
 *
 * PAGING
 * ======
 * When pageColumn information is provided, we validate that the column indeed exists
 * in the QueryTree before adding this information into the query
 *
 * SORTING
 * =======
 * When ordering information is provided, we validate that the column indeed exists in the
 * QueryTree before adding this information into the query
 *
 * FILTERING
 * =========
 * Add the necessary WHERE conditions based on the provided filters
 *
 * Logging
 * =======
 * print - Print both the QueryPiece graph structure and the related OQL fragments produced by
 *        the query tree
 *
 *
 *
 * 
 * First Phase -- Restrict to simple needs 
 * 
 * Example filters
 * -----------------------------
 * ilike(param, %1)
 * in(param, %1)
 * idEq(%1)
 * ge(%1)
 * asc(%1)
 * 
 * e.g.
 * 
 * ilike(name, "%xyz%")
 * in("id", String[])
 * idEq(String)
 * ge("createdOn", long)
 * ge("updatedOn", long)
 * asc("name")
 * 
 * Modify view to support filters
 * ------------------------------
 * 
 * FILTERS
 * ilike(name, ":name")            // lower(name) LIKE :name
 * in(state, ":state"              // name IN(:state)
 * equal(ownedBy.name, ":owner")   // ownedBy.name = :owner 
 * ge(createdOn, ":createdSince")  // createdOn > :createdSince
 * ge(updatedOn, ":updatedSince"); // updaetdOn > :updatedSince
 * asc(name)                       // ORDER BY name  also honor OrderBy annotation in model if no ORDER BY clause is provided
 * between(updatedOn, ":updatedFrom", ":updatedTo")
 * 
 * */


public class QueryTransformer
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private BusinessObject   entity;
	private Type             type;	
	private QueryPiece       queryPiece;
	private List<Filter>     additionalFilters;
	private boolean          addIdentifier;
	private List<String>     selectedColumns;    

	public BusinessObject getEntity() {
		return entity;
	}	

	public boolean doAddIdentifier() {
		return addIdentifier;
	}

	public void setAddIdentifier(boolean addIdentifier) {
		this.addIdentifier = addIdentifier;
	}
	
	public void init(Type type, QueryPiece queryPiece, List<Filter> additionalFilters) {
		this.type = type;
		this.queryPiece = queryPiece;

		this.additionalFilters = new ArrayList<Filter>();
		for(Filter filter: additionalFilters) {
			Filter narrowedFilter = filter.copy();
			this.additionalFilters.add(narrowedFilter);	
		}	
		this.additionalFilters.addAll(queryPiece.getFilters());	
		
		Collections.sort(additionalFilters);
	}	

	public void init(BusinessObject entity, QueryPiece queryPiece, List<Filter> additionalFilters) {
		init(entity.getDomainType(), queryPiece, additionalFilters);
		this.entity = entity;
		
		if(entity.getIdentifierValue() != null) {
			addIdentifier = true;
		}
	}

	private QueryCapability getQueryCapability(PersistenceOrchestrator po)  {
		return po.getQueryCapability();
	}

	public Query constructQuery(Settings settings, Map<String, Object> filters) {
		return constructDML(queryPiece.getContentView(), settings, filters);
	}

	public Query constructDML(View view, Settings settings, Map<String, Object> filters) {
		//		First check for a StoredProcedure query, then a SQL query 
		//		When retrieving the QueryBuilder instance, registered SQL queries are given preference over HQL/JPQL queries.
		//
		//		It is the responsibility of the user to ensure that the registered SQL is ANSI compliant 
		//		and can work with all the databases that the user uses.
		
		if(view != null && view.getStoredProcedure() != null) {
			StoredProcedure querySP = view.getStoredProcedure(settings.getAction());

			if(querySP != null) {
				return settings.getPersistenceOrchestrator().getQuery(
					querySP.getName(),
					QueryType.SP,
					querySP,
					settings);
			}
		}

		if(view != null && view.getNativeQuery() != null) {
			String queryString = view.getNativeQuery().getQueryString();
			if(doAddIdentifier()) {
				String idClause = view.getNativeQuery().getIdentifierClause();
				if((idClause == null || idClause.trim().equals("")))
					throw new RuntimeException("Identifier clause expected in native query");

				queryString = queryString + " " + idClause + " = :" + QueryProperty.ID_PARAMETER_NAME;
			}

			queryString.replaceAll("[\n\r]", "");
			return settings.getPersistenceOrchestrator().getQuery(queryString, QueryType.SQL, view.getNativeQuery(), settings);
		}

		// User OQL
		if(view != null && view.getUserOQLQuery() != null) {
			String oqlString = view.getUserOQLQuery().getQueryString();
			return settings.getPersistenceOrchestrator().getQuery(oqlString, QueryType.OQL, view.getUserOQLQuery(), settings);
		}

		// System OQL
		StringBuilder oql = generateOQLQuery(settings, settings.getPersistenceOrchestrator(), filters);
		
		final Logger vb = LogManager.getLogger(Constants.Log.VIEW_BRANCH);
		if(vb.isDebugEnabled()) {
			vb.debug("OQL of view [" + view.getName() + "] => " + oql.toString());
		}

		return settings.getPersistenceOrchestrator().getQuery(
			oql.toString(),
			QueryType.OQL,
			null,
			settings);
	}
	
	public StringBuilder generateOQLQuery(Settings settings, PersistenceOrchestrator po, Map<String, Object> filters) {

		StringBuilder oql = new StringBuilder(constructOQL(settings, po));
		oql.append(buildWhereClause(settings, filters));
		oql.append(buildOrderClause(po));
		
		return oql;
	}
	
	private void debugSelectColumns(Map<String, ColumnMeta> meta) {
		final Logger vb = LogManager.getLogger(Constants.Log.VIEW_BRANCH);
		if(vb.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("********* C O L U M N   M E T A [" + queryPiece.getName() + "] *********\r\n");
			for(ColumnMeta columnMeta: meta.values()) {
				sb.append(columnMeta.toString() + "\r\n");
			}			
			vb.debug(sb.toString());
		}		
	}

	protected String constructOQL(Settings settings, PersistenceOrchestrator po) {
		Map<String, ColumnMeta> meta = queryPiece.getAugmentedAttributes();
/*
		StringBuilder OQL = new StringBuilder(SELECT_CLAUSE);

		int position = 0;
		selectedColumns = new ArrayList<String>();
		for(ColumnMeta columnMeta: meta.values()) {
			if(!columnMeta.getViewProperty().doFetch()) { // Should the property not be fetched
				continue;
			}

			columnMeta.setPosition(position++);	
			selectedColumns.add(QueryProperty.unqualifyProperty(columnMeta.getAttributePath()) );
			if(!SELECT_CLAUSE.equals(OQL.toString()))
				OQL.append(COMMA_DELIMITER);
			String columnString = columnMeta.getQueryString(getQueryCapability(po));
			OQL.append(columnString);
		}
		debugSelectColumns(meta);

		Class<?> entityClass = (settings == null || settings.getNarrowedClass() == null) ? type.getInstanceClass() : settings.getNarrowedClass();
		OQL.append(" FROM " + entityClass.getName() + " AS " + queryPiece.getViewProperty(QueryProperty.ROOT_PROPERTY_NAME).getAlias() + " " + po.getPolymorphicClause(entityClass));
		
		// Join explicitly against all open property types as the persistence layer does not know about these relationships
		for(QueryProperty viewProperty: queryPiece.getAliasedItems()) {
			
			if(viewProperty.getProperty() != null && viewProperty.getProperty().isOpenContent()) {
				ExtendedProperty extendedProperty = (ExtendedProperty) viewProperty.getProperty();
				
				// Represents the source type of the foreign key relationship
				// The source type is reversed based on whether this is a TO_ONE or TO_MANY relationship
				EntityType sourceType = null;
				if(extendedProperty.getRelationshipType() == RelationshipType.TO_ONE) {
					sourceType = (EntityType) extendedProperty.getType();
				} else if(extendedProperty.getRelationshipType() == RelationshipType.TO_MANY) {
					sourceType = (EntityType) extendedProperty.getElementType();
				}
				OQL.append("," + sourceType.getEntityName() + " AS " + viewProperty.getAlias());
			}
			// Also the WHERE clause now needs to have a condition to explicitly join with these entities
			// So we do the similar processing there
		}

		// if more than one collection property is specified then we would get a severe performance hit due to cartesian product
		// This is solved by breaking the query intelligently. 
		for(QueryProperty viewProperty: queryPiece.getAliasedItems()) {
			QueryProperty parentView = viewProperty.getParent();
			
			// Skip joining open content properties as the persistence layer does not know about these relationships
			if(parentView == null || (viewProperty.getProperty() != null && viewProperty.getProperty().isOpenContent()) ) {
				continue; // the root needs to be skipped
			}

			OQL.append(po.getOQLJoinFragment(viewProperty));
		}

		if(queryPiece.getContentView() != null && queryPiece.getContentView().getJoin() != null && queryPiece.getContentView().getJoin().getEntity() != null)
			OQL.append(", " + queryPiece.getContentView().getJoin().getEntity());		

		return OQL.toString();
		*/
		return "";
	}	

	protected String buildOrderClause(PersistenceOrchestrator po) {
		StringBuilder result = new StringBuilder();
/*
		Map<Integer, String> orderClauses = new TreeMap<Integer, String>();
		for(ColumnMeta columnMeta: queryPiece.getAugmentedAttributes().values()) {
			if(columnMeta.getAttributePath().endsWith(QueryProperty.LIST_INDEX_ATTRIBUTE)) {
				String columnString = columnMeta.getQueryString(getQueryCapability(po));
				orderClauses.put(columnMeta.getAttributeLevel(), columnString);
			}
		}

		// filter order clauses
		StringBuilder filterOrderBy = new StringBuilder();
		for(Filter filter: this.additionalFilters) {
			if(filter.isOrderBy()) {
				if(filterOrderBy.length() > 0)
					filterOrderBy.append(COMMA_DELIMITER);
				filterOrderBy.append(filter.getQueryString());
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
*/
		return result.toString();
	}

	protected String getNormalizedName(String propertyPath) {
		QueryProperty viewProperty = queryPiece.getViewProperty(QueryProperty.qualifyProperty(propertyPath));
		return viewProperty.getNormalizedName();
	}	
	
	protected void checkAndAddFilters(StringBuilder result, Map<String, Object> filters) {
		Map<String, Object> originalFilters = new HashMap<String, Object>(filters);
		filters.clear();
/*
		Map<String, Parameter> parameterMap = new HashMap<String, Parameter>();
		for(Parameter parameter: queryPiece.getParameter())
			parameterMap.put(parameter.name, parameter);
		
		// Initialize the filterbypath map
		for(Filter filter: this.additionalFilters) {

			// Order By filters are handled separately
			if(filter.isOrderBy()) {
				continue;
			}
			
			// Filter is skipped
			if(!filter.isFilterIncluded(originalFilters, filters, parameterMap))
				continue;
			
			addWhereStep(result);
			result.append(filter.getQueryString());
		}	
		
		// Set parameters referred from native query or join clause
		if(queryPiece.getContentView() != null && queryPiece.getContentView().getJoin() != null) {
			// Expression
			addWhereStep(result);

			for(Parameter parameter: queryPiece.getContentView().getJoin().getParameter()) {		
				if(originalFilters.containsKey(parameter.name)) {
					filters.put(parameter.name, originalFilters.get(parameter.name));
					result.append(parameter.expression);
				}
			}
		}
		*/
	}
	
	protected void checkAndAddId(StringBuilder result) {

		if(addIdentifier) {
			addWhereStep(result);

			result.append( getNormalizedName(((EntityType)entity.getDomainType()).getIdentifierProperty().getName()) );
			result.append( " = :" + QueryProperty.ID_PARAMETER_NAME);			
		}
	}
	
	private String getDirection(Filter filter) {
		if(filter.functionExpression instanceof AscFunctionExpression) {
			return " > ";
		} else {
			return " < ";
		}
	}
	
	private String getEqualOrderByExp(int endPos, List<Filter> orderBy) {
		// 	Create an equals condition for all the orderBy fields until endPos
		
		StringBuilder result = new StringBuilder("");
		for(int i = 0; i <= endPos; i++) {
			Filter f = orderBy.get(i);
			if(i > 0) {
				result.append(" AND ");
			}
			result.append( f.getNormalizedName() + " = : " + QueryProperty.NEXTTOKEN_PARAM_PREFIX + f.getAttribute());
		}
		
		return result.toString();
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
		
		List<Filter> orderBy = new LinkedList<Filter>();
		for(Filter filter: this.additionalFilters) {
			if(filter.isOrderBy()) {
				orderBy.add(filter);
			}
		}		
		
		addWhereStep(queryString);
		queryString.append( " ( ");		
		for(int i = 0; i < orderBy.size(); i++) {
			Filter f = orderBy.get(i);
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

	protected String buildWhereClause(Settings settings, Map<String, Object> filters) {
		StringBuilder result = new StringBuilder();
		
		checkAndAddFilters(result, filters);
		checkAndAddId(result);
		checkAndAddChunkStart(settings, result);
		checkAndAddOpenPropertyJoins(result);

		return result.toString();
	}	
	
	private void checkAndAddOpenPropertyJoins(StringBuilder result) {
		StringBuilder whereFragment = new StringBuilder("");
		/*
		for(QueryProperty viewProperty: queryPiece.getAliasedItems()) {
			
			if(viewProperty.getProperty() != null && viewProperty.getProperty().isOpenContent()) {
				ExtendedProperty extendedProperty = (ExtendedProperty) viewProperty.getProperty();
				
				// Represents the source type of the foreign key relationship
				// The source type is reversed based on whether this is a TO_ONE or TO_MANY relationship
				Map<String, String> keyFields = extendedProperty.getKeyFields();
				
				if(viewProperty.getParent() == null) {
					throw new IllegalStateException("The open property cannot be the root");
				}
				
				String sourceAlias = null;
				String targetAlias = null;
				
				if(extendedProperty.getRelationshipType() == RelationshipType.TO_ONE) {
					sourceAlias = viewProperty.getParent().getAlias();
					targetAlias = viewProperty.getAlias();
				} else if(extendedProperty.getRelationshipType() == RelationshipType.TO_MANY) {
					sourceAlias = viewProperty.getAlias();
					targetAlias = viewProperty.getParent().getAlias();
				}					
				
				if(sourceAlias != null && targetAlias != null) {
					int index = 0;

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
		}		
		if(whereFragment.length() > 0) {
			addWhereStep(result);
			result.append(whereFragment);
		}
		*/
	}

	private void addWhereStep(StringBuilder result) {
		if(result.length() > 0)
			result.append(" AND ");		
		else
			result.append(" WHERE ");		
	}

	public void postProcess(QueryPiece queryView, Settings settings, Query query, Map<String, Object> filters) {
		
		for(Map.Entry<String, Object> entry: filters.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}

		if(doAddIdentifier() && query.hasParameter(QueryProperty.ID_PARAMETER_NAME)) {
			query.setParameter(QueryProperty.ID_PARAMETER_NAME, getEntity().getIdentifierValue());
		}

		// Set the chunk values
		Map<String, Object> nextToken = settings.getNextToken();
		if(nextToken != null) {
			for(Map.Entry<String, Object> entry: nextToken.entrySet()) {
				if(!query.hasParameter(QueryProperty.NEXTTOKEN_PARAM_PREFIX + entry.getKey())) {
					throw new IllegalStateException("NextToken missing information for orderBy field: " + entry.getKey());
				}
				query.setParameter(QueryProperty.NEXTTOKEN_PARAM_PREFIX + entry.getKey(), entry.getValue());
			}
		}

		// pagination
		if(settings.getOffset() != null)
			query.setFirstResult(settings.getOffset());
		if(settings.getLimit() != null)
			query.setMaxResults(settings.getLimit());
		
		// initialize the query with the selected columns
		query.setColumns(selectedColumns);

		EntityType entityType = (EntityType)((entity == null) ? type : entity.getDomainType());
		query.prepare(entityType, queryView);
	}
}
