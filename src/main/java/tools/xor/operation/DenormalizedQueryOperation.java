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
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.view.NativeQuery;
import tools.xor.view.OQLQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryBuilder;
import tools.xor.view.QueryTree;
import tools.xor.view.StoredProcedureQuery;
import tools.xor.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DenormalizedQueryOperation extends QueryOperation {

	// Represents a list of map objects
	private List<Object[]> result = new ArrayList<Object[]>();

	@Override
	public void execute (CallInfo callInfo)
	{
		BusinessObject sourceEntity = (BusinessObject)callInfo.getInput();
		DataAccessService das = sourceEntity.getObjectCreator().getDAS();
		QueryBuilder qb = das.getQueryBuilder();

		// Always use the REFERENCE type
		Type referenceType = (callInfo.getSettings().getNarrowedClass() == null) ?
			((BusinessObject)callInfo.getInput()).getDomainType() :
			getNarrowedClass(das, callInfo.getSettings());
		QueryTree queryTree = callInfo.getSettings().getView().getEntityView(
			referenceType,
			callInfo.getSettings().doNarrow());

		// We do not support Native SQL queries with sub-branches
		if (queryTree.getOutEdges(queryTree.getRoot()).size() > 1) {
			throw new RuntimeException("Denormalized queries not supported with sub-branch views");
		}

		qb.init(
			(BusinessObject)callInfo.getInput(),
			queryTree.getRoot(),
			callInfo.getSettings().getAdditionalFilters());
		Query query = createQuery(queryTree.getRoot(), callInfo, qb);
		execute(query, callInfo.getSettings());
	}

	@Override
	public void execute(Settings settings, DataAccessService das) {
		QueryBuilder qb = das.getQueryBuilder();
		Query query = createQuery(settings, qb);
		execute(query, settings);
	}

	protected Query createQuery(Settings settings, QueryBuilder qb) {
		Map<String, Object> mutableFilters = new HashMap<String, Object>(settings.getFilters());
		Query query = qb.constructDML(settings.getView(), settings, mutableFilters);

		for(Map.Entry<String, Object> entry: mutableFilters.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}

		return query;
	}

	private void execute(Query query, Settings settings) {

		try {
			View view = settings.getView();
			NativeQuery nativeQuery = view.getNativeQuery();
			OQLQuery userOQLQuery = view.getUserOQLQuery();
			List<String> selectedColumns;

			if (nativeQuery != null) {
				selectedColumns = nativeQuery.getResultList();
			}
			else if (userOQLQuery != null) {
				selectedColumns = view.getAttributeList();
			}
			else {
				if (query instanceof StoredProcedureQuery) {
					selectedColumns = ((StoredProcedureQuery)query).getStoredProcedure().getResultList();
				}
				else {
					selectedColumns = query.getColumns();
				}
			}
			if (selectedColumns != null && selectedColumns.size() > 0) {
				result.add(selectedColumns.toArray());
			}

			boolean processingFirstRow = true;
			for (Object obj : query.getResultList(view, settings)) {

				if (ClassUtil.getDimensionCount(obj) == 1) {
					Object[] objArray = (Object[])obj;

					// If selectedColumns is not initialized then we initialize it
					if (processingFirstRow) {
						if (selectedColumns == null || selectedColumns.size() == 0) {
							if (query.getColumns() != null) {
								result.add(query.getColumns().toArray());
							}
							else {
								selectedColumns = new ArrayList<>(objArray.length);
								for (int i = 0; i < objArray.length; i++) {
									selectedColumns.add(i, "Col " + (i + 1));
								}
								result.add(selectedColumns.toArray());
							}
						}
						else if (selectedColumns.size() != objArray.length) {
							throw new RuntimeException(
								"The view column count is not the same as the query column count");
						}
						processingFirstRow = false;
					}
					result.add(objArray);
				}
				else {
					if (selectedColumns.size() != 1) {
						throw new RuntimeException(
							"The view has does not have a single column specified, but the result has only one column");
					}
					result.add(new Object[] { obj });
				}
			}
		}
		catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public Object getResult() {
		return result;
	}
}

