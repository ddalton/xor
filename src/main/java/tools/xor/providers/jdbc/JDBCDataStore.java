package tools.xor.providers.jdbc;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import tools.xor.AbstractTypeMapper;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.NaturalEntityKey;
import tools.xor.Property;
import tools.xor.SurrogateEntityKey;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.service.AbstractDataStore;
import tools.xor.service.QueryCapability;
import tools.xor.util.ClassUtil;
import tools.xor.view.NativeQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryJoinAction;
import tools.xor.view.QueryTreeInvocation;
import tools.xor.view.StoredProcedure;
import tools.xor.view.StoredProcedureQuery;

public class JDBCDataStore
	extends AbstractDataStore
{
	private DataSource dataSource;
	private JDBCSessionContext context;

	public DataSource getDataSource ()
	{
		return dataSource;
	}

	public JDBCSessionContext getSessionContext() {
		return this.context;
	}

	public void setDataSource (DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	public JDBCDataStore() {
	}

	public JDBCDataStore(JDBCSessionContext context, Object data) {
		this.context = new JDBCSessionContext(this, context);
	}

	public Connection getNewConnection () {
		return DataSourceUtils.getConnection(dataSource);
	}

	/**
	 * No-op for JDBC.
	 * @see tools.xor.providers.jdbc.JDBCSessionContext#persistGraph(tools.xor.util.ObjectCreator, tools.xor.Settings)
	 * @param entity to save
	 */
	@Override
	public void saveOrUpdate(Object entity) {

	}

	@Override protected void createStatement (StoredProcedure sp)
	{
		try {
			Connection connection = context.getConnection();
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
		context.clear();
	}

	@Override
	public void clear(Set<Object> ids) {
		// no-op
	}
	
	@Override 
	public void refresh(Object object) {
		throw new UnsupportedOperationException("This is not supported for the JDBC interface");
	}

	/**
	 * No-op for JDBC.
	 * @see tools.xor.providers.jdbc.JDBCSessionContext#deleteGraph(tools.xor.util.ObjectCreator, tools.xor.Settings)
	 * @param entity to delete
	 */
	@Override
	public void delete(Object entity) {

	}

	@Override
	public void flush() {
		context.flush();
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
		// If the object cannot be found in the JDBCSessionContext, then it is considered transient
		if(getEntity(from) == null) {
			return true;
		}

		return false;
	}

	@Override
	public Object getPersistentObject(CallInfo callInfo, TypeMapper typeMapper) {
		// The persistent object is the original object if any
		// get it from the JDBCSessionContext

		BusinessObject from = (BusinessObject) callInfo.getInput();
		return getEntity(from);
	}

	public Object getEntity(BusinessObject bo) {
		Object persistentObject = null;

		if(!EntityType.class.isAssignableFrom(bo.getType().getClass())) {
			return null;
		}

		EntityType type = (EntityType) bo.getType();
		ExtendedProperty identifierProperty = (ExtendedProperty) type.getIdentifierProperty();
		if(identifierProperty != null) {
			Serializable id = (Serializable)identifierProperty.getValue(bo);
			if (id != null && !"".equals(id)) {
				persistentObject = findById(type, id);
			}
		} else {
			// Use the natural key
			Map<String, Object> naturalKey = new HashMap<>();
			if(type.getNaturalKey() != null) {
				for(String key: type.getExpandedNaturalKey()) {
					Property property = type.getProperty(key);
					Object value = ((ExtendedProperty)property).getValue(bo);
					naturalKey.put(property.getName(), value);
				}
				NaturalEntityKey nek = new NaturalEntityKey(naturalKey, type.getName());
				persistentObject = findById(type, nek);
			} else {
				throw new RuntimeException("Type " + type.getName() + " does not have a natural key");
			}
		}

		return persistentObject;
	}

	@Override
	public Object findById(Class<?> persistentClass, Object id) {
		throw new UnsupportedOperationException("Use findById with type argument instead.");
	}

	@Override
	public Object findById(Type type, Object id) {

		if(context.readFromDB()) {
			// TODO: get the object from the database
			return null;
		} else {
			// The object with the given id is obtained from the JDBCSessionContext
			EntityKey ek;
			if (!(id instanceof EntityKey)) {
				ek = new SurrogateEntityKey(id, AbstractTypeMapper.getSurrogateKeyTypeName(type));
			}
			else {
				ek = (EntityKey)id;
			}
			return context.getEntity(ek);
		}
	}

	@Override public List<Object> findByIds (EntityType entityType, Collection ids)
	{
		List<Object> result = new LinkedList<>();

		for(Object id: ids) {
			result.add(findById(entityType, id));
		}

		return result;
	}
	
	@Override
	public Object findByProperty(Type type, Map<String, Object> propertyValues) {
		// TODO
		throw new UnsupportedOperationException("This is not supported for the JDBC interface");
	}

	@Override public Object getCollection (Type type, Map<String, Object> collectionOwnerKey)
	{
		throw new UnsupportedOperationException("This is not supported for the JDBC interface");
	}

	@Override
	public QueryCapability getQueryCapability() {
		return null;
	}


	@Override
	public Object getCached(Class<?> persistentClass, Object id)
	{
		// Currently JDBC does not support caching
		return null;
	}

	@Override public Query getQuery (String queryString, QueryType queryType, Object queryInput)
	{
		Query result;
		switch(queryType) {

		case SQL:
			Connection connection = context.getConnection();
			result = new JDBCQuery(queryString, connection, (NativeQuery) queryInput);
			break;

		case SP:
			createStatement((StoredProcedure) queryInput);
			result = new StoredProcedureQuery((StoredProcedure) queryInput);
			break;

		case SP_MULTI:
			// Only the root has type SP, the dependent ones do not create a statement
			result = new StoredProcedureQuery(null);
			break;

		case OQL:
			connection = null;
			if (!Query.isDeferred(queryString)) {
				connection = context.getConnection();
			}
			result = new JDBCQuery(queryString, connection, null);
			break;

		default:
			throw new RuntimeException("Unsupported queryType: " + queryType.name());
		}

		return result;
	}

	@Override
	public void evaluateDeferred(Query query, QueryType queryType, QueryTreeInvocation qti) {
		if(query instanceof JDBCQuery && query.isDeferred()) {
			if (queryType == QueryType.SQL) {
				((JDBCQuery)query).setProviderQuery();
			}

			super.evaluateDeferred(query, queryType, qti);
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
			return context.getConnection().createBlob();
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public void initForQuery() {
		context.readOnlyTransaction();
	}

	@Override public void populateQueryJoinTable (String invocationId, Set ids)
	{
		try {
			saveQueryJoinTable(context.getConnection(), invocationId, ids);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public void createQueryJoinTable(Integer stringKeyLen) {

		DBTranslator translator = DBTranslator.getTranslator(context.getConnection());
		if(translator.tableExists(context.getConnection(), QueryJoinAction.JOIN_TABLE_NAME)) {
			return;
		}

		String sql = translator.getCreateQueryJoinTableSQL(stringKeyLen);

		try {
			Statement statement = context.getConnection().createStatement();
			statement.executeUpdate(sql);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}
}
