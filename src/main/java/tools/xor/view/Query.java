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
import java.util.Set;

import tools.xor.Settings;
import tools.xor.service.DataStore;

public interface Query extends DML {

	public static final String INTERQUERY_JOIN_PLACEHOLDER = "^PLACEHOLDER^";
	public static final String QUERY_JOIN_TABLENAME = "XOR_QUERY_JOIN_";

	public static boolean isDeferred(String queryString) {
		return queryString.contains(Query.INTERQUERY_JOIN_PLACEHOLDER);
	}

	public static DataStore.QueryType getQueryType(Query query) {
		if(query.isOQL()) {
			return DataStore.QueryType.OQL;
		} else if(query.isSQL()) {
			return DataStore.QueryType.SQL;
		} else {
			return DataStore.QueryType.SP;
		}
	}

	/**
	 * Get the result from the query
	 * @param view of this operation
	 * @param settings for this operation
	 * @return query result
	 */
	@SuppressWarnings("rawtypes")
	List getResultList(View view, Settings settings);

	/**
	 * Get single result from the query
	 * @param view of this operation
	 * @param settings for this operation
	 * @return single result object
	 */
	Object getSingleResult(View view, Settings settings);
	
	/**
	 * set the limit for the number of returned results
	 * @param limit value
	 */
	void setMaxResults(int limit);
	
	/**
	 * Set the starting position in the result set
	 * @param offset value
	 */
	void setFirstResult(int offset);

	/**
	 * Get a list of the columns selected by this query
	 * @return list of columns
	 */
	List<String> getColumns();

	/**
	 * Get the position a particular attribute path is located
	 * @param path column in the select query
	 * @return position starting from 0
	 */
	int getColumnPosition(String path);

	/**
	 * Return the string representation of the query
	 * @return query string
	 */
	String getQueryString ();

	/**
	 * Return the string representation of the query
	 * @return query string
	 */
	/**
	 * Set the query string for this Query object
	 * @param string containing the query
	 */
	void setQueryString (String string);

	/**
	 * Set the list of columns selected by this query
	 * @param columns to set
	 */
	void setColumns(List<String> columns);

	/**
	 * Update the bind parameters
	 * @param bindParams bind parameters 
	 */
	void updateParamMap (List<BindParameter> bindParams);

	/**
	 * Checks if the query is an OQL
	 * @return true if this is the case
	 */
	boolean isOQL();

	/**
	 * Checks if the query is an SQL
	 * @return true if this is the case
	 */
	boolean isSQL();

	/**
	 * Used if the query needs to run multiple instance of an IN list batch
	 * @param values list size is greater than QueryTreeInvocation#MAX_INLIST_SIZE
	 */
	public void processLargeInList(Set values);

	/**
	 * Returns true if the creation of the provider query object needs to be delayed.
	 * This can happen if the place holder string is part of the query.
	 * The resolution of the placeholder occurs after the parent query is executed.
	 *
	 * @return true if the query is deferred
	 */
	public boolean isDeferred();

	// Extract the parameters and create them

	/**
	 * Extract the parameters from the query
	 * @return modifed query string with bind placeholders
	 */
	String extractParameters ();
}
