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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.util.Edge;
import tools.xor.view.Query;
import tools.xor.view.QueryTransformer;
import tools.xor.view.QueryPiece;
import tools.xor.view.QueryTree;
import tools.xor.view.StoredProcedure;

/**
 * Needs to handle both the types of QueryTree instances:
 * 1. Flattened
 * 2. Nested
 *  
 * @author Dilip Dalton
 *
 */
public class QueryOperation extends TreeTraversal {

	private List<Object> result = new ArrayList<Object>();
	
	protected Type getNarrowedClass(DataAccessService das, Settings settings) {
		TypeMapper typeMapper = das.getTypeMapper();
		return das.getType(typeMapper.toDomain(settings.getNarrowedClass()));
	}

	@Override
	public void execute(CallInfo callInfo) {
		BusinessObject sourceEntity = (BusinessObject) callInfo.getInput();
		DataAccessService das = sourceEntity.getObjectCreator().getDAS();
		QueryTransformer qb = das.getQueryBuilder();
		
		// Always use the REFERENCE type
		Type referenceType = (callInfo.getSettings().getNarrowedClass() == null) ? ((BusinessObject) callInfo.getInput()).getDomainType() : getNarrowedClass(das, callInfo.getSettings());
		QueryTree queryTree = callInfo.getSettings().getView().getEntityView( referenceType, callInfo.getSettings().doNarrow() );
		Map<BusinessObject, Object> uniqueList = new LinkedHashMap<BusinessObject, Object>();

		// Put in loop if there are sub-branches
		if(queryTree.getOutEdges(queryTree.getRoot()).size() > 1) {
			StoredProcedure sp = queryTree.getRoot().getContentView().getStoredProcedure(AggregateAction.READ);
			if(sp != null && sp.isMultiple()) {
				executeBranch(queryTree.getRoot(), uniqueList, qb, callInfo);
			} else {
				for (Object edge : queryTree.getOutEdges(queryTree.getRoot())) {
					QueryPiece branch = ((Edge<QueryPiece>)edge).getEnd();
					executeBranch(branch, uniqueList, qb, callInfo);
				}
			}
		} else {
			executeBranch(queryTree.getRoot(), uniqueList, qb, callInfo);
		}
		
		// Do a second pass
		for(BusinessObject root: uniqueList.keySet())
			if(root.getContainer() == null) // Only add root objects
				result.add(root);
	}
	
	protected Query createQuery(QueryPiece queryPiece, CallInfo callInfo, QueryTransformer qb) {
		Map<String, Object> mutableFilters = new HashMap<String, Object>(callInfo.getSettings().getFilters());
		Query query = qb.constructQuery(callInfo.getSettings(), mutableFilters);

		qb.postProcess(queryPiece, callInfo.getSettings(), query, mutableFilters);
		
		return query;				
	}

	protected Query getQueryInstance(QueryPiece branch, QueryTransformer qb, CallInfo callInfo) {
		qb.init((BusinessObject) callInfo.getInput(), branch, callInfo.getSettings().getAdditionalFilters());
		Query query = createQuery(branch, callInfo, qb);

		return query;
	}

	private void executeBranch(QueryPiece branch, Map<BusinessObject, Object> uniqueList, QueryTransformer qb, CallInfo callInfo) {

		Query query = getQueryInstance(branch, qb, callInfo);
		
		try {
			
			// club all the results relevant to the same entity
			// add an id attribute for the mail entity. Add an owner attribute for each collection property referenced.
			// adjust the properties and for every new attribute added (id or owner) create a filler/dummy property column in the view
			List tempResult = query.getResultList(branch.getContentView(), callInfo.getSettings());
			for(Object obj: tempResult) {
				BusinessObject newRootObject = branch.getQueryRoot(obj, (BusinessObject) callInfo.getOutput());

				if(ClassUtil.getDimensionCount(obj) == 1) {
					branch.normalize(newRootObject, (Object[])obj);
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
}
