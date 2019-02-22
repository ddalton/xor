package tools.xor.providers.jdbc;

import org.springframework.jdbc.datasource.DataSourceUtils;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.AbstractPersistenceOrchestrator;
import tools.xor.service.QueryCapability;
import tools.xor.util.ClassUtil;
import tools.xor.view.JPAQuery;
import tools.xor.view.NativeQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryTreeInvocation;
import tools.xor.view.StoredProcedure;
import tools.xor.view.StoredProcedureQuery;

import javax.sql.DataSource;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JDBCPersistenceOrchestrator
	extends AbstractPersistenceOrchestrator
{
	private DataSource dataSource;

	public DataSource getDataSource ()
	{
		return dataSource;
	}

	public void setDataSource (DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	public JDBCPersistenceOrchestrator() {

	}

	public JDBCPersistenceOrchestrator(Object sessionContext, Object data) {

	}

	@Override
	public void saveOrUpdate(Object entity) {

		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override protected void createStatement (StoredProcedure sp)
	{
		try {
			Connection connection = DataSourceUtils.getConnection(dataSource);
			if (sp.isImplicit()) {
				sp.setStatement(connection.createStatement());
			}
			else {
				sp.setStatement(connection.prepareCall(sp.jdbcCallString()));
			}
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override
	public void clear(Set<Object> ids) {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}
	
	@Override 
	public void refresh(Object object) {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override
	public void delete(Object entity) {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override
	public void flush() {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override
	public Object disableAutoFlush() {
		return null;
	}

	@Override
	public void setFlushMode(Object flushMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean isTransient(BusinessObject from) {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override
	public Object findById(Class<?> persistentClass, Object id) {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override public List<Object> findByIds (EntityType entityType, Collection ids)
	{
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}
	
	@Override
	public Object findByProperty(Type type, Map<String, Object> propertyValues) {

		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override public Object getCollection (Type type, Map<String, Object> collectionOwnerKey)
	{
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override
	public QueryCapability getQueryCapability() {
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}


	@Override
	public Object getCached(Class<?> persistentClass, Object id)
	{
		throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");
	}

	@Override public Query getQuery (String queryString, QueryType queryType, Object queryInput, Settings settings)
	{
		Query result = null;
		switch(queryType) {

		case SQL:
			Connection connection = null;
			if (!Query.isDeferred(queryString)) {
				connection = DataSourceUtils.getConnection(dataSource);
			}
			result = new JDBCQuery(queryString, connection, (NativeQuery) queryInput);
			break;

		case SP:
			createStatement((StoredProcedure) queryInput);
			result = new StoredProcedureQuery((StoredProcedure) queryInput);
			break;

		case OQL:
			throw new UnsupportedOperationException("This is not yet supported for the JDBC interface");

		default:
			throw new RuntimeException("Unsupported queryType: " + queryType.name());
		}

		return result;
	}

	@Override
	public void evaluateDeferred(Query query, QueryType queryType, QueryTreeInvocation qti) {
		if(query instanceof JDBCQuery && Query.isDeferred(query.getQueryString())) {
			String queryString = qti.getResolvedQuery(query);
			if (queryType == QueryType.SQL) {
				((JDBCQuery)query).setProviderQuery(queryString, DataSourceUtils.getConnection(dataSource));
			}
		}
	}

	@Override
	protected void performAttach(BusinessObject input, Object instance) {
		throw new UnsupportedOperationException("The entity type " + input.getType().getName()
			+ " does not support dynamic update");
	}
	
	@Override
	public boolean supportsStoredProcedure() {
		return true;
	}

	@Override public Blob createBlob ()
	{
		try {
			return DataSourceUtils.getConnection(dataSource).createBlob();
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}
}
