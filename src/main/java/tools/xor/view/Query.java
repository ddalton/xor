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
import tools.xor.Settings;

public interface Query extends DML {

	/**
	 * Get the result from the query
	 * @param view of this operation
	 * @param settings for this operation
	 * @return query result
	 */
	@SuppressWarnings("rawtypes")
	public List getResultList(View view, Settings settings);

	/**
	 * Get single result from the query
	 * @param view of this operation
	 * @param settings for this operation
	 * @return single result object
	 */
	public Object getSingleResult(View view, Settings settings);
	
	/**
	 * set the limit for the number of returned results
	 * @param limit value
	 */
	public void setMaxResults(int limit);
	
	/**
	 * Set the starting position in the result set
	 * @param offset value
	 */
	public void setFirstResult(int offset);

	/**
	 * Get a list of the columns selected by this query
	 * @return list of columns
	 */
	public List<String> getColumns();

	/**
	 * Set the list of columns selected by this query
	 * @param columns to set
	 */
	public void setColumns(List<String> columns);
}
