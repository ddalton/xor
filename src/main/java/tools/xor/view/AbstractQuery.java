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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.providers.jdbc.DBTranslator;

public abstract class AbstractQuery implements Query {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	final static Pattern paramPattern = Pattern.compile( ":(\\w+)" );
	
	private List<String> columns;
	private Map<String, Integer> columnMap;
	private String queryString;
	private List<Map<String, Object>> batches;

	// We always refer to bind parameters by name
	// Even positional parameters in the query need to have a name mapped
	protected Map<String, List<BindParameter>> positionByName = new HashMap<>(); // for direct JDBC using named parameters
	
	@Override
	public List<String> getColumns() {
		return this.columns;
	}

	@Override
	public String getQueryString () {
		return this.queryString;
	}

	public AbstractQuery(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public void setColumns(List<String> columns) {
		this.columns = new ArrayList<>(columns);

		this.columnMap = new HashMap<>();
		for(int i = 0; i < columns.size(); i++) {
			columnMap.put(columns.get(i), i);
		}
	}

	@Override
	public int getColumnPosition(String path) {
		if(columnMap.containsKey(path)) {
			return columnMap.get(path);
		} else {
			return -1;
		}
	}
	
	@Override
	public void prepare(EntityType entityType, QueryTree queryTree) {
		// nothing to prepare for a SQL query, but StoredProcedure needs to be prepared
	}

	protected void setBindParameter(int position, Object value) {

	}

	protected void setParameters (Settings settings,
								  Map<String, Object> paramValues)
	{
		if (positionByName != null) {
			for (Map.Entry<String, List<BindParameter>> entry : positionByName.entrySet()) {
				String paramName = entry.getKey();
				if (!paramValues.containsKey(paramName)) {
					throw new RuntimeException(
						"Unable to find param value with key: " + paramName);
				}

				List<BindParameter> params = entry.getValue();
				Object value = paramValues.get(paramName);
				for(BindParameter bindParam: params) {
					if (bindParam.type != null) {
						int timestampType = BindParameter.getType(bindParam.type);
						if (timestampType == Types.TIMESTAMP
							|| timestampType == Types.TIMESTAMP_WITH_TIMEZONE) {
							bindParam.setDateFormat(settings.getDateFormat());
						}
					}
					setBindParameter(bindParam.position, value);
				}
			}
		}
	}

	/**
	 * It is straight forward to discover the insert column types
	 * @param connection JDBC connection object
	 * @param sqlQuery SQL INSERT statment
	 * @return list of ParameterMapping objects for each of the columns
	 * @throws SQLException when getting metadata
	 */
	protected List<BindParameter> autoDiscoverInsert(Connection connection, String sqlQuery) throws SQLException
	{
		// Replace consecutive spaces with a single space
		String selectColumnQuery = sqlQuery.trim().replaceAll("\\s+", " ").toUpperCase();

		// Remove the values portion
		selectColumnQuery = selectColumnQuery.substring(0, selectColumnQuery.indexOf("VALUES"));

		// Check if we need all columns are only some of them
		String columns = "*";
		if(selectColumnQuery.contains("(") && selectColumnQuery.contains(")")) {
			columns = selectColumnQuery.substring(selectColumnQuery.indexOf("(")+1,selectColumnQuery.indexOf(")"));
		}
		selectColumnQuery = selectColumnQuery.replace("INSERT INTO ", "SELECT " + columns + " FROM ");
		if(selectColumnQuery.contains("(")) {
			selectColumnQuery = selectColumnQuery.substring(0, selectColumnQuery.indexOf("("));
		}
		System.out.println("Select query: " + selectColumnQuery);

		PreparedStatement ps = connection.prepareStatement(selectColumnQuery);
		ResultSetMetaData rsmd = ps.getMetaData();

		List<BindParameter> list = new ArrayList<>(rsmd.getColumnCount());
		for(int i = 0; i < rsmd.getColumnCount(); i++) {
			BindParameter pm = new BindParameter();
			pm.setType(new Integer(rsmd.getColumnType(i+1)).toString());
			pm.setName(rsmd.getColumnName(i+1));
			list.add(pm);
		}

		return list;
	}

	public static List extractResults(ResultSet rs) throws SQLException
	{
		DBTranslator translator = DBTranslator.getTranslator(rs.getStatement());
		List result = new ArrayList();
		while (rs.next()) {

			Object[] row = extractRow(rs, translator);
			result.add(row);
		}

		return result;
	}

	public static Object[] extractRow(ResultSet rs, DBTranslator translator) throws SQLException
	{
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();

		Object[] row = new Object[columnCount];
		for(int i = 0; i < columnCount; i++) {
			// Get the value from the ResultSet, JDBC columnIndex starts from 1
			row[i] = BindParameter.getValue(
				translator,
				rsmd.getColumnType(i + 1),
				rs,
				i + 1);
		}

		return row;
	}

	@Override
	public void processLargeInList(Set values) {
		int numBatches = values.size()/QueryTreeInvocation.MAX_INLIST_SIZE + 1;
		this.batches = new ArrayList<>(numBatches);

		Iterator iter = values.iterator();
		for(int i = 0; i < numBatches; i++) {
			Map<String, Object> batch = new HashMap<>();
			batches.add(batch);
			int start = QueryTreeInvocation.OFFSET;

			Object value = null;
			while(start % (QueryTreeInvocation.MAX_INLIST_SIZE+1) != 0) {
				// If there are no more values we use the last value
				// to pad the IN list of the last batch
				if(iter.hasNext()) {
					value = iter.next();
				}
				batch.put(QueryFragment.PARENT_INLIST + start++, value);
			}
		}
	}

	protected List getResultListInternal(View view, Settings settings) {
		throw new UnsupportedOperationException("The implementation is required or the getResultList needs to overridden");
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getResultList(View view, Settings settings) {
		if(batches == null) {
			return getResultListInternal(view, settings);
		} else {
			List result = new LinkedList<>();
			for(Map<String, Object> batch: batches) {
				for(Map.Entry<String, Object> entry: batch.entrySet()) {
					setParameter(entry.getKey(), entry.getValue());
				}
				result.addAll(getResultListInternal(view, settings));
			}
			return result;
		}
	}

	// Extract the parameters and create them
	// The named parameter is converted to a positional parameter
	@Override
	public String extractParameters () {
		positionByName.clear();

		final Matcher matcher = paramPattern.matcher(getQueryString());

		StringBuffer modifiedSQL = new StringBuffer();
		int position = 1; // JDBC param number starts from 1
		while (matcher.find()) {
			//System.out.println("Full match: " + matcher1.group(0));
		    
			if(matcher.group(1) != null) {
				String paramName = matcher.group(1);

				// All the positional parameters for a single name
				List<BindParameter> params = positionByName.get(paramName);
				if(params == null) {
					params = new ArrayList<>();
					positionByName.put(paramName, params);
				}

				BindParameter bindParam = BindParameter.instance(position++, paramName);
				params.add(bindParam);
				// Create an ordinal parameter as required by JPQL
	            matcher.appendReplacement(modifiedSQL, getOrdinalParameter(bindParam));
			} else {
			    logger.error("Problem in extracting the parameter: " + getQueryString());
			}
		}
		matcher.appendTail(modifiedSQL);

		return modifiedSQL.toString();
	}
	
	protected String getOrdinalParameter(BindParameter bindParam) {
	    return "?"+bindParam.getPosition();
	}
}
