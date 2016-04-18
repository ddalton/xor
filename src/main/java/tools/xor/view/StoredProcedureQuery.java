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
			if(resultCount++ == 1) {
				// When requesting the result for the first time, we execute
				// the query
				sp.getCallableStatement().execute();
			}
			
			// Each viewBranch specifies the output location 
			//TODO: Dilip
			//StoredProcedure outputSP = viewBranch.
			if(ol.getType() == OutputType.RETURN) {
				// Handle getMoreResults after this call
				ResultSet rs = sp.getCallableStatement().getResultSet();
				
				// If the resultlist is empty from the StoredProcedure then
				// get the types from the AggregateView
				List<Type> attributeTypes = getAttributeTypes(viewBranch);
				if(attributeTypes.isEmpty()) {
					attributeTypes = viewBranch.getAttributeTypes();
				}

				// The size should match with the max columns in the result set
				Object[] row = new Object[attributeTypes.size()];
				
				int columnIndex = 0;
				for(Type type: attributeTypes) {
					// Get the value from the ResultSet, JDBC columnIndex starts from 1
					// TODO: Dilip
					//row[columnIndex++] = type.getValue(rs, columnIndex);
				}

			} else {
				/*TODO: Dilip
				sp.getCallableStatement()
				Object obj = spQuery.getOutputParameterValue(sp.getOutputLocation().getParameter());
				if(obj instanceof List) {
					return (List) obj;
				} else {
					List result = new ArrayList();
					result.add(obj);
					return result;
				}
				*/
			}
		} catch (SQLException e) {
			ClassUtil.wrapRun(e);
		}
		
		//TODO: Dilip
		return result;
		
		/*
		 * Look at http://ahexamples.blogspot.com/2014/05/example-of-java-jdbc-call.html
		 * 
		 		try {
			final CallableStatement statement = (CallableStatement) getSession().getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( call, true );


			// prepare parameters
			int i = 1;

			for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
				parameter.prepare( statement, i );
				if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
					i++;
				}
				else {
					i += parameter.getSqlTypes().length;
				}
			}

			return new ProcedureOutputsImpl( this, statement );
			
			NOTE: look at executeQuery instead of execute for READ action
			Also, support multiple result sets, by this way, multiple round trips to the DB is avoided.
		 */
	}

	@Override
	public Object getSingleResult() {
		//TODO: Dilip
		//return spQuery.getSingleResult();
		throw new UnsupportedOperationException();
	}

	@Override
	public void setParameter(String name, Object value) {
		ParameterMapping param = paramMap.get(name);
		//TODO: Dilip
		//param.type.setSQLValue(sp.getCallableStatement(), param.position, value);
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
		/*
		 * Needed to set IN, INOUT - setParameter
		 * OUT, INOUT - this method
		 * 
		 	@Override
	public void prepare(CallableStatement statement, int startIndex) throws SQLException {
		// initially set up the Type we will use for binding as the explicit type.
		Type typeToUse = hibernateType;
		int[] sqlTypesToUse = sqlTypes;

		// however, for Calendar binding with an explicit TemporalType we may need to adjust this...
		if ( bind != null && bind.getExplicitTemporalType() != null ) {
			if ( Calendar.class.isInstance( bind.getValue() ) ) {
				switch ( bind.getExplicitTemporalType() ) {
					case TIMESTAMP: {
						typeToUse = CalendarType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( session().getFactory() );
						break;
					}
					case DATE: {
						typeToUse = CalendarDateType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( session().getFactory() );
						break;
					}
					case TIME: {
						typeToUse = CalendarTimeType.INSTANCE;
						sqlTypesToUse = typeToUse.sqlTypes( session().getFactory() );
						break;
					}
				}
			}
		}

		this.startIndex = startIndex;
		if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
			if ( mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
				if ( sqlTypesToUse.length > 1 ) {
					// there is more than one column involved; see if the Hibernate Type can handle
					// multi-param extraction...
					final boolean canHandleMultiParamExtraction =
							ProcedureParameterExtractionAware.class.isInstance( hibernateType )
									&& ( (ProcedureParameterExtractionAware) hibernateType ).canDoExtraction();
					if ( ! canHandleMultiParamExtraction ) {
						// it cannot...
						throw new UnsupportedOperationException(
								"Type [" + hibernateType + "] does support multi-parameter value extraction"
						);
					}
				}
				for ( int i = 0; i < sqlTypesToUse.length; i++ ) {
					statement.registerOutParameter( startIndex + i, sqlTypesToUse[i] );
				}
			}

			if ( mode == ParameterMode.INOUT || mode == ParameterMode.IN ) {
				if ( bind == null || bind.getValue() == null ) {
					// the user did not bind a value to the parameter being processed.  That might be ok *if* the
					// procedure as defined in the database defines a default value for that parameter.
					// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
					// parameter defines a default value.  So we simply allow the procedure execution to happen
					// assuming that the database will complain appropriately if not setting the given parameter
					// bind value is an error.
					log.debugf(
							"Stored procedure [%s] IN/INOUT parameter [%s] not bound; assuming procedure defines default value",
							procedureCall.getProcedureName(),
							this
					);
				}
				else {
					typeToUse.nullSafeSet( statement, bind.getValue(), startIndex, session() );
				}
			}
		}
		else {
			// we have a REF_CURSOR type param
			if ( procedureCall.getParameterStrategy() == ParameterStrategy.NAMED ) {
				session().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( statement, getName() );
			}
			else {
				session().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( statement, startIndex );
			}
		}
	}
		 */
		
		
		if(sp.getParameterList() != null) {
			for(ParameterMapping param: sp.getParameterList()) {
				if(param.type == null || param.type == void.class) {
					// set the type
					if(param.attribute == null) {
						throw new RuntimeException("For non-attribute parameters the type has to be specified, since it cannot be inferred");
					}
					ExtendedProperty p = (ExtendedProperty) entityType.getProperty(param.attribute);
					if(!p.getType().isDataType()) {
						throw new RuntimeException("The attribute should refer to a simple type");
					}
					param.type = p.getType().getInstanceClass();
				}
				
				if(param.mode == ParameterMode.OUT || param.mode == ParameterMode.INOUT) {
					registerOutParameter(param.position, param);
				}
			}
		}
	}	
	
	private void registerOutParameter(int position, ParameterMapping param) {
		/* TODO: Dilip
		if(param.type.getSQLType() == java.sql.Types.NUMERIC || param.type.getSQLType() == java.sql.Types.DECIMAL) {
			sp.callableStatement.registerOutParameter(position, param.type.getSQLType(), param.scale);
		} else {
			sp.callableStatement.registerOutParameter(position, param.type.getSQLType());
		}
		*/
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
