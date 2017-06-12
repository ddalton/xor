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

import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import tools.xor.BusinessObject;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.ClassUtil;
import tools.xor.view.AggregateView;
import tools.xor.view.Query;
import tools.xor.view.StoredProcedure;

public class DatastorePersistenceOrchestrator extends AbstractPersistenceOrchestrator {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
    private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    
    public DatastorePersistenceOrchestrator() {
    	
    }
    
    public DatastorePersistenceOrchestrator(Object sessionContext, Object data) {
    	
    }    

	@Override
	public void saveOrUpdate(Object entity) {
		datastore.put((Entity) entity);		
	}
	
	@Override 
	public void clear() {
		// This has no effect in the Datastore
	}
	
	@Override 
	public void refresh(Object object) {
		// This has no effect in the Datastore
	}		

	@Override
	public void delete(Object entity) {
		// TODO: Extract the key from the entity
		// and create the key array
		datastore.delete((Key[]) entity);		
	}

	@Override
	public void flush() {
		// This has no effect in the Datastore
	}

	@Override
	public Object disableAutoFlush() {
		// This has no effect in the Datastore
		return null;
	}

	@Override
	public void setFlushMode(Object flushMode) {
		// This has no effect in the Datastore
	}

	@Override
	public Object findById(Class<?> persistentClass, Object id) {
		try {
			return datastore.get((Key) id);
		} catch (EntityNotFoundException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public List<Object> findByIds(EntityType entityType, final Collection ids) {
		return new ArrayList(datastore.get(ids).values());
	}
	
	private List<Object> getResult(Type type, Map<String, Object> propertyValues) {
		com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(type.getName());
		
		List<Filter> filters = new LinkedList<Filter>();
		for( Map.Entry<String, Object> entry: propertyValues.entrySet()) {
			filters.add(new FilterPredicate(entry.getKey(),
					                      FilterOperator.EQUAL,
					                      entry.getValue()));			
		}
		q.setFilter(CompositeFilterOperator.and(filters));
		
		// Use PreparedQuery interface to retrieve results
		PreparedQuery pq = datastore.prepare(q);

		List<Object> result = new LinkedList<Object>();
		for (Entity element : pq.asIterable()) {		
			result.add(element);
		}
		
		return result;
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
	@Deprecated
	// TODO: Replace this method with one that works on the QueryBuilder. This effectively means
	// we can support persistence mechanisms that don't support String based Query construction
	// e.g., getQuery(QueryBuilder)
	public Query getQuery(String queryString, QueryType queryType, Object queryInput) {

		Query result = null;
		switch(queryType) {
		case OQL:
			// TODO: create the query
			break;

		case SQL:
		case SP:
			// fall through
		default:
			throw new RuntimeException("Unsupported queryType: " + queryType.name());
		}

		return result;
	}	
	
	protected void createStatement (StoredProcedure sp) {
		throw new UnsupportedOperationException("Stored procedure is not supported");
	}

	@Override
	public Object getCached(Class<?> persistentClass, Object id) {
		return null;
	}
	
	@Override
	public boolean supportsStoredProcedure() {
		return false;
	}

	@Override public Blob createBlob ()
	{
		throw new UnsupportedOperationException("Blob type is not supported");
	}
}
