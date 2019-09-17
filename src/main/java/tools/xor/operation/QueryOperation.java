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

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.TypeMapper;
import tools.xor.UnchangedTypeMapper;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.util.InterQuery;
import tools.xor.view.ObjectResolver;
import tools.xor.view.Query;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryDispatcher;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;
import tools.xor.view.AggregateTree;
import tools.xor.view.QueryTreeInvocation;
import tools.xor.view.SerialDispatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	Map<BusinessObject, Object> uniqueList = new LinkedHashMap<>();

	public BusinessObject getEntity() {
		return entity;
	}
	
	protected tools.xor.Type getNarrowedType (DataAccessService das, Settings settings) {
		TypeMapper typeMapper = das.getTypeMapper();

		if(typeMapper instanceof UnchangedTypeMapper) {
			return settings.getEntityType();
		}

		return das.getShape().getType(typeMapper.toDomain(settings.getNarrowedClass()));
	}

	protected void validate() {
	}

	@Override
	public void execute(CallInfo callInfo) {
		this.entity = (BusinessObject) callInfo.getInput();
		assert entity != null : "Entity information is required.";

		DataAccessService das = this.entity.getObjectCreator().getDAS();
		
		// Always use the REFERENCE type
		tools.xor.Type referenceType = (callInfo.getSettings().getNarrowedClass() == null) ? ((BusinessObject) callInfo.getInput()).getDomainType() : getNarrowedType(
			das,
			callInfo.getSettings());
		AggregateTree<QueryTree, InterQuery<QueryTree>> aggregateTree = callInfo.getSettings().getView().getAggregateTree(
			das,
			referenceType,
			callInfo.getSettings().doNarrow());

		// Construct the query based on the settings
		QueryBuilder builder = new QueryBuilder(aggregateTree, this.entity);
		builder.construct(callInfo.getSettings());

		QueryDispatcher dispatcher = new SerialDispatcher();
		dispatcher.execute(aggregateTree, this, callInfo);
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
	public void processRecords(List records, QueryTree queryTree, CallInfo callInfo, QueryTreeInvocation queryInvocation) {
		
		try {
			// club all the results relevant to the same entity
			// add an id attribute for the mail entity. Add an owner attribute for each collection property referenced.
			// adjust the properties and for every new attribute added (id or owner) create a filler/dummy property column in the view
			Map<String, Object> previous = null;
			for(Object obj: records) {
				if(ClassUtil.getDimensionCount(obj) == 1) {
					// TODO: For child queries the root/anchor object should be the parent object
					// TODO: for that query edge
					// TODO: The parent id is the root id of the child query tree
					// TODO: Use this id to get the parent objects (anchor objects) from the
					// TODO: QueryTreeInvocation
					List<BusinessObject> anchorObjects = queryTree.getRootObjects(
						obj,
						(BusinessObject)callInfo.getOutput(),
						queryInvocation);
					for(BusinessObject anchorObject: anchorObjects) {

						previous = queryTree.resolveField(
							anchorObject,
							(Object[])obj,
							previous,
							queryInvocation);

						// TODO: get the list with empty path
						if (anchorObject.getContainer() == null && !uniqueList.containsKey(
							anchorObject)) {
							// Only add root objects
							uniqueList.put(anchorObject, null);
						}
					}
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
	public void preProcess(QueryTree queryTree, Settings settings) {
		Query query = queryTree.getQuery();
		super.preProcess(settings, query);

		if(getEntity().getIdentifierValue() != null && query.hasParameter(QueryFragment.ID_PARAMETER_NAME)) {
			query.setParameter(QueryFragment.ID_PARAMETER_NAME, getEntity().getIdentifierValue());
		}

		// initialize the query with the selected columns
		query.prepare((EntityType)entity.getType(), queryTree);
	}
}
