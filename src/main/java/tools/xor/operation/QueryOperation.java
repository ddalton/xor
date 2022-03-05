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
import tools.xor.UnchangedTypeMapper;
import tools.xor.service.DataModel;
import tools.xor.util.ClassUtil;
import tools.xor.util.InterQuery;
import tools.xor.view.AggregateTree;
import tools.xor.view.ObjectResolver;
import tools.xor.view.ParallelDispatcher;
import tools.xor.view.Query;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryDispatcher;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;
import tools.xor.view.QueryTreeInvocation;
import tools.xor.view.SerialDispatcher;

/**
 * Needs to handle both the types of QueryTree instances:
 * 1. Flattened
 * 2. Nested
 * 
 * QueryOperation does not involved DTO copying as the
 * reconstituted object is directly returned to the user.
 * 
 * Operates on a Query By Example pattern.
 * So the user has to provide an input object and its entityType in the settings object.
 * 
 * NOTE: If the user would like the object result from the query operation to be 
 *   in the shape of the QueryType then the QueryType needs to be passed in as the entityType
 *   in the settings object.
 *   In some cases (QueryType is basedOn entityType), this restriction can be bypassed during
 *   reconstitution since the property is found from the root QueryFragment of the QueryTree, 
 *   and since all the types are represented as QueryType instances in QueryTree, the correct property is found. 
 *  
 * @author Dilip Dalton
 *
 */
public class QueryOperation extends TreeTraversal implements ObjectResolver
{

	private List<Object> result = new ArrayList<Object>();
	private BusinessObject   entity;
	Map<BusinessObject, Object> uniqueList = new LinkedHashMap<>();

	public BusinessObject getEntity() {
		return entity;
	}
	
	protected tools.xor.Type getDowncast (DataModel das, Settings settings) {
		TypeMapper typeMapper = das.getTypeMapper();

		if(typeMapper instanceof UnchangedTypeMapper) {
			return settings.getEntityType();
		}

		return das.getShape().getType(typeMapper.toDomain(settings.getDowncastName()));
	}

	protected void validate() {
	}

	@Override
	public void execute(CallInfo callInfo) {
		this.entity = (BusinessObject) callInfo.getInput();
		assert entity != null : "Entity information is required.";

		DataModel das = this.entity.getObjectCreator().getDataModel();
		
		// Always use the REFERENCE type
		tools.xor.Type referenceType = (callInfo.getSettings().getDowncastName() == null) ? ((BusinessObject) callInfo.getInput()).getDomainType() : getDowncast(
			das,
			callInfo.getSettings());
		AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = callInfo.getSettings().getView().getAggregateTree(
			referenceType);

		// Construct the query based on the settings
		QueryBuilder builder = new QueryBuilder(aggregateTree, this.entity);
		builder.construct(callInfo.getSettings());

		QueryDispatcher dispatcher = ClassUtil.doParallelDispatch() ? new ParallelDispatcher(aggregateTree, this, callInfo) : new SerialDispatcher(aggregateTree, this, callInfo);
		dispatcher.execute();
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
	public void notify (BusinessObject anchorObject,
								boolean isRoot)
	{
		if (isRoot && anchorObject.getContainer() == null
			&& !uniqueList.containsKey(
			anchorObject)) {
			// Only add root objects
			uniqueList.put(anchorObject, null);
		}
	}

	@Override
	public Object getResult() {
		return result;
	}

	@Override
	public void preProcess(QueryTree queryTree, Settings settings, QueryTreeInvocation qti, InterQuery parentEdge) {
		Query query = queryTree.getQuery();
		super.preProcess(settings, query, parentEdge == null);

		if(getEntity().getIdentifierValue() != null && query.hasParameter(QueryFragment.ID_PARAMETER_NAME)) {
			query.setParameter(QueryFragment.ID_PARAMETER_NAME, getEntity().getIdentifierValue());
		}

		if(query.hasParameter(QueryFragment.PARENT_INVOCATION_ID_PARAM)) {
			if(parentEdge == null) {
				throw new RuntimeException(String.format("Found %s parameter when this is not a child query", QueryFragment.PARENT_INVOCATION_ID_PARAM));
			}
			String invocationId = qti.getInvocationId((QueryTree)parentEdge.getStart());
			if(invocationId == null) {
				throw new RuntimeException("The parent query is not yet executed");
			}
			query.setParameter(QueryFragment.PARENT_INVOCATION_ID_PARAM, invocationId);
		}

		if(query.hasParameter(QueryFragment.INVOCATION_ID_PARAM)) {
			// It is not necessary for the child query to be executed
			String invocationId = qti.getOrCreateInvocationId(queryTree);
			query.setParameter(QueryFragment.INVOCATION_ID_PARAM, invocationId);
		}
		
        if(query.hasParameter(QueryFragment.LAST_PARENT_ID_PARAM)) {
            Object lastParentId = qti.getLastParentId(queryTree);
            query.setParameter(QueryFragment.LAST_PARENT_ID_PARAM, lastParentId);
        }		

		// initialize the query with the selected columns
		query.prepare((EntityType)entity.getType(), queryTree);
	}
}
