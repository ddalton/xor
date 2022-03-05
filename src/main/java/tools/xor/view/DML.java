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

import tools.xor.EntityType;
import tools.xor.Settings;

public interface DML {

	/**
	 * set the value for a parameter
	 * @param name of parameter
	 * @param value of parameter
	 */
	public void setParameter(String name, Object value);
	
	/**
	 * Indicates if the parameter with the specified name is defined in the query
	 * @param name of parameter
	 * @return true if parameter is present
	 */
	public boolean hasParameter(String name);
	
	/**
	 * Give a chance to initialize the query provider
	 * @param entityType of entity
	 * @param queryTree query object
	 */
	public void prepare(EntityType entityType, QueryTree queryTree);

	/**
	 * Executes the DML statement
	 * @param settings object containing NativeQuery and AggregateAction values
	 * @return any results if a query or row count if an update
	 */
	public Object execute(Settings settings);
}
