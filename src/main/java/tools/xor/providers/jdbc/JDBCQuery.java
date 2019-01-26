package tools.xor.providers.jdbc;

import tools.xor.AggregateAction;
import tools.xor.Settings;
import tools.xor.util.ClassUtil;
import tools.xor.view.AbstractQuery;
import tools.xor.view.BindParameter;
import tools.xor.view.NativeQuery;
import tools.xor.view.QueryStringHelper;
import tools.xor.view.View;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDBCQuery extends AbstractQuery
{
	private String sqlQuery;
	private Connection connection;
	private PreparedStatement preparedStatement;
	private NativeQuery nativeQuery;
	private Map<String, BindParameter> paramMap = new HashMap<>();
	private Map<String, Object> paramValues = new HashMap<>();

	public JDBCQuery(String sql, Connection connection, NativeQuery nativeQuery) {
		this.sqlQuery = sql;
		this.connection = connection;
		this.nativeQuery = nativeQuery;

		try {
			this.preparedStatement = connection.prepareStatement(sqlQuery);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}

		initParamMap();
	}

	private void initParamMap() {
		QueryStringHelper.initParamMap(paramMap, nativeQuery.getParameterList());
	}

	@Override
	public void updateParamMap (List<BindParameter> relevantParams) {
		QueryStringHelper.initParamMap(paramMap, relevantParams);
	}

	@Override
	public void setParameter(String name, Object value) {
		if(paramMap.containsKey(name)) {
			paramValues.put(name, value);
		}
	}

	@Override
	public boolean hasParameter(String name) {
		return paramMap.containsKey(name);
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
						nq.setParameterList(autoDiscoverInsert(connection, sqlQuery));
						initParamMap();
					} else {
						throw new RuntimeException("Bind variable types need to be specified");
					}
				}

			setParameters(settings, paramMap, paramValues, preparedStatement);

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

	protected void setParameters (Settings settings,
								  Map<String, BindParameter> paramMap,
								  Map<String, Object> paramValues,
								  PreparedStatement statement)
	{
		if (paramMap != null) {
			for (Map.Entry<String, BindParameter> entry : paramMap.entrySet()) {
				if (!paramValues.containsKey(entry.getKey())) {
					throw new RuntimeException(
						"Unable to find param value with key: " + entry.getKey());
				}
				BindParameter pm = entry.getValue();

				int timestampType = BindParameter.getType(pm.type);
				if (timestampType == Types.TIMESTAMP
					|| timestampType == Types.TIMESTAMP_WITH_TIMEZONE) {
					pm.setDateFormat(settings.getDateFormat());
				}
				pm.setValue(statement, paramValues.get(entry.getKey()));
			}
		}
	}

	private List getResultSet (Settings settings)
	{
		List result = new ArrayList<>();

		try {
			setParameters(settings, paramMap, paramValues, preparedStatement);
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
			ClassUtil.wrapRun(se);
		}

		return result;
	}

	@Override public List getResultList (View view, Settings settings)
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
}

