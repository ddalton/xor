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
import java.util.List;
import java.util.Map;

import tools.xor.EntityType;
import tools.xor.Settings;

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

	protected void setPositionalParameters (Settings settings,
											Map<Integer, Object> positionalParameters,
											Map<Integer, BindParameter> paramMap,
											PreparedStatement statement)
	{
		if (positionalParameters != null) {
			for (Map.Entry<Integer, Object> entry : positionalParameters.entrySet()) {
				if (!paramMap.containsKey(entry.getKey())) {
					throw new RuntimeException(
						"Unable to find parameterList with key: " + entry.getKey());
				}
				// Note JDBC positional parameters start from 1
				BindParameter pm = paramMap.get(entry.getKey());

				int timestampType = BindParameter.getType(pm.type);
				if (timestampType == Types.TIMESTAMP
					|| timestampType == Types.TIMESTAMP_WITH_TIMEZONE) {
					pm.setDateFormat(settings.getDateFormat());
				}
				pm.setValue(statement, entry.getValue());
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
		List result = new ArrayList();
		while (rs.next()) {

			Object[] row = extractRow(rs);
			result.add(row);
		}

		return result;
	}

	public static Object[] extractRow(ResultSet rs) throws SQLException
	{
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();

		Object[] row = new Object[columnCount];
		for(int i = 0; i < columnCount; i++) {
			// Get the value from the ResultSet, JDBC columnIndex starts from 1
			row[i] = BindParameter.getValue(
				rsmd.getColumnType(i + 1),
				rs,
				i + 1);
		}

		return row;
	}
}
