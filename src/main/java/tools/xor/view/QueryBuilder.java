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
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.PersistenceOrchestrator.QueryType;
import tools.xor.service.QueryCapability;
import tools.xor.util.Constants;
import tools.xor.view.expression.AscFunctionExpression;

/*
 * QueryBuilder needs to handle the following expressions
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

public class QueryBuilder {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private static final String SELECT_CLAUSE = "SELECT ";
	private static final String COMMA_DELIMITER = ", ";

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
			Filter narrowedFilter = filter.narrow();
			this.additionalFilters.add(narrowedFilter);	
		}	
		this.additionalFilters.addAll(queryPiece.getFilters());	
		
		Collections.sort(additionalFilters);
	}	

	public void init(BusinessObject entity, QueryPiece queryPiece, List<Filter> additionalFilters) {
		init(entity.getDomainType(), queryPiece, additionalFilters);
		this.entity = entity;
		queryPiece.normalizeFilters(this.additionalFilters, getQueryCapability(entity.getObjectCreator().getPersistenceOrchestrator()));
		
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
	}	

	protected String buildOrderClause(PersistenceOrchestrator po) {
		StringBuilder result = new StringBuilder();

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

		return result.toString();
	}

	protected String getNormalizedName(String propertyPath) {
		QueryProperty viewProperty = queryPiece.getViewProperty(QueryProperty.qualifyProperty(propertyPath));
		return viewProperty.getNormalizedName();
	}	
	
	protected void checkAndAddFilters(StringBuilder result, Map<String, Object> filters) {
		Map<String, Object> originalFilters = new HashMap<String, Object>(filters);
		filters.clear();

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
