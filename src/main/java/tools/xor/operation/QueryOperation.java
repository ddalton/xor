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

package tools.xor.operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.TypeMapper;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.util.InterQuery;
import tools.xor.view.ObjectResolver;
import tools.xor.view.Query;
import tools.xor.view.QueryDispatcher;
import tools.xor.view.QueryProperty;
import tools.xor.view.QueryTransformer;
import tools.xor.view.QueryPiece;
import tools.xor.view.QueryTree;
import tools.xor.view.SerialDispatcher;

/**
 * Needs to handle both the types of QueryTree instances:
 * 1. Flattened
 * 2. Nested
 *  
 * @author Dilip Dalton
 *
 */
public class QueryOperation extends TreeTraversal implements ObjectResolver
{

	private List<Object> result = new ArrayList<Object>();
	private BusinessObject   entity;
	Map<BusinessObject, Object> uniqueList = new LinkedHashMap<BusinessObject, Object>();

	public BusinessObject getEntity() {
		return entity;
	}
	
	protected tools.xor.Type getNarrowedClass(DataAccessService das, Settings settings) {
		TypeMapper typeMapper = das.getTypeMapper();
		return das.getType(typeMapper.toDomain(settings.getNarrowedClass()));
	}

	@Override
	public void execute(CallInfo callInfo) {
		this.entity = (BusinessObject) callInfo.getInput();
		assert entity != null : "Entity information is required.";

		DataAccessService das = this.entity.getObjectCreator().getDAS();
		QueryTransformer qb = das.getQueryBuilder();
		
		// Always use the REFERENCE type
		tools.xor.Type referenceType = (callInfo.getSettings().getNarrowedClass() == null) ? ((BusinessObject) callInfo.getInput()).getDomainType() : getNarrowedClass(das, callInfo.getSettings());
		QueryTree<QueryPiece, InterQuery<QueryPiece>> queryTree = callInfo.getSettings().getView().getEntityView( referenceType, callInfo.getSettings().doNarrow() );

		QueryDispatcher dispatcher = new SerialDispatcher();
		dispatcher.execute(queryTree, this, callInfo);
	}

	@Override
	public void postProcess() {
		for(BusinessObject root: uniqueList.keySet()) {
			if (root.getContainer() == null) {
				// Only add root objects
				result.add(root);
			}
		}
	}

	@Override
	public void processRecords(List records, QueryPiece queryPiece, CallInfo callInfo) {
		
		try {
			// club all the results relevant to the same entity
			// add an id attribute for the mail entity. Add an owner attribute for each collection property referenced.
			// adjust the properties and for every new attribute added (id or owner) create a filler/dummy property column in the view
			for(Object obj: records) {
				BusinessObject newRootObject = queryPiece.getRootObject(
					obj,
					(BusinessObject)callInfo.getOutput());

				if(ClassUtil.getDimensionCount(obj) == 1) {
					queryPiece.normalize(newRootObject, (Object[])obj);
					if(newRootObject.getContainer() == null && !uniqueList.containsKey(newRootObject)) // Only add root objects
						uniqueList.put(newRootObject, null);
				}
			}
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}		
	}

	@Override
	public Object getResult() {
		return result;
	}

	@Override
	public void preProcess(QueryPiece qp, Settings settings, Query query, Map<String, Object> params) {
		for(Map.Entry<String, Object> entry: params.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}

		if(getEntity().getIdentifierValue() != null && query.hasParameter(QueryProperty.ID_PARAMETER_NAME)) {
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
		query.setColumns(qp.getFieldNames());
		query.prepare((EntityType)entity.getType(), qp);
	}
}
