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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;

import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.view.AggregateView;
import tools.xor.view.HibernateQuery;
import tools.xor.view.Query;
import tools.xor.view.StoredProcedure;
import tools.xor.view.StoredProcedureQuery;

public abstract class HibernatePersistenceOrchestrator extends AbstractPersistenceOrchestrator {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final String SQL_LOGGER = "org.hibernate.SQL";
	
	public HibernatePersistenceOrchestrator() {
		
	}
	
	/**
	 * We don't make use of sessionContext currently since we can
	 * get the session from sessionFactory.getCurrentSession()
	 * 
	 * @param sessionContext session context
	 * @param data any additional data that needs to be passed by the user
	 */
	public HibernatePersistenceOrchestrator(Object sessionContext, Object data) {
		if (ApplicationConfiguration.config().containsKey(Constants.Config.SQL_STACK)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.SQL_STACK)) {
			Logger logger = Logger.getLogger(SQL_LOGGER);
			Enumeration<RollingFileAppender> e = logger.getAllAppenders();
			while (e.hasMoreElements()) {
				RollingFileAppender appender = e.nextElement();
				Layout original = appender.getLayout();
				if (SQLLogLayout.class.isAssignableFrom(original.getClass()))
					continue;

				appender.setLayout(new SQLLogLayout(original));
				appender.activateOptions();
			}
		}
	}	
	
	public abstract SessionFactory getSessionFactory();

	protected Session getSession() {
		if (this.getSessionFactory().getCurrentSession() == null)
			throw new IllegalStateException("Session has not been set on DAO before usage");
		return this.getSessionFactory().getCurrentSession();
	}    	
	
	public static class SQLLogLayout extends PatternLayout {
		public static final int MAX_STACKSIZE = 35;
		
		private Layout layout;
		
		public SQLLogLayout(Layout layout) {
			super();
			this.layout = layout;
		}
		
		@Override
		public String format(LoggingEvent event) {
			StringBuilder sb = new StringBuilder(layout.format(event));

			if(SQL_LOGGER.equals(event.getLoggerName())) {
				StackTraceElement[] elements = Thread.currentThread().getStackTrace();
				
				int i = 0;
				for (StackTraceElement element: elements ) {
					/*
					if(
						element.toString().startsWith("java") ||
						element.toString().startsWith("org")  ||
						element.toString().startsWith("sun")  ||
						element.getClassName().equals(SQLLogLayout.class.getName())
					) {
							continue;
					}*/
									
					sb.append(element.toString()).append("  " + sb.length() + "\r\n");
					if(i++ > MAX_STACKSIZE)
						break;
				}
				sb.append("\r\n");
			}
			return sb.toString();			
		}
	}

	@Override
	public void saveOrUpdate(Object entity) {
		//System.out.println("HibernatePersistenceOrchestrator#saveOrUpdate");
		getSession().saveOrUpdate(entity);		
	}
	
	@Override 
	public void clear() {
		getSession().clear();
	}	

	@Override 
	public void refresh(Object object) {
		getSession().refresh(object);
	}	

	@Override
	public void delete(Object entity) {
		getSession().delete(entity);		
	}

	@Override
	public void flush() {
		getSession().flush();
	}

	@Override
	public Object disableAutoFlush() {
		Object oldFlushMode = getSession().getFlushMode();
		getSession().setFlushMode(FlushMode.MANUAL);
		
		return oldFlushMode;
	}

	@Override
	public void setFlushMode(Object flushMode) {
		getSession().setFlushMode((FlushMode) flushMode);
	}

	@Override
	public Object findById(Class<?> persistentClass, Object id) {
		return getSession().get(persistentClass, (Serializable) id);
	}

	@Override
	public List<Object> findByIds(EntityType entityType, final Collection ids) {
		org.hibernate.Query query = getSession().createQuery(
			"SELECT e FROM " + entityType.getName() + " e WHERE e.id in :ids");
		query.setParameter("ids", ids);
		return query.list();
	}
	
	private List<Object> getResult(Type type, Map<String, Object> propertyValues) {
		Class<?> persistentClass = type.getInstanceClass();
		Criteria crit = getSession().createCriteria(persistentClass);

		for(Map.Entry<String, Object> entry: propertyValues.entrySet())
			crit.add(Restrictions.eq(entry.getKey(), entry.getValue()));
		
		return crit.list();
	}

	@Override
	public Object findByProperty(Type type, Map<String, Object> propertyValues) {

		List<Object> resultList = getResult(type, propertyValues);
		if (resultList != null && !resultList.isEmpty()) {
			return resultList.get(0);
		}
		return null;		
	}
	
	@Override
	public Object getCollection(Type type, Map<String, Object> propertyValues) {
		
		List<Object> resultList = getResult(type, propertyValues);
		Set<Object> result = new HashSet<Object>(resultList);
		return result;
	}	

	@Override
	public QueryCapability getQueryCapability() {
		return new JPAQueryCapability();
	}

	@Override
	public Query getQuery(String queryString, QueryType queryType, StoredProcedure sp) {		

		Query result = null;
		switch(queryType) {
		case OQL:
			result = new HibernateQuery(getSessionFactory().getCurrentSession().createQuery(queryString));
			break;

		case SQL:
			result = new HibernateQuery(getSessionFactory().getCurrentSession().createSQLQuery(queryString));
			break;
			
		case SP:
			createStatement(sp);
			result = new StoredProcedureQuery(sp);
			break;			

		default:
			throw new RuntimeException("Unsupported queryType: " + queryType.name());
		}

		return result;
	}
	
	@Override
	protected void createStatement (final StoredProcedure sp) {
		
		getSession().doWork(new Work() {
		    @Override
		    public void execute(Connection conn) throws SQLException {
				try {
					DatabaseMetaData dbmd = conn.getMetaData();
					if(!dbmd.supportsStoredProcedures()) {
						throw new UnsupportedOperationException("Stored procedures with JDBC escape syntax is not supported");
					}
					if(sp.isImplicit()) {
						sp.setStatement(conn.createStatement());
					} else {
						sp.setStatement(conn.prepareCall(sp.jdbcCallString()));
					}
				} catch (SQLException e) {
					logger.info("Unable to retrieve JDBC metadata: " + e.getMessage());
				} 
		    }
		});
	}	

	@Override
	public Object getCached(Class<?> persistentClass, Object id) {
		Object result = getSession().load(persistentClass, (Serializable) id);
		if(Hibernate.isInitialized(result)) {
			return result;
		}
		
		return null;
	}

	@Override
	public void attach(BusinessObject bo, AggregateView view) {
		
		if( view.getStateGraph((EntityType) bo.getType()).supportsDynamicUpdate() ) {
			// reattaches the object to the session
			getSession().buildLockRequest(LockOptions.NONE).lock(bo.getInstance());
		} else {
			throw new UnsupportedOperationException("The entity type " + bo.getType().getName() 
					+ " does not support dynamic update for the view " + view.getName());
		}
	}	
}
