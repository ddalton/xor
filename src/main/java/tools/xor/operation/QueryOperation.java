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

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ProcessingStage;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.view.Query;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryView;

public class QueryOperation extends AbstractOperation {

	private List<Object> result = new ArrayList<Object>();
	
	protected Type getNarrowedClass(DataAccessService das, Settings settings) {
		TypeMapper typeMapper = das.getTypeMapper();
		return das.getType(typeMapper.toDomain(settings.getNarrowedClass()));
	}

	@Override
	public void execute(CallInfo callInfo) {
		BusinessObject sourceEntity = (BusinessObject) callInfo.getInput();
		DataAccessService das = sourceEntity.getObjectCreator().getDAS();
		QueryBuilder qb = das.getQueryBuilder();
		
		// Always use the REFERENCE type
		Type referenceType = (callInfo.getSettings().getNarrowedClass() == null) ? ((BusinessObject) callInfo.getInput()).getDomainType() : getNarrowedClass(das, callInfo.getSettings());
		QueryView aggregateView = callInfo.getSettings().getView().getEntityView( referenceType, callInfo.getSettings().doNarrow() );
		Map<BusinessObject, Object> uniqueList = new LinkedHashMap<BusinessObject, Object>();	
		
		// Put in loop if there are sub-branches
		if(aggregateView.getSubBranches().size() > 1) {
			for(QueryView branch: aggregateView.getSubBranches())
				executeBranch(branch, uniqueList, qb, callInfo);
		} else {
			executeBranch(aggregateView, uniqueList, qb, callInfo);
		}
		
		// Do a second pass
		for(BusinessObject root: uniqueList.keySet())
			if(root.getContainer() == null) // Only add root objects
				result.add(root);
	}
	
	protected Query createQuery(QueryView queryView, CallInfo callInfo, QueryBuilder qb) {
		Map<String, Object> mutableFilters = new HashMap<String, Object>(callInfo.getSettings().getFilters());
		Query query = qb.constructQuery(callInfo, mutableFilters);

		qb.postProcess(queryView, callInfo, query, mutableFilters);
		
		return query;				
	}
	
	protected void checkSecurity(QueryView branch, CallInfo callInfo) {
		if(branch.isCrossAggregate() && !callInfo.getSettings().permitCrossAggregate())
			throw new RuntimeException("The view crosses aggregate boundary, this could be a security risk. If this is intentional then permit this by modifying the settings");
	}
	
	private void executeBranch(QueryView branch, Map<BusinessObject, Object> uniqueList, QueryBuilder qb, CallInfo callInfo) {
		qb.init((BusinessObject) callInfo.getInput(), branch, callInfo.getSettings().getAdditionalFilters());	
		Query query = createQuery(branch, callInfo, qb);
		
		try {
			checkSecurity(branch, callInfo);
			
			// club all the results relevant to the same entity
			// add an id attribute for the mail entity. Add an owner attribute for each collection property referenced.
			// adjust the properties and for every new attribute added (id or owner) create a filler/dummy property column in the view
			List tempResult = query.getResultList(branch);
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
	
	@Override
	protected boolean supportsCreate(CallInfo callInfo) {
		return false;
	}	
	
	@Override
	protected boolean supportsUpdate(CallInfo callInfo) {
		return false;
	}		
	
	@Override
	protected void postVisited(CallInfo ci) {
		if(ci.getStage() == ProcessingStage.CREATE && ci.getSettings().doBaseline() ) {
			ci.getOutputObjectCreator().getPersistenceOrchestrator().attach((BusinessObject) ci.getOutput(), ci.getSettings().getView());
		}
	}	
}
