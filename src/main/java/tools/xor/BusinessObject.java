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
import tools.xor.util.graph.ObjectGraph;
import tools.xor.view.QueryTree;
import tools.xor.view.QueryTreeInvocation;

public interface BusinessObject extends DataObject, ObjectGraph.StateComparator.TypedObject
{

	/**
	 * Return the ObjectCreator instance responsible for tracking the java object - DataObject instance mappings
	 * @return ObjectCreator for this business object
	 */
	public ObjectCreator getObjectCreator();
	
	/**
	 * Get the actual object instance managed by this data object
	 * @return the object representing the instance
	 */
	public Object getInstance();
	
	/**
	 * Gets the polymorphic instance class name, if possible
	 * else fallback to the type class name
	 * 
	 * @return the Java classname of the instance
	 */
	public String getInstanceClassName();	
	
	/**
	 * Get the actual object instance managed by this data object
	 * in the normalized form
	 * @param settings The user entered settings
	 * @return normalized object
	 */
	public Object getNormalizedInstance(Settings settings);	
	
	/**
	 * Set the reference to the actual java object
	 * @param instance The instance that needs to be wrapped in this BusinessObject
	 */
	public void setInstance(Object instance);
	
	/**
	 * Returns a persistence managed object wrapped in a data object
	 * @param settings The user entered settings
	 * @return persistence managed DataObject
	 */	
	public BusinessObject load(Settings settings);	
	
	/**
	 * Return a DataObject initialized according to the level specified in the parameters
	 * @param settings The user entered settings
	 * @return an external model (non-persistence managed) DataObject
	 */
	public DataObject read(Settings settings);

	/**
	 * Convert from Domain model to External model without involving the database
	 * @param settings The user entered settings
	 * @return an external model (non-persistence managed) DataObject
	 */
	public DataObject toExternal(Settings settings);
	
	/**
	 * Perform an efficient read using a query. This requires the use of a view
	 * @param settings The user entered settings
	 * @return list of BusinesObject instances. NOTE: these objects are not persistence managed
	 */
	public List<?> query(Settings settings);

	/**
	 * Update a persistent object according to the level specified in the parameters
	 * @param settings The user entered settings
	 * @return a persistence managed DataObject
	 */
	public BusinessObject update(Settings settings);

	/**
	 * Create a data object from a data object and initialize according to the level specified in the
	 * parameters
	 * @see BusinessObject#read
	 * @param settings The user entered settings
	 * @return persistence managed DataObject
	 */
	public DataObject create(Settings settings);

	/**
	 * Convert from external model to domain model without involving the database
	 * @param settings The user entered Settings
	 * @return persistence managed DataObject
	 */
	public DataObject toDomain(Settings settings);

	/**
	 * Delete the object and any associated objects based on the view scope
	 * @param settings The user entered settings
	 */
	public void delete(Settings settings);

	/**
	 * Copy the full data object instance based on the settings in the typeMapper
	 * @param settings The user entered settings
	 * @return persistence managed DataObject copy
	 */
	public DataObject clone(Settings settings);
	
	/**
	 * Get the property object based on the property path
	 * @param path The property represented as path, this can be many levels deep
	 * @return property object representing the last path element
	 */
	public Property getPropertyByPath(String path);	

	/**
	 * Data structure used to track the containment relationship within an object graph
	 * Useful for finding which object controls the life of another object
	 * 
	 * @return ObjectPersister object
	 */
	public ObjectPersister getObjectPersister();	
	
	/**
	 * A flag useful for processing graphs (e.g., detecting cycles in the graph)
	 * @return true if the BusinessObject has been visited
	 */
	public boolean isVisited();
	
	/**
	 * Set the flag value
	 * @param visited boolean value
	 */
	public void setVisited(boolean visited);

	/**
	 * The root entity of the aggregate
	 * @return true if root
	 */
	public boolean isRoot();

	/**
	 * Flag to indicate if the object is managed by a 3rd party persistence layer
	 * @return true if persistence managed object
	 */
	public boolean isPersistent();

	/**
	 * Flag that represents if this object was created with data from a persistent store (e.g., a Hibernate/JPA managed object)
	 * @param persistent boolean value
	 */
	public void setPersistent(boolean persistent);	
	
	/**
	 * Gets the id for a persistent instance
	 * @return id value
	 */
	public Object getIdentifierValue();

	/**
	 * Gets the version for a persistent instance. This is needed for optimistic concurrency control.
	 * @return version value
	 */
	public Object getVersionValue();
	
	/**
	 * Gets the open property value. Applicable only for dynamic objects such as JSONObject
	 * @param propertyName The name of the open property
	 * @return value of open property
	 */
	public String getOpenProperty(String propertyName);

	/**
	 * Returns the SurrogateEntityKey instance for this entity
	 * @return the surrogate entity key
	 */
	public EntityKey getSurrogateKey ();

