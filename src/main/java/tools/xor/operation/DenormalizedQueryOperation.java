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
import java.util.List;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.Type;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.view.NativeQuery;
import tools.xor.view.OQLQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryView;
import tools.xor.view.StoredProcedure;
import tools.xor.view.StoredProcedureQuery;

public class DenormalizedQueryOperation extends QueryOperation {
	
	// Represents a list of map objects
	private List<Object[]> result = new ArrayList<Object[]>();

	@Override
	public void execute(CallInfo callInfo) {
		BusinessObject sourceEntity = (BusinessObject) callInfo.getInput();
		DataAccessService das = sourceEntity.getObjectCreator().getDAS();
		QueryBuilder qb = das.getQueryBuilder();
		
		// Always use the REFERENCE type
		Type referenceType = (callInfo.getSettings().getNarrowedClass() == null) ? ((BusinessObject) callInfo.getInput()).getDomainType() : getNarrowedClass(das, callInfo.getSettings());
		QueryView aggregateView = callInfo.getSettings().getView().getEntityView( referenceType, callInfo.getSettings().doNarrow() );
		
		// We do not support Native SQL queries with sub-branches
		if(aggregateView.getSubBranches().size() > 1) {
			throw new RuntimeException("Denormalized queries not supported with sub-branch views");
		}
		
		execute(aggregateView, qb, callInfo);
	}

	private void execute(QueryView branch, QueryBuilder qb, CallInfo callInfo) {
		qb.init((BusinessObject) callInfo.getInput(), branch, callInfo.getSettings().getAdditionalFilters());	
		Query query = createQuery(branch, callInfo, qb);

		try {
			checkSecurity(branch, callInfo);

			NativeQuery nativeQuery = branch.view().getNativeQuery();
			OQLQuery userOQLQuery = branch.view().getUserOQLQuery();
			List<String> selectedColumns;
			if(nativeQuery != null) {
				selectedColumns = nativeQuery.getResultList();
			} else if(userOQLQuery != null) {
				selectedColumns = branch.getContentView().getAttributeList();
			} else {
				if(query instanceof StoredProcedureQuery) {
					selectedColumns = ((StoredProcedureQuery) query).getStoredProcedure().getResultList();
				} else {
					selectedColumns = query.getColumns();
				}
			}
			result.add(selectedColumns.toArray());

			for(Object obj: query.getResultList(branch)) {

				if(ClassUtil.getDimensionCount(obj) == 1) {
					Object[] objArray = (Object[]) obj;
					if(selectedColumns.size() != objArray.length) {
						throw new RuntimeException("The view column count is not the same as the query column count");
					}
					result.add(objArray);
				} else {
					if(selectedColumns.size() != 1) {
						throw new RuntimeException("The view has does not have a single column specified, but the result has only one column");
					}
					result.add(new Object[] { obj });
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
