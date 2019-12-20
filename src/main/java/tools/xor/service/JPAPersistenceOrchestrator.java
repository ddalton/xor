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

package tools.xor.service;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.providers.jdbc.DBTranslator;
import tools.xor.util.ClassUtil;
import tools.xor.view.JPAQuery;
import tools.xor.view.NativeQuery;
import tools.xor.view.Query;
import tools.xor.view.QueryJoinAction;
import tools.xor.view.QueryTreeInvocation;
import tools.xor.view.StoredProcedure;
import tools.xor.view.StoredProcedureQuery;

public abstract class JPAPersistenceOrchestrator extends AbstractPersistenceOrchestrator
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private boolean supportsSP;
	private ConnectionUtil connectionUtil;

	protected abstract EntityManager getEntityManager ();

	protected abstract EntityManagerFactory getEntityManagerFactory ();

	public JPAPersistenceOrchestrator ()
	{
		this.connectionUtil = new ConnectionUtil()
		{
			@Override public Connection getConnection ()
			{
				throw new UnsupportedOperationException("Get connection is not supported");
			}

			@Override public void execute (Object context)
			{
				if (context instanceof Work) {
					Work work = (Work)context;
					Session session = getEntityManager().unwrap(Session.class);
					session.doWork(work);
				}
			}
		};
	}

	public JPAPersistenceOrchestrator (Object sessionContext, Object data)
	{
		this();
	}

	@Override
	public void saveOrUpdate (Object entity)
	{
		getEntityManager().persist(entity);
	}

	@Override
	public void clear ()
	{
		getEntityManager().clear();
	}

	@Override
	public void clear (Set<Object> bos)
	{
		for (Object bo : bos) {
			getEntityManager().detach(ClassUtil.getInstance(bo));
		}
	}

	@Override
	public void refresh (Object object)
	{
		getEntityManager().refresh(object);
	}

	@Override
	public void delete (Object entity)
	{
		getEntityManager().remove(entity);
	}

	@Override
	public void flush ()
	{
		getEntityManager().flush();
	}

	@Override
	public Object disableAutoFlush ()
	{
		Object oldFlushMode = this.getEntityManager().getFlushMode();
		this.getEntityManager().setFlushMode(FlushModeType.COMMIT);

		return oldFlushMode;
	}

	@Override
	public void setFlushMode (Object flushMode)
	{
		this.getEntityManager().setFlushMode((FlushModeType)flushMode);
	}

	@Override
	public Object findById (Class<?> persistentClass, Object id)
	{
		return getEntityManager().find(persistentClass, (Serializable)id);
	}

	@Override
	public Object findById (Type type, Object id)
	{
		return findById(type.getInstanceClass(), id);
	}

	@Override
	public List<Object> findByIds (EntityType entityType, final Collection ids)
	{
		javax.persistence.Query query = getEntityManager().createQuery(
			"SELECT e FROM " + entityType.getName() + " e WHERE e.id in :ids");
		query.setParameter("ids", ids);
		return query.getResultList();
	}

	private List<Object> getResult (Type type, Map<String, Object> propertyValues)
	{
		CriteriaBuilder builder = getEntityManagerFactory().getCriteriaBuilder();
		CriteriaQuery<Object> crit = builder.createQuery();

		Class<?> persistentClass = type.getInstanceClass();
		Root from = crit.from(persistentClass);
		CriteriaQuery<Object> select = crit.select(from);

		Predicate[] predicates = new Predicate[propertyValues.size()];
		int index = 0;
		for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
			predicates[index++] = builder.equal(from.get(entry.getKey()), entry.getValue());
		}

		crit.where(builder.and(predicates));

		TypedQuery<Object> typedQuery = getEntityManager().createQuery(select);
		return typedQuery.getResultList();
	}

	@Override
	public Object findByProperty (Type type, Map<String, Object> propertyValues)
	{

		List<Object> resultList = getResult(type, propertyValues);
		if (resultList != null && !resultList.isEmpty()) {
			return resultList.get(0);
		}
		return null;
	}

	@Override
	public Object getCollection (Type type, Map<String, Object> propertyValues)
	{

		List<Object> resultList = getResult(type, propertyValues);
		Set<Object> result = new HashSet<Object>(resultList);
		return result;
	}

	@Override
	public QueryCapability getQueryCapability ()
	{
		return new JPAQueryCapability();
	}

	@Override
	public Query getQuery (String queryString, QueryType queryType, Object queryInput)
	{

		Query result = null;
		switch (queryType) {
		case OQL:
			result = new JPAQuery(
				queryString,
				Query.isDeferred(queryString) ? null : getEntityManager().createQuery(queryString));
			break;

		case SQL:
			result = new JPAQuery(
				queryString,
				Query.isDeferred(queryString) ?
					null :
					getEntityManager().createNativeQuery(queryString),
				(NativeQuery)queryInput);
			break;

		case SP:
			createStatement((StoredProcedure)queryInput);
			result = new StoredProcedureQuery((StoredProcedure)queryInput);
			break;

		case SP_MULTI:
			// Only the root has type SP, the dependent ones do not create a statement
			result = new StoredProcedureQuery(null);
			break;

		default:
			throw new RuntimeException("Unsupported queryType: " + queryType.name());
		}

		return result;
	}

	@Override
	public void evaluateDeferred (Query query, QueryType queryType, QueryTreeInvocation qti)
	{
		if (query instanceof JPAQuery && query.isDeferred()) {
			String queryString = query.getQueryString();
			if (queryType == QueryType.OQL) {
				((JPAQuery)query).setProviderQuery(
					getEntityManager().createQuery(queryString));
			}
			else if (queryType == QueryType.SQL) {
				((JPAQuery)query).setProviderQuery(
					getEntityManager().createNativeQuery(queryString));
			}

			super.evaluateDeferred(query, queryType, qti);
		}
	}

	protected void createStatement (final StoredProcedure sp)
	{
		try {
			this.connectionUtil.execute(
				new Work()
				{
					@Override
					public void execute (Connection connection) throws SQLException
					{
						// do whatever you need to do with the connection
						// test
						try {
							DatabaseMetaData dbmd = connection.getMetaData();
							if (!dbmd.supportsStoredProcedures()) {
								throw new UnsupportedOperationException(
									"Stored procedures with JDBC escape syntax is not supported");
							}

							if (sp.isImplicit()) {
								sp.setStatement(connection.createStatement());
							}
							else {
								sp.setStatement(connection.prepareCall(sp.jdbcCallString()));
							}
						}
						catch (SQLException e) {
							logger.info("Unable to retrieve JDBC metadata: " + e.getMessage());
						}
					}
				});
		}
		catch (PersistenceException pe) {
			throw new RuntimeException("Unable to obtain the JDBC connection");
		}
	}

	@Override
	public Blob createBlob ()
	{
		HibernatePersistenceOrchestrator.BlobCreator blobCreator = new HibernatePersistenceOrchestrator.BlobCreator();
		this.connectionUtil.execute(blobCreator);

		return blobCreator.getBlob();
	}

	@Override
	public Object getCached (Class<?> persistentClass, Object id)
	{
		PersistenceUnitUtil unitUtil = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
		Object result = getEntityManager().getReference(persistentClass, id);

		if (unitUtil.isLoaded(result)) {
			return result;
		}

		return null;
	}

	@Override
	protected void performAttach (BusinessObject input, Object instance)
	{
		// reattaches the object to the session
		getEntityManager().lock(instance, LockModeType.NONE);
	}

	@Override
	public boolean supportsStoredProcedure ()
	{
		try {
			Method m = EntityManager.class.getMethod("createStoredProcedureQuery", String.class);
			if (m != null)
				supportsSP = true;
		}
		catch (Exception e) {
			supportsSP = false;
		}

		return supportsSP;
	}

	@Override
	public void populateQueryJoinTable (String invocationId, Set ids)
	{
		this.connectionUtil.execute(
			new Work()
			{
				@Override
				public void execute (Connection connection) throws SQLException
				{
					saveQueryJoinTable(connection, invocationId, ids);
				}
			});
	}

	@Override
	public void createQueryJoinTable(final Integer stringKeyLen) {
		this.connectionUtil.execute(
			new Work()
			{
				@Override
				public void execute (Connection connection) throws SQLException
				{
					DBTranslator translator = DBTranslator.getTranslator(connection);
					if(translator.tableExists(connection, QueryJoinAction.JOIN_TABLE_NAME)) {
						return;
					}
					String sql = translator.getCreateQueryJoinTableSQL(stringKeyLen);

					try {
						Statement statement = connection.createStatement();
						statement.executeUpdate(sql);
					}
					catch (SQLException e) {
						throw ClassUtil.wrapRun(e);
					}
				}
			});
	}
}