	/**
	 * Returns the SurrogateEntityKey instance for this entity
	 * @param anchor state tree path that determines the shape we are interested in
	 * @return the surrogate entity key
	 */
	public EntityKey getSurrogateKey (String anchor);

	/**
	 * Returns the NaturalEntityKey instance for this entity
	 * @return the natural entity key
	 */
	public List<EntityKey> getNaturalKey ();

	/**
	 * Returns the NaturalEntityKey instance for this entity
	 * @param anchor state tree path that determines the shape we are interested in
	 * @return the natural entity key
	 */
	public List<EntityKey> getNaturalKey (String anchor);

	/**
	 * Retrieves an entity by its id and entity type. This can be useful to find if there is a different
	 * BusinessObject instance recorded in the ObjectCreator of this BusinessObject.
	 * @param entity BusinessObject whose id and type are utilized in this API.
	 * @param anchor state tree path that determines the shape we are interested in
	 * @return BusinessObject
	 */
	public BusinessObject getEntity(BusinessObject entity, String anchor);

	/**
	 * Clear the record of this entity, i.e., removes the knowledge of this entity from the framework 
	 * @param entity BusinessObject whose id and type are utilized in this API.
	 * @param anchor state tree path that determines the shape we are interested in
	 */
	public void removeEntity(BusinessObject entity, String anchor);
	
	/**
	 * Executes the post logic methods on this object, i.e., invokes the methods that have the PostLogic annotation
	 * @param settings The user entered settings
	 */
	public void invokePostLogic(Settings settings);

	/**
	 * If this object represents a collection, then returns its collection of objects as a list of ExtendedDataObjects
	 * @return list of instances wrapped as BusinessObjects
	 */
	public List<BusinessObject> getList();

	/**
	 * If this object represents a collection, then returns its collection of objects as a list of ExtendedDataObjects
	 * @param settings needed to resolve root entity type when bulk processing
	 * @return list of instances wrapped as BusinessObjects
	 */
	public List<BusinessObject> getBulkList(Settings settings);

	/**
	 * Set the property of this object using the values from propertyResult. This method is mainly used from a query result.
	 * @param fullPropertyPath The path representing property whose value needs to be set
	 * @param propertyResult The result from a query
	 * @param queryTree for the query
	 * @param visitor for helping with adding elements to their collection
	 * @param queryInvocation holds intermediate results from the parent queries to help with stitching
	 * @throws Exception if property cannot be found
	 */
	void reconstitute (String fullPropertyPath,
					   Map<String, Object> propertyResult,
					   QueryTree queryTree,
					   ReconstituteRecordVisitor visitor,
					   QueryTreeInvocation queryInvocation) throws Exception;
	
	/**
	 * Responsible for creating a new data object whose lifecycle is linked with this object.
	 * 
	 * @param id identifier
	 * @param instanceType Type of the object to be created
	 * @param property The property where the created object will be placed
	 * @param anchor the path at which the object needs to be created. The scope for the same object can be
	 *               different on different paths.
	 * @return new BusinessObject 
	 * @throws Exception when creating an instance
	 */
	public BusinessObject createDataObject(Object id, Type instanceType, Property property, String anchor) throws Exception;
	
	/**
	 * Create a new Data object given a Surrogate key and a natural key
	 * 
	 * @param id surrogate key
	 * @param naturalKeyValues natural key
	 * @param instanceType Type of the object to be created
	 * @param property The property where the created object will be set
	 * @return new BusinessObject
	 * @throws Exception when creating the BusinessObject
	 */
	public BusinessObject createDataObject(Object id, Map<String, Object> naturalKeyValues, Type instanceType, Property property) throws Exception;

	/**
	 * Create a new Data object given a Surrogate key and a natural key
	 *
	 * @param id surrogate key
	 * @param naturalKeyValues natural key
	 * @param instanceType Type of the object to be created
	 * @param property The property where the created object will be set
	 * @param anchor the path at which the object needs to be created. The scope for the same object can be
	 *               different on different paths.
	 * @return new BusinessObject
	 * @throws Exception when creating the BusinessObject
	 */
	public BusinessObject createDataObject(Object id, Map<String, Object> naturalKeyValues, Type instanceType, Property property, String anchor) throws Exception;

	/**
	 * This method is used to create a new data object that has to be in the scope of the data object that creates it.
	 * But its lifecycle is not linked with this object.
	 *
	 * @param id identifier
	 * @param instanceType Type of the object to be created
	 * @return new BusinessObject 
	 * @throws Exception when creating an instance
	 */
	public BusinessObject createDataObject(Object id, Type instanceType) throws Exception;

	/**
	 * This method is used to create a new data object that has to be in the scope of the data object that creates it.
	 * But its lifecycle is not linked with this object.
	 *
	 * @param id identifier
	 * @param instanceType Type of the object to be created
	 * @param anchor path at which this object is anchored. Two objects with the same id are considered different
	 *               if they are anchored on different paths
	 * @return new BusinessObject
	 * @throws Exception when creating an instance
	 */
	public BusinessObject createDataObject(Object id, Type instanceType, String anchor) throws Exception;

