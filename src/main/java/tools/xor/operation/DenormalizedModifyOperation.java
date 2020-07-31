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

import java.util.HashMap;
import java.util.Map;

import tools.xor.Settings;
import tools.xor.util.ClassUtil;
import tools.xor.view.DML;
import tools.xor.view.QueryTransformer;

public class DenormalizedModifyOperation extends GraphTraversal {
	
	// Represents a list of map objects
	private Object result;

	@Override
	public void execute(Settings settings) {
		QueryTransformer qb = new QueryTransformer();
		DML dml = createDML(settings, qb);

		try {
			result = dml.execute(settings);
		}
		catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	protected DML createDML(Settings settings, QueryTransformer qb) {
		Map<String, Object> mutableFilters = new HashMap<String, Object>(settings.getParams());

		DML dml = qb.constructDML(settings.getView(), settings);
		for(Map.Entry<String, Object> entry: mutableFilters.entrySet()) {
			dml.setParameter(entry.getKey(), entry.getValue());
		}

		return dml;
	}

	@Override
	public Object getResult() {
		return result;
	}		
}
