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

public interface Query {

	/**
	 * Get the result from the query
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List getResultList(QueryView queryView); 

	/**
	 * Get single result from the query
	 */
	public Object getSingleResult();
	
	/**
	 * set the value for a parameter
	 * @param value
	 */
	public void setParameter(String name, Object value);
	
	/**
	 * Indicates if the parameter with the specified name is defined in the query
	 * @param name
	 * @return
	 */
	public boolean hasParameter(String name);
	
	/**
	 * set the limit for the number of returned results
	 */
	public void setMaxResults(int limit);
	
	/**
	 * Set the starting position in the result set
	 */
	public void setFirstResult(int offset);

	/**
	 * Get a list of the columns selected by this query
	 * @return
	 */
	public List<String> getColumns();

	/**
	 * Set the list of columns selected by this query
	 * @param columns
	 */
	public void setColumns(List<String> columns);

	/**
	 * Give a chance to initialize the query provider
	 */
	public void prepare(EntityType entityType, QueryView queryView);
}
