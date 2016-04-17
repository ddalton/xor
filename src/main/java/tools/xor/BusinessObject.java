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

package tools.xor;

import java.util.List;
import java.util.Map;

import tools.xor.util.ObjectCreator;

public interface BusinessObject extends DataObject {

	/**
	 * Return the ObjectCreator instance responsible for tracking the java object - DataObject instance mappings
	 * @return
	 */
	public ObjectCreator getObjectCreator();
	
	/**
	 * Get the actual object instance managed by this data object
	 */
	public Object getInstance();
	
	/**
	 * Get the actual object instance managed by this data object
	 * in the normalized form
	 */
	public Object getNormalizedInstance(Settings settings);	
	
	/**
	 * Set the reference to the actual java object
	 * @param instance
	 */
	public void setInstance(Object instance);
	
	/**
	 * Returns a persistence managed object wrapped in a data object
	 * @return
	 */	
	public BusinessObject load(Settings settings);	
	
	/**
	 * Return a DataObject initialized according to the level specified in the parameters
	 */
	public DataObject read(Settings settings);
	
	/**
	 * Perform an efficient read using a query. This requires the use of a view
	 */
	public List<?> query(Settings settings);

	/**
	 * Update a persistent object according to the level specified in the parameters
	 */
	public BusinessObject update(Settings settings);

	/**
	 * Create a data object from a data object and initialize according to the level specified in the
	 * parameters
	 * @see BusinessObject#read
	 */
	public DataObject create(Settings settings);

	/**
	 * Copy the full data object instance based on the settings in the typeMapper
	 * @throws Exception 
	 */
	public DataObject clone(Settings settings);
	
	/**
	 * Get the property object based on the property path
	 */
	public Property getPropertyByPath(String path);	

	/**
	 * Data structure used to track the containment relationship within an object graph
	 * Useful for finding which object controls the life of another object
	 * 
	 * @return
	 */
	public ObjectPersister getObjectPersister();	
	
	/**
	 * A flag useful for processing graphs (e.g., detecting cycles in the graph)
	 * @return the flag value
	 */
	public boolean isVisited();
	
	/**
	 * Set the flag value
	 * @param visited
	 */
	public void setVisited(boolean visited);

	/**
	 * The root entity of the aggregate
	 * @return
	 */
	public boolean isRoot();

	/**
	 * Flag to indicate if the object is managed by a 3rd party persistence layer
	 */
	public boolean isPersistent();

	/**
	 * Flag that represents if this object was created with data from a persistent store (e.g., a Hibernate/JPA managed object)
	 * @param persistent
	 */
	public void setPersistent(boolean persistent);	
	
	/**
	 * Gets the id for a persistent instance
	 */
	public Object getIdentifierValue();

	/** 
	 * Records this entity using the id and entity type
	 * @param entity
	 */
	public void addEntity(BusinessObject entity);

	/**
	 * Retrieves an entity by its id and entity type
	 * @param entity
	 * @return
	 */
	public BusinessObject getEntity(BusinessObject entity);

	/**
	 * Clear the record of this entity, i.e., removes the knowledge of this entity from the framework 
	 * @param entity
	 */
	public void removeEntity(BusinessObject entity);
	
	/**
	 * Executes the post logic methods on this object, i.e., invokes the methods that have the PostLogic annotation 
	 */
	public void invokePostLogic(Settings settings);

	/**
	 * When used in a collection, what property is uniquely represented in a non-instance specific manner 
	 * This is set using an annotation on the domain model class. Used in path expressions.
	 * This is currently restricted to a single property.
	 */	
	public ExtendedProperty getCollectionKeyProperty();

	/**
	 * If this object represents a collection, then returns its collection of objects as a list of ExtendedDataObjects
	 * @return
	 */
	public List<BusinessObject> getList();

	/**
	 * Set the property of this object using the values from propertyResult. This method is mainly used from a query result.
	 * @param propertyPath
	 * @param propertyResult
	 * @throws Exception
	 */
	void set(String propertyPath, Map<String, Object> propertyResult) throws Exception;
	
	/**
	 * Responsible for creating a new data object whose lifecycle is linked with this object.
	 * 
	 * @param id
	 * @param instanceType
	 * @param property
	 * @return
	 * @throws Exception
	 * @see {@link BusinessObject#createDataObject(Object, ExtendedType)}
	 */
	public BusinessObject createDataObject(Object id, Type instanceType, Property property) throws Exception;	

	/**
	 * This method is used to create a new data object that has to be in the scope of the data object that creates it.
	 * But its lifecycle is not linked with this object.
	 *
	 * @param id
	 * @param instanceType
	 * @return
	 * @throws Exception
	 * @see {@link BusinessObject#createDataObject(Object, ExtendedType, Property)}
	 */
	public BusinessObject createDataObject(Object id, Type instanceType) throws Exception;

	/**
	 * Used to set the container for a data object. This method is useful when the data object was created before its container.
	 * @param value
	 */
	public void setContainer(DataObject value);

	/**
	 * Set the containment property 
	 * @param value
	 */
	public void setContainmentProperty(Property value);

	/**
	 * This is not an embedded or a collection data object, but represents an entity data object
	 * @return
	 */
	public boolean isEntity();

	/**
	 * If different types are being used for the VO and business logic classes then, the domain type refers to the 
	 * business logic class. This method is useful for logic that needs to deal with persistent data. Since persistent
	 * data works with domain types.
	 * 
	 * @return
	 */
	public boolean isDomainType();

	/**
	 * Get the domain type object
	 * @return
	 */
	public Type getDomainType();

	/**
	 * Get the external type object, i.e., the VO class 
	 * @return
	 */
	public Type getExternalType();

	/**
	 * Link all the reverse side (non-cascade side) of the bi-directional references
	 */
	public void linkBackPointer();
	
	/**
	 * Unlink all the reverse side of the bi-directional references
	 */
	public void unlinkBackPointer();

	/**
	 * Returns the collection element id. This method is invoked on the Collection Element data object
	 * @return
	 */
	public String getCollectionElementId();
	
	/**
	 * Invoked on the Collection data object
	 */
	public String getCollectionElementId(Object collectionElement);

	/**
	 * Flag to denote if the instance has had modifications
	 * @return
	 */
	public boolean isModified();

	/**
	 * Flag to denote that the instance has been modified
	 * @param modified
	 */
	public void setModified(boolean modified);

	/**
	 * Create a copy of the instance and populating
	 * only those fields that need to be initialized
	 * @return
	 */
	public Object createReferenceCopy();

	/**
	 * Gets an existing BusinessObject, if one is not existing
	 * it creates the BusinessObject wrapper around the object instance
	 * and returns it.
	 * 
	 * @param property
	 * @return
	 */
	public Object getDataObject(Property property);	
}
