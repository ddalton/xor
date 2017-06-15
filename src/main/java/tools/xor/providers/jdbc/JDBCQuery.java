package tools.xor.providers.jdbc;

import org.apache.commons.lang.StringUtils;
import tools.xor.AggregateAction;
import tools.xor.Settings;
import tools.xor.util.ClassUtil;
import tools.xor.view.AbstractQuery;
import tools.xor.view.BindParameter;
import tools.xor.view.NativeQuery;
import tools.xor.view.View;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JDBCQuery extends AbstractQuery
{
	private String sqlQuery;
	private Connection connection;
	private PreparedStatement preparedStatement;
	private Map<Integer, Object> positionalParameters;
	private NativeQuery nativeQuery;
	private Map<Integer, BindParameter> paramMap = new HashMap<Integer, BindParameter>();

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
		int position = 1;
		if (nativeQuery.getParameterList() != null) {
			for (BindParameter param : nativeQuery.getParameterList()) {
				param.position = position;
				paramMap.put(position++, param);
			}
		}
	}


	@Override
	public void setParameter(String name, Object value) {
		boolean isPositional = false;
		if (StringUtils.isNumeric(name)) {
			isPositional = true;
		}

		if (isPositional) {
			if (positionalParameters == null) {
				// Sort the parameters by its position
				positionalParameters = new TreeMap<>();
			}

			int position = Integer.valueOf(name);
			positionalParameters.put(position, value);

		}
		else {
			throw new RuntimeException("Named parameters not supported for JDBC");
		}
	}

	public void reset() {
		if(positionalParameters != null) {
			positionalParameters.clear();
		}
	}

	@Override
	public boolean hasParameter(String name) {
		throw new RuntimeException("Named parameters not supported for JDBC");
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

			setPositionalParameters(settings, positionalParameters, paramMap, preparedStatement);

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


	private List getResultSet (Settings settings)
	{
		List result = new ArrayList<>();

		try {
			setPositionalParameters(settings, positionalParameters, paramMap, preparedStatement);
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

