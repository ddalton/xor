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

import java.util.Collection;
import java.util.List;

public interface DAOTemplate<T> {
	/**
	 * Find the entity by its primary key
	 * @param id identifier
	 * @return Entity object
	 */
    T findById(Object id);    
    
    /**
     * Find an entity by the "name" property
     * 
     * @param name value
     * @return object
     */
	T findByName(String name); 
	
	/**
	 * Find a list of entities by its primary key
	 * @param ids list of identifiers
	 * @return list of entity objects
	 */
    List<T> findByIds(Collection<Object> ids);
    
    /**
     * Get a list of all the instances of an entity type
     * @return list of entities
     */
    List<T> findAll();
    
    /**
     * Save the entity in the database
     * @param entity to be saved
     * @return entity
     */
    T saveOrUpdate(T entity);
    
    /**
     * Delete the entity from the database
     * @param entity to delete
     */
    void delete(T entity);
    
    /**
     * refresh the state of the instance from the database
     * @param entity to refresh
     */
    public void refresh(T entity);
    
    /**
     * Clear all the managed entities from the cache, thus making them all detached
     */
    public void clear();
    
    /**
     * Set the java class of the entity
     * @param clazz java class
     */
    public void setPersistentClass(T clazz);
}