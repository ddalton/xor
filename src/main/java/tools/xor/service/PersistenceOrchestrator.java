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

import java.util.Map;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.view.AggregateView;
import tools.xor.view.Query;
import tools.xor.view.StoredProcedure;

public interface PersistenceOrchestrator {
	
    public enum QueryType {
        OQL,
        SQL,
        SP
    }
	
	/**
	 * Find the entity by its primary key
	 * @param id
	 * @return
	 */
	public Object findById(Class<?> persistentClass, Object id);	
	
	/**
	 * Find the entity by one or more property values
	 * @param id
	 * @return
	 */
	public Object findByProperty(Type type, Map<String, String> propertyValues);		

    /**
     * Save the entity in the persistence store
     * @param entity
     * @return
     */
    public void saveOrUpdate(Object entity);
    
    /**
     * Delete the entity from the persistence store
     * @param entity
     */
    public void delete(Object entity);
    
    /**
     * Flush any buffered content to the persistence store
     */
    public void flush();
    
    /**
     * This disables auto-flush necessary by the persistence layer
     * to run queries with the latest data.
     * Since we manage the latest data and queries are only used
     * to reference objects we can afford to skip this as we manage the
     * references to newly created objects.
     * 
     * @return old flush mode
     * 
     */
	public Object disableAutoFlush();
	
	/**
	 * Set the flush mode
	 * @param flushMode
	 */
	public void setFlushMode(Object flushMode);
    
    /**
     * Clears any objects held in the persistence cache
     */
    public void clear();
    
    /**
     * Refreshes the object held in the persistence cache
     */
    public void refresh(Object object);    
    
    /**
     * Indicates if this persistence orchestrator automatically tracks version 
     * If not, the framework takes care of updating the version in order to support optimistic concurrency control
     */
    public boolean supportsVersionTracking();
    
    /**
     * Returns the mechanisms supported for querying
     * @return
     */
    public QueryCapability getQueryCapability();
    
    /**
     * Some ORMs cannot resolve all the issues while persisting an aggregate. 
     * For e.g., cycle dependencies within an aggregate is a problem while persisting it.
     * 
     * This is usually not a problem with NoSQL databases.
     * Setting this flag to false, allows the framework to individually persist
     * objects within the aggregate in based on topological ordering of the required fields.
     */
    public boolean canProcessAggregate();
    
	/**
	 * Check if there is an existing persistent instance with the identifier 
	 * specified in object
	 * 
	 * @param object
	 * @return A persistent object or null 
	 */
	public Object getPersistentObject(CallInfo callInfo, TypeMapper typeMapper);
	
	/**
	 * If the object is present in the local cache (session) return it 
	 * else return null
	 * 
	 * @param id
	 * @return
	 */
	public Object getCached(Class<?> persistentClass, Object id);
	
	/**
	 * Return the query object specific to the persistence mechanism
	 */
	public Query getQuery(String queryString, QueryType queryType, StoredProcedure sp);
	
	/**
	 * If the persistence orchestrator can take a user created object and transform it
	 * to a persistence managed object (i.e, track changes)
	 * This is in contrast to merge where only a managed object can be managed and
	 * not a user created object
	 * For this to work, the view needs to support dynamic update
	 * 
	 * @param bo The object wrapper being reattached
	 * @param view The view representing the scope of the update
	 * @return
	 */
	public void attach(BusinessObject bo, AggregateView view);
	
	/**
	 * Checks to see if stored procedure support 
	 * This includes OUT parameters support. TODO: relax this requirement in the future.
	 * @return
	 */
	public boolean supportsStoredProcedure();

}