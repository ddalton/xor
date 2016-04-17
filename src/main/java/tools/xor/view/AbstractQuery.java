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

import java.util.List;

import tools.xor.EntityType;

public abstract class AbstractQuery implements Query {
	
	private List<String> columns;
	
	@Override
	public List<String> getColumns() {
		return this.columns;
	}

	@Override
	public void setColumns(List<String> columns) {
		this.columns = columns;
	}	
	
	@Override
	public void prepare(EntityType entityType, QueryView queryView) {
		// nothing to prepare for a SQL query, but StoredProcedure needs to be prepared
	}
}
