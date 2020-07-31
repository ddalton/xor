package tools.xor.providers.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.xor.AggregateAction;
import tools.xor.Settings;
import tools.xor.util.ClassUtil;
import tools.xor.view.AbstractQuery;
import tools.xor.view.BindParameter;
import tools.xor.view.NativeQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryStringHelper;
import tools.xor.view.View;

public class JDBCQuery extends AbstractQuery
{
	private Connection connection;
	private PreparedStatement preparedStatement;
	private NativeQuery nativeQuery;
	private Map<String, Object> paramValues = new HashMap<>();

	public JDBCQuery(String sql, Connection connection, NativeQuery nativeQuery) {
		super(sql);
		this.connection = connection;
		this.nativeQuery = nativeQuery;

		if(isNativeQuery()) {
			initParamMap();
		} else {
			setQueryString(extractParameters());
		}

		createPreparedStatement();
	}

	private void createPreparedStatement() {
		try {
			if(connection != null) {
				if(!Query.isDeferred(getQueryString())) {
					this.preparedStatement = connection.prepareStatement(getQueryString());
				}
			} else {
				throw new RuntimeException("Need a JDBC connection");
			}
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	public void setProviderQuery() {
		createPreparedStatement();
	}

	private boolean isNativeQuery() {
		return this.nativeQuery != null;
	}

	private void initParamMap() {
		QueryStringHelper.initPositionalParamMap(positionByName, nativeQuery.getParameterList());
	}

	@Override
	public void updateParamMap (List<BindParameter> relevantParams) {
		QueryStringHelper.initPositionalParamMap(positionByName, relevantParams);
	}

	@Override public boolean isOQL ()
	{
		return false;
	}

	@Override public boolean isSQL ()
	{
		return true;
	}

	@Override
	public void setParameter(String name, Object value) {
		if(positionByName.containsKey(name)) {
			paramValues.put(name, value);
		}
	}

	@Override
	public boolean hasParameter(String name) {
		return positionByName.containsKey(name);
	}

	private int executeUpdate (Settings settings)
	{
		// Check if batching is enabled
		NativeQuery nq = settings.getView().getNativeQuery();

		int result = 0;
		JDBCBatchContext context = (JDBCBatchContext)settings.getSessionContext();
		if(context != null) {
			context.setQuery(this);
		}

		try {
				// We will try to auto discover the types of the columns being inserted
				if(nq.getParameterList() == null) {
					if (settings.getAction() == AggregateAction.CREATE) {
						nq.setParameterList(autoDiscoverInsert(connection, getQueryString()));
						initParamMap();
					} else {
						throw new RuntimeException("Bind variable types need to be specified");
					}
				}

			QueryStringHelper.setParameters(settings, preparedStatement, positionByName, paramValues);

			if (context != null) {
				if(context.isShouldBatch()) {
					preparedStatement.addBatch();
				} else {
					// last command in the batch
					preparedStatement.addBatch();
					// Return a negative value to signify the number of batch SQLs executed
					result = preparedStatement.executeBatch().length * -1;
					context.setQuery(null);
				}
			} else {
				result = preparedStatement.executeUpdate();
			}
		} catch (SQLException se) {
			throw ClassUtil.wrapRun(se);
		}

		return result;
	}

	@Override public Object execute (Settings settings)
	{
		if(settings.getAction() != AggregateAction.READ) {
			return executeUpdate(settings);

		} else {
			return getResultList(null, settings);
		}
	}
/*
	protected void setParameters (Settings settings,
								  PreparedStatement statement)
	{
		DBTranslator translator = DBTranslator.getTranslator(statement);
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
					// bind by position
					bindParam.setValue(statement, translator, value);
				}
			}
		}
	}
*/
	private List getResultSet (Settings settings)
	{
		List result = new ArrayList<>();

		try {
			QueryStringHelper.setParameters(settings, preparedStatement, positionByName, paramValues);
			ResultSet rs = preparedStatement.executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();
			int NumOfCol = rsmd.getColumnCount();

			List columnLabels = new ArrayList<>(NumOfCol);
			for(int i = 1; i <= NumOfCol; i++) {
				columnLabels.add(rsmd.getColumnLabel(i));
			}
			setColumns(columnLabels);

			while (rs.next()) {
				Object[] row = new Object[NumOfCol];
				for (int i = 1; i <= NumOfCol; i++) {
					row[i - 1] = rs.getObject(i);
				}
				result.add(row);
			}
			rs.close();
		} catch (SQLException se) {
			throw ClassUtil.wrapRun(se);
		}

		return result;
	}

	@Override protected List getResultListInternal (View view, Settings settings)
	{
		return getResultSet(settings);
	}

	@Override public Object getSingleResult (View view, Settings settings)
	{
		// Note: the first row is the column label
		List result = getResultSet(settings);
		if (result.size() == 0) {
			return null;
		}
		if (result.size() > 1) {
			throw new RuntimeException("Has more than 1 result");
		}
		else {
			return result.get(0);
		}
	}

	@Override
	public void setMaxResults(int limit) {
		try {
			this.preparedStatement.setMaxRows(limit);
			this.preparedStatement.setFetchSize(limit);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public void setFirstResult(int offset) {
		throw new RuntimeException("Offset not supported directly, modify your query to implement this.");
	}

	public boolean isDeferred() {
		return this.preparedStatement == null;
	}
	
	/* 
	 * JDBC query does not support ordinal parameter, so we return the legacy parameter placeholder representation
	 * (non-Javadoc)
	 * @see tools.xor.view.AbstractQuery#getOrdinalParameter(tools.xor.view.BindParameter)
	 */
    protected String getOrdinalParameter(BindParameter bindParam) {
        return "?";
    }	
}