	/**
	 * Used to set the container for a data object. This method is useful when the data object was created before its container.
	 * @param value the parent object
	 */
	public void setContainer(DataObject value);

	/**
	 * Set the containment property 
	 * @param value the parent property referring to this DataObject
	 */
	public void setContainmentProperty(Property value);

	/**
	 * This is not an embedded or a collection data object, but represents an entity data object
	 * @return true if an entity
	 */
	public boolean isEntity();

	/**
	 * If different types are being used for the VO and business logic classes then, the domain type refers to the 
	 * business logic class. This method is useful for logic that needs to deal with persistent data. Since persistent
	 * data works with domain types.
	 * 
	 * @return true if it is a domain type
	 */
	public boolean isDomainType();

	/**
	 * Get the domain type object
	 * @return the domain type of this object
	 */
	public Type getDomainType();

	/**
	 * Link all the reverse side (non-cascade side) of the bi-directional references
	 */
	public void linkBackPointer();
	
	/**
	 * Unlink all the reverse side of the bi-directional references
	 */
	public void unlinkBackPointer();

	/**
	 * Returns the collection element key value. This method is invoked on the Collection Element data object
	 * @param property from which collections specific metadata is obtained
	 * @return value of the element key
	 */
	public Object getCollectionElementKey(Property property);

	/**
	 * Flag to denote if the instance has had modifications
	 * @return true if modified
	 */
	public boolean isModified();

	/**
	 * Flag to denote that the instance has been modified
	 * @param modified boolean value
	 */
	public void setModified(boolean modified);

	/**
	 * Create a copy of the instance and populating
	 * only those fields that need to be initialized
	 * @return a reference copy of the instance
	 */
	public Object createReferenceCopy();

	/**
	 * Gets an existing BusinessObject, if one is not existing
	 * it creates the BusinessObject wrapper around the object instance
	 * and returns it.
	 * 
	 * @param property holding the object
	 * @return DataObject
	 */
	public Object getDataObject(Property property);	
	
	/**
	 * A node is considered a dependent if it has incoming edges and one of them has the cascade flag set to true
	 * @return true if it is owned by
	 */
	public boolean isDependent();	
	
	/**
	 * This method takes care of creating the containment graph and also any needed DataObject wrappers for the instance objects
	 * This is especially important if the object is referenced more than once and we don't want to create a copy for each reference
	 * It also demarcates the spanning tree of the graph based on containment relationships
	 */		
	public void createAggregate();

	/**
	 * Specifying the view improves the performance of this method as only the fields in the view need to be processed.
	 * @param settings the settings object from which the view is obtained
	 */
	public void createAggregate(Settings settings);
	
	/**
	 * Get a business object of the same type as the current business object but with different id
	 * @param id surrogate key value
	 * @param type domain type irrespective of whether we are obtaining an external or a domain object
	 * @return BusinessObject if found, null otherwise
	 */
	public BusinessObject getBySurrogateKey(Object id, Type type);

	/**
	 * Get a business object of the same type as the current business object but with different id
	 * @param id surrogate key value
	 * @param type domain type irrespective of whether we are obtaining an external or a domain object
	 * @param path at which the object is anchored from the root object
	 * @return BusinessObject if found, null otherwise
	 */
	public BusinessObject getBySurrogateKey(Object id, Type type, String path);
	
	/**
	 * Get a business object of the same type as the current business object but with different natural key
	 * @param naturalKeyValues naturalkey values
	 * @param type  domain type irrespective of whether we are obtaining an external or a domain object
	 * @return BusinessObject if found, null otherwise
	 */
	public BusinessObject getByNaturalKey(Map<String, Object> naturalKeyValues, Type type);

	/**
	 * Retrieve a business object of the same type with a different natural key and path
	 * @param naturalKeyValues natural key
	 * @param type domain type
	 * @param path at which the object is anchored from the root object
	 * @return BusinessObject if found, null otherwise
	 */
	public BusinessObject getByNaturalKey(Map<String, Object> naturalKeyValues, Type type, String path);

	/**
	 * Get the object from the object graph for open property
	 * @param name of the property
	 * @return BusinessObject value of the open property
	 */
	public BusinessObject getOpenPropertyValue(String name);

	/**
	 * Gets the settings instance under whose scope this BusinessObject was created.
	 * @return settings instance
	 */
	public Settings getSettings();

	/**
	 * Represents the type of the property referencing this object
	 * @return type
	 */
	public Type getPropertyType();

	/**
	 * Check if the object is a reference association object and set the
	 * XOR.keyref flag accordingly
	 */
	public void examine();

	/**
	 * Checks if the instance is a reference association object
	 * @return true if this is a reference association object
	 */
	public boolean isReference ();

	/**
	 * Change the type of the object by downcasting the type.
	 * @param type subtype of the current type
	 */
	public void downcast(EntityType type);
}
