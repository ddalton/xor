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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryTransformer;

/**
 * The DataModel represents the logical schema in the form of types and properties.
 * It is comprised of one or more shapes to represent either multi-tenants in a multi-tenant 
 * system or categorized by any other means.
 * 
 * @author Dilip Dalton
 *
 */
public interface DataModel {

	static final String DEFAULT_SHAPE = "_DEFAULT_";
	static final String RELATIONAL_SHAPE = "_RELATIONAL_"; // no relationships, useful for data generation and import

	/**
	 * Returns the shape in the current session/thread. So it needs to be overridden by
	 * subclasses.
	 * NOTE: Take particular care if this should be called during the bootstrap process as that will
	 * interfere in the order of Shape creation. Any overriding code should check if
	 * the default shape is being constructed and if so, return the default shape.
	 *
	 * @return Shape of types
	 */
	public Shape getShape();

	/**
	 * Return a specified shape
	 * @param name of the shape
	 * @return shape object
	 */
	public Shape getShape(String name);

	/**
	 * Gets the Shape that created the type present in the argument
	 * @param entityType entity Type
	 * @return Shape responsible for creating the entity type
	 */
	public Shape getOwner(EntityType entityType);
	
	/**
	 * Add the shape to the DAS, and additionally control if 
	 * the added shape can be make active.
	 * 
	 * @param shape object representing the type system
	 */
	public void addShape(Shape shape);
	
	/**
	 * Make the shape as the default shape for the DAS.
	 * This is useful so that the name of the shape does not have to be passed
	 * all over the code.
	 * 
	 * @param shape to be made active
	 */
	public void setActive(Shape shape);
	
	/**
     * It depends on the specific Data Model on how the default Shape needs to be constructed
     * if the name is not provided
     * 
     * @return default shape
     */
	public Shape createShape();
	
    /**
     * Add the shape to the DAS.
     * The shape that is added does not affect the active shape.
     *
     * @param name of the Shape representing the type system
     * @return shape that was added
     */
    public Shape createShape(String name);    

	/**
	 * Build the Type and Property objects for the static API
	 * Rebuilds an existing shape.
	 *
	 * @param name of the Shape representing the type system
	 * @param extension code to add new types/properties to the shape
	 * @return shape that was added
	 */
	public Shape createShape(String name, SchemaExtension extension);
	
    
    /**
     * Add the shape to the DAS.
     * The shape that is added does not affect the active shape.
     *
     * @param name of the Shape representing the type system
     * @param extension code to add new types/properties to the shape 
     * @param typeInheritance describes the strategy of how properties and shapes are shared
     * @return shape that was added
     */
    public Shape createShape(String name, SchemaExtension extension, Shape.Inheritance typeInheritance);	

	/**
	 * Remove an existing shape
	 * @param name of the shape
	 */
	public void removeShape(String name);

	/**
	 * Return the type based on the given clazz, falling back to the provided type
	 * for the default shape.
	 *
	 * @param shape of the type system
	 * @param typeName the type name whose Type object we need.
	 * @param type the type to fallback on
	 * @return the type object
	 */
	public Type getType(Shape shape, String typeName, Type type);
	
	/**
	 * This method uses the persistence mechanism to do any post-processing activity
	 * @param newInstance new instance
	 * @param autoWire  true, if the model needs to be autowired
	 */
	public void postProcess(Object newInstance, boolean autoWire);
	
	/**
	 * The mapper used for creating the external types
	 * @return typemapper object
	 */
	public TypeMapper getTypeMapper();
	
	/**
	 * Gets the builder responsible for forming queries
	 * @return query builder object
	 */
	public QueryTransformer getQueryBuilder();

	/**
	 *
	 * Go through all the registered shapes and sync them.
	 * This will synchronize any modified views with the view objects
	 * held in main memory.
	 *
	 * This should be invoked after all the shapes are registered with the
	 * DataAccessService.
	 *
	 * @param avVersions views grouped by view name
	 */
	public void sync(Map<String, List<AggregateView>> avVersions);

	/**
	 * Returns the PersistenceProvider associated with this DataModel.
	 * The same DataModel should be able to work with different data providers.
	 * For e.g., JPA, JDBC etc
	 * @return PersistenceProvider instance
	 */
	PersistenceProvider getPersistenceProvider();
	
	/**
	 * Set the PersistenceProvider for this DataModel
	 * @param persistenceProvider instance
	 */
	void setPersistenceProvider(PersistenceProvider persistenceProvider);
	
	/**
	 * Get the persistence util specific to a persistence provider.
	 * A persistence provider might support multiple implementations and the
	 * PersistenceUtil encapsulates the specifics of a particular implementation.
	 * 
	 * A persistence provider might not support multiple implementations, so
	 * this is optional.
	 * 
	 * @return PersistenceUtil instance
	 */
	default PersistenceUtil getPersistenceUtil() { return null; }
	
	/**
	 * Set the PersistenceUtil for this DataModel and PersistenceProvider.
     * A persistence provider might not support multiple implementations, so
     * this is optional.
     * 
	 * @param persistenceUtil instance
	 */
	default void setPersistenceUtil(PersistenceUtil persistenceUtil) {  }

	/**
	 * Initialize the generators needed for data generating from an Excel file
	 * @param is InputStream of the Excel domain values file
	 */
	public void initGenerators(InputStream is);

	/**
	 * Returns the SettingsBuilder object
	 * @return SettingsBuilder object
	 */
	public Settings.SettingsBuilder settings();

	/**
	 * Populates the provider defined types within the shape object
	 * @param shape that needs to be populated
	 * @param extension for allowing the framework user to extend the shape
	 * @param entityNames optional, if present then only the types listed in this set is populated from
	 *    the underlying ORM provider
	 */
	void processShape(Shape shape, SchemaExtension extension, Set<String> entityNames);
}
