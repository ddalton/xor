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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.ParameterMode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AggregateAction;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Settings;
import tools.xor.providers.jdbc.DBTranslator;
import tools.xor.util.ClassUtil;

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

 *
 */
public class StoredProcedureQuery extends AbstractQuery {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private StoredProcedure sp;
	private DBTranslator translator;
	private Map<String, Object> paramValues = new HashMap<>();
	
	// If a stored procedure is returning multiple results and an error occurs,
	// this variable helps to debug on which result call the error occurred
	private int resultCount = 0;

	public StoredProcedureQuery(StoredProcedure sp) {
		// Stored procedure does not have a query string
		super(null);

		this.sp = sp;

		if(sp != null) {
			QueryStringHelper.initPositionalParamMap(positionByName, sp.parameterList, true);
		}

		populateDefaultValues();
	}

	private void populateDefaultValues() {
		// NOTE: if a parameter is being used in multiple positions it should be using the
		//       same value. So looking at the first item is sufficient.
		for(Map.Entry<String, List<BindParameter>> entry: positionByName.entrySet()) {
			BindParameter bp = entry.getValue().get(0);
			if(bp.type != null && bp.getDefaultValue() != null) {
				paramValues.put(entry.getKey(), bp.getDefaultValue());
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
	public List getResultList(View view, Settings settings)
	{
		// Implicit SQL cannot have bind parameters
		if(!sp.implicit) {
			QueryStringHelper.setParameters(
				settings,
				(PreparedStatement)sp.getStatement(),
				positionByName,
				paramValues);
		}
		return (List)execute(view, AggregateAction.READ);
	}

	private void initPosition(Map<Integer, View> viewPosition, View view) {
		viewPosition.put(view.getResultPosition(), view);

		if(view.getChildren() != null) {
			for(View child: view.getChildren()) {
				initPosition(viewPosition, child);
			}
		}
	}

	private void setResults(View view, List results) {
		if(view instanceof AggregateView) {
			((AggregateView)view).setResults(results);
		} else {
			throw new RuntimeException("A custom view being modified should be an instance of AggregateView");
		}
	}

	public Object execute(View view, AggregateAction action) {
		Map<Integer, View> positionView = new HashMap<>();
		Map<String, View> viewParam = new HashMap<>();

		initPosition(positionView, view);

		List<OutputLocation> ol = sp.getOutputLocation();
		if(ol != null) {
			for(OutputLocation loc: ol) {
				if(loc.getPosition() >= 0) {
					throw new RuntimeException("A view using an output parameter should have a negative position");
				}
				viewParam.put(loc.getParameter(), positionView.get(loc.getPosition()));
			}
		} else {
			positionView.put(0, view);
		}

		try {
			ResultSet rs = null;
			boolean hasResults;
			multiple: do {
				if (resultCount == 0) {
					// When requesting the result for the first time, we execute
					// the query
					// The stored procedure only need to contain queries
					if(sp.isImplicit()) {
						hasResults = sp.getStatement().execute(sp.getCallString());
					} else {
						hasResults = ((CallableStatement)sp.getStatement()).execute();
					}

					if(!sp.isImplicit()) {
						// Look for the result from an OUT parameter.
						for (BindParameter param : sp.parameterList) {
							if (param.mode == ParameterMode.OUT || param.mode == ParameterMode.INOUT) {
								if(viewParam.containsKey(param.name)) {

									// get cursor and cast it to ResultSet
									rs = (ResultSet)((CallableStatement)sp.getStatement()).getObject(
										param.position);
									setResults(viewParam.get(param.name), extractResults(rs));

									// got the result, we don't want to break in case the SP has side effects
									// break multiple;
								}
							}
						}
					}

				} else {
					// Only makes sense for implicit results
					hasResults = sp.getStatement().getMoreResults();
				}

				if(hasResults) {
					rs = sp.getStatement().getResultSet();
				}

				// Each view specifies the output location
				if (positionView.containsKey(resultCount)) {
					setResults(positionView.get(resultCount), extractResults(rs));

					// got the result, we don't want to break in case the SP has side effects
					// break multiple;
				}

				if(hasResults) {
					resultCount++;
				}

			} while (hasResults || sp.getStatement().getUpdateCount() != -1);

		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		} finally {
			try {
				sp.getStatement().close();
			} catch (SQLException e) {
				throw ClassUtil.wrapRun(e);
			}
		}

		List result = ((AggregateView)view).getResults();
		return result;
	}

	@Override
	/**
	 * @throws javax.persistence.NoResultException if there is no result
	 * @throws javax.persistence.NonUniqueResultException if more than one result
	 */
	public Object getSingleResult(View view, Settings settings) {
		List result = getResultList(view, settings);
		if(result.size() == 0) {
			throw new NoResultException();
		}
		if(result.size() > 1) {
			throw new NonUniqueResultException();
		}

		return result.get(0);
	}

	private DBTranslator getTranslator() {
		if(this.translator == null) {
			this.translator = DBTranslator.getTranslator(sp.getStatement());
		}

		return this.translator;
	}

	@Override
	public void setParameter(String name, Object value) {

		if(positionByName.containsKey(name)) {
			paramValues.put(name, value);
		}
/*
		List<BindParameter> params = positionByName.get(name);
		for(BindParameter param: params) {
			param.setValue((CallableStatement)sp.getStatement(), getTranslator(), value);
		}

 */
	}

	@Override
	public boolean hasParameter(String name) {
		return positionByName.containsKey(name);
	}

	@Override
	public void setMaxResults(int limit) {
		if(sp.getMaxResults() != null) {
			try {
				sp.statement.setMaxRows(Integer.valueOf(sp.getMaxResults()));
			} catch (SQLException e) {
				logger.warn("Unable to set maxRows: " + e.getMessage());
			}
		}
	}

	@Override
	public void setFirstResult(int offset) {
		throw new UnsupportedOperationException("The setFirstResult is currently unsupported for stored procedures");
	}

	@Override public void updateParamMap (List<BindParameter> relevantParams)
	{
		QueryStringHelper.initPositionalParamMap(positionByName, relevantParams, true);
	}

	@Override public boolean isOQL ()
	{
		return false;
	}

	@Override public boolean isSQL ()
	{
		return false;
	}

	@Override
	public void prepare(EntityType entityType, QueryTree queryTree) {

		// If this belongs to a SP_MULTI query then the root query has already
		// been executed, so nothing to do here
		if(sp == null) {
			return;
		}

		// A Java Type can map to multiple SQL types, so it is mandatory for the user
		// to specify the parameter type
		if(sp.getParameterList() != null) {
			for(BindParameter param: sp.getParameterList()) {
				
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

	@Override public Object execute (Settings settings)
	{
		// TODO: support object modification
		return null;
	}

	private void registerOutParameter (int position, BindParameter param)
	{
		try {

			int sqlType = 0;
			CallableStatement callableStatement = (CallableStatement)sp.statement;
			try {
				// Obtain a dialect specific type e.g., OracleTypes.CURSOR
				sqlType = Integer.parseInt(param.type);
			}
			catch (NumberFormatException e) {
				// Fallback to java.sql.Types
				sqlType = (int)java.sql.Types.class.getDeclaredField(param.type).get(null);
			}

			if (sqlType == java.sql.Types.NUMERIC || sqlType == java.sql.Types.DECIMAL) {
				callableStatement.registerOutParameter(position, sqlType, param.scale);
			}
			else {
				callableStatement.registerOutParameter(position, sqlType);
			}
		}
		catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	public boolean isDeferred() {
		return false;
	}
}
