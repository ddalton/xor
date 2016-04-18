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
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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

import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.view.AggregateView;
import tools.xor.view.JPAQuery;
import tools.xor.view.Query;
import tools.xor.view.StoredProcedure;
import tools.xor.view.StoredProcedureQuery;

public abstract class JPAPersistenceOrchestrator extends AbstractPersistenceOrchestrator {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
    private boolean supportsSP;
    
    protected abstract EntityManager getEntityManager();
    
    protected abstract EntityManagerFactory getEntityManagerFactory();
    
    public JPAPersistenceOrchestrator() {
    	
    }
    
    public JPAPersistenceOrchestrator(Object sessionContext, Object data) {
    	
    }    
	
	/**
	 * Save the entity in the persistence store
	 * 
	 * @param entity
	 * @return
	 */
	@Override
	public void saveOrUpdate(Object entity) {
		getEntityManager().persist(entity);		
	}
	
	@Override 
	public void clear() {
		getEntityManager().clear();
	}
	
	@Override 
	public void refresh(Object object) {
		getEntityManager().refresh(object);
	}		

	/**
	 * Delete the entity from the persistence store
	 * 
	 * @param entity
	 */
	@Override
	public void delete(Object entity) {
		getEntityManager().remove(entity);		
	}

	@Override
	public void flush() {
		getEntityManager().flush();
	}

	@Override
	public Object disableAutoFlush() {
		Object oldFlushMode = this.getEntityManager().getFlushMode();
		this.getEntityManager().setFlushMode(FlushModeType.COMMIT);
		
		return oldFlushMode;
	}

	@Override
	public void setFlushMode(Object flushMode) {
		this.getEntityManager().setFlushMode((FlushModeType) flushMode);
	}

	@Override
	public Object findById(Class<?> persistentClass, Object id) {
	   return getEntityManager().find(persistentClass, (Serializable) id);
	}
	
	@Override
	public Object findByProperty(Type type, Map<String, String> propertyValues) {
		CriteriaBuilder builder = getEntityManagerFactory().getCriteriaBuilder();
		CriteriaQuery<Object> crit = builder.createQuery();
		
		Class<?> persistentClass = type.getInstanceClass();
		Root from = crit.from(persistentClass);
		CriteriaQuery<Object> select = crit.select(from);

		Predicate[] predicates = new Predicate[propertyValues.size()];
		int index = 0;
		for( Map.Entry<String, String> entry: propertyValues.entrySet())
			predicates[index++] = builder.equal(from.get(entry.getKey()), entry.getValue());

		crit.where(builder.and(predicates));

		TypedQuery<Object> typedQuery = getEntityManager().createQuery(select);
		List<Object> resultList = typedQuery.getResultList();
		if (resultList != null && !resultList.isEmpty()) {
			return resultList.get(0);
		}
		return null;		
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
			result = new JPAQuery(getEntityManager().createQuery(queryString));
			break;

		case SQL:
			result = new JPAQuery(getEntityManager().createNativeQuery(queryString));
			break;
			
		case SP:
			createCallableStatement(sp);
			result = new StoredProcedureQuery(sp);
			break;

		default:
			throw new RuntimeException("Unsupported queryType: " + queryType.name());
		}

		return result;
	}	
	
	protected void createCallableStatement(StoredProcedure sp) {
		java.sql.Connection conn = null;
		try {
			getEntityManager().unwrap(java.sql.Connection.class);
		} catch(PersistenceException pe) {
			try {
				// try hibernate provider
				conn = getEntityManager().unwrap(org.hibernate.internal.SessionImpl.class).connection();
			} catch (Exception e) {
				throw new RuntimeException("Unable to obtain the JDBC connection");
			}
		}
		try {
			DatabaseMetaData dbmd = conn.getMetaData();
			if(!dbmd.supportsStoredProcedures()) {
				throw new UnsupportedOperationException("Stored procedures with JDBC escape syntax is not supported");
			}
			sp.setCallableStatement(conn.prepareCall(sp.jdbcCallString()));
		} catch (SQLException e) {
			logger.info("Unable to retrieve JDBC metadata: " + e.getMessage());
		} 
	}

	@Override
	public Object getCached(Class<?> persistentClass, Object id) {
		PersistenceUnitUtil unitUtil = getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
		Object result = getEntityManager().getReference(persistentClass, id);
		
		if(unitUtil.isLoaded(result)) {
			return result;
		}
		
		return null;
	}	
	
	@Override
	public void attach(BusinessObject bo, AggregateView view) {
		
		if( view.getStateGraph((EntityType) bo.getType()).supportsDynamicUpdate() ) {
			// reattaches the object to the session
			getEntityManager().lock(bo.getInstance(), LockModeType.NONE);
		} else {
			throw new UnsupportedOperationException("The entity type " + bo.getType().getName() 
					+ " does not support dynamic update for the view " + view.getName());
		}
	}	
	
	@Override
	public boolean supportsStoredProcedure() {
		try {
			Method m = EntityManager.class.getMethod("createStoredProcedureQuery", String.class);
			if(m != null)
				supportsSP = true;
		} catch (Exception e) {
			supportsSP = false;
		}

		return supportsSP;
	}
}
