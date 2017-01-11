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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Store;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.ParameterMode;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Type;
import tools.xor.util.ClassUtil;
import tools.xor.view.OutputLocation.OutputType;

/**
The syntax for invoking a stored procedure in JDBC is shown below. Note that the square brackets indicate that what is between them is optional; they are not themselves part of the syntax.

    {call procedure_name[(?, ?, ...)]}
The syntax for a procedure that returns a result parameter is:
    {? = call procedure_name[(?, ?, ...)]}
The syntax for a stored procedure with no parameters would look like this:
    {call procedure_name}

A stored procedure can be overloaded, so we will provide an option for the user to specify the callString
{call getorders(?::INT)}
{call getorders(?::DATE)}

NOTE: the child AggregateViews need to be ordered such that the resultsets are processed before reading any of the OUT parameters
 *
 */
public class StoredProcedureQuery extends AbstractQuery {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private StoredProcedure sp;
	private Map<String, ParameterMapping> paramMap = new HashMap<String, ParameterMapping>();
	
	// If a stored procedure is returning multiple results and an error occurs,
	// this variable helps to debug on which result call the error occurred
	private int resultCount = 1;

	public StoredProcedureQuery(StoredProcedure sp) {

		this.sp = sp;
		
		int position = 1;
		if (sp.parameterList != null) {
			for (ParameterMapping param : sp.parameterList) {
				param.position = position;
				String attrName = param.attribute;
				if (attrName == null) {
					attrName = param.name;
				}
				paramMap.put(attrName, param);
			}
		}
	}

	public StoredProcedure getStoredProcedure() {
		return this.sp;
	}

	@SuppressWarnings("rawtypes")
	@Override
	/**
	 * This method is not idempotent, i.e., it does not return the
	 * same results in multiple calls.
	 * The reason is because stored procedures can return multiple resultSets
	 * and this method can be used to get the next one on each subsequent call,
	 * so the result for each call can be different based on how the stored
	 * procedure is written.
	 * This allows helps to support multiple viewBranch, with each viewBranch
	 * mapping to a different resultSet.
	 */
	public List getResultList(QueryView viewBranch) {
		OutputLocation ol = sp.getOutputLocation().get(resultCount-1);
		List result = new ArrayList();
		try {
			boolean hasMoreResults = false;
			if(resultCount++ == 1) {
				// When requesting the result for the first time, we execute
				// the query
				hasMoreResults = sp.getCallableStatement().execute();
			}
			
			// Each viewBranch specifies the output location
			ResultSet rs = null;
			if(ol.getType() == OutputType.RETURN) {
				// getMoreResults after this call will need to be invoked if the stored procedure
				// is returning more result sets. Since this is not very reliable
				// will not implement it by default and will consider adding it only through
				// a config parameter if needed.
				rs = (ResultSet)sp.getCallableStatement().getObject(1);

			} else {
				// Look for the result from an OUT parameter.
				for (ParameterMapping param : sp.parameterList) {
					if (param.mode == ParameterMode.OUT || param.mode == ParameterMode.INOUT) {

						// get cursor and cast it to ResultSet
						rs = (ResultSet) sp.getCallableStatement().getObject(param.position);
						break;
					}
				}
			}

			// If the resultlist is empty from the StoredProcedure then
			// get the types from the AggregateView
			List<Type> attributeTypes = getAttributeTypes(viewBranch);
			if(attributeTypes.isEmpty()) {
				attributeTypes = viewBranch.getAttributeTypes();
			}

			while(rs.next()) {
				// The size should match with the max columns in the result set
				Object[] row = new Object[attributeTypes.size()];
				result.add(row);

				int columnIndex = 0;
				for (Type type : attributeTypes) {
					// Get the value from the ResultSet, JDBC columnIndex starts from 1
					row[columnIndex++] = ParameterMapping.getValue(
						type.getInstanceClass(),
						rs,
						columnIndex);
				}
			}

		} catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}

		return result;
	}

	@Override
	/**
	 * @throws javax.persistence.NoResultException if there is no result
	 * @throws javax.persistence.NonUniqueResultException if more than one result
	 */
	public Object getSingleResult(QueryView queryView) {
		List result = getResultList(queryView);
		if(result.size() == 0) {
			throw new NoResultException();
		}
		if(result.size() > 1) {
			throw new NonUniqueResultException();
		}

		return result.get(0);
	}

	@Override
	public void setParameter(String name, Object value) {
		ParameterMapping param = paramMap.get(name);
		param.setValue(sp.getCallableStatement(), value);
	}

	@Override
	public boolean hasParameter(String name) {
		return paramMap.containsKey(name);
	}

	@Override
	public void setMaxResults(int limit) {
		if(sp.getMaxResults() != null) {
			try {
				sp.callableStatement.setMaxRows(Integer.valueOf(sp.getMaxResults()));
			} catch (SQLException e) {
				logger.warn("Unable to set maxRows: " + e.getMessage());
			}
		}
	}

	@Override
	public void setFirstResult(int offset) {
		throw new UnsupportedOperationException("The setFirstResult is currently unsupported for stored procedures");
	}	
	
	@Override
	public void prepare(EntityType entityType, QueryView queryView) {

		// A Java Type can map to multiple SQL types, so it is mandatory for the user
		// to specify the parameter type
		if(sp.getParameterList() != null) {
			for(ParameterMapping param: sp.getParameterList()) {
				
				if(param.mode == ParameterMode.OUT || param.mode == ParameterMode.INOUT) {
					if(param.type == null) {
						throw new RuntimeException("type is mandatory for OUT/INOUT parameter [name: " + param.name + ", attribute: " + param.attribute + "]");
					}
					registerOutParameter(param.position, param);
				} else { // IN parameters
					if(param.attribute != null) {
						// Value taken from the entity attribute
						ExtendedProperty p = (ExtendedProperty) entityType.getProperty(param.attribute);
						if(!p.getType().isDataType()) {
							throw new RuntimeException("The attribute should refer to a simple type");
						}
					}
				}
			}
		}
	}

	private void registerOutParameter (int position, ParameterMapping param)
	{
		try {

			int sqlType = 0;
			try {
				// Obtain a dialect specific type e.g., OracleTypes.CURSOR
				sqlType = Integer.parseInt(param.type);
			}
			catch (NumberFormatException e) {
				// Fallback to java.sql.Types
				sqlType = (int)java.sql.Types.class.getDeclaredField(param.type).get(null);
			}

			if (sqlType == java.sql.Types.NUMERIC || sqlType == java.sql.Types.DECIMAL) {
				sp.callableStatement.registerOutParameter(position, sqlType, param.scale);
			}
			else {
				sp.callableStatement.registerOutParameter(position, sqlType);
			}
		}
		catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}
	
	private List<Type> getAttributeTypes(QueryView viewBranch) {
		int initialCapacity = 0;
		if(sp.getResultList() != null) {
			initialCapacity = sp.getResultList().size();
		}
		
		List<Type> result = new ArrayList<>(initialCapacity);
		for(String attr: sp.getResultList()) {
			ExtendedProperty p = (ExtendedProperty) viewBranch.getAggregateType().getProperty(attr);
			if(!p.getType().isDataType()) {
				throw new RuntimeException("The attribute should refer to a simple type");
			}
			result.add(p.getType());
		}
		
		return result;
	}
}
