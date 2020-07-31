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
import java.util.List;
import java.util.Map;

import tools.xor.CallInfo;
import tools.xor.Settings;
import tools.xor.util.ClassUtil;
import tools.xor.view.Query;
import tools.xor.view.QueryTransformer;
import tools.xor.view.View;

public class DenormalizedQueryOperation extends AbstractOperation {

	// Represents a list of map objects
	private List<Object[]> result = new ArrayList<Object[]>();

	@Override public void execute (CallInfo callInfo)
	{
		execute(callInfo.getSettings());
	}

	@Override
	public void execute(Settings settings) {

		QueryTransformer qb = new QueryTransformer();
		Query query = createQuery(settings, qb);
		execute(query, settings);
	}

	protected Query createQuery(Settings settings, QueryTransformer qb) {
		Map<String, Object> mutableFilters = new HashMap<String, Object>(settings.getParams());
		Query query = qb.constructDML(settings.getView(), settings);

		for(Map.Entry<String, Object> entry: mutableFilters.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
		}

		preProcess(settings, query);

		return query;
	}

	private void execute(Query query, Settings settings) {

		try {
			View view = settings.getView();
			List<String> selectedColumns = view.getAttributeList();

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

	public void preProcess(Settings settings, Query query)
	{
		super.preProcess(settings, query, true);
	}

	@Override
	public Object getResult() {
		return result;
	}
}

