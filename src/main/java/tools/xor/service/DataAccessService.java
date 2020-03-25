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
import tools.xor.util.PersistenceType;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryTransformer;

/**
 * 
 * @author Dilip Dalton
 *
 */
public interface DataAccessService {

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
	 * @param isActive true if the shape is the default shape for the DAS
	 */
	public void addShape(Shape shape, boolean isActive);
	
    /**
     * Add the shape to the DAS.
     * The shape that is added does not affect the active shape.
     *
     * @param name of the Shape representing the type system
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
	 * Remove an existing shape
	 * @param name of the shape
	 */
	public void removeShape(String name);

	/**
	 * Return the type based on the given clazz, falling back to the provided type
	 * for the default shape.
	 *
	 * @param shape of the type system
	 * @param clazz the java class whose Type object we need. Usually this is the desired class.
	 * @param type the type to fallback on
	 * @return the type object
	 */
	public Type getType(Shape shape, Class<?> clazz, Type type);
	
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
	 * Returns the access type
	 * @return persistence type
	 */
	public PersistenceType getAccessType();

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
	 * Creates the PersistenceOrchestrator appropriate for this
	 * DAS
	 * @param sessionContext required if manually creating the session/entityManager
	 * @param data any additional data required by the PersistenceOrchestrator, e.g., 
	 *        persistence unit name
	 * @return PersistenceOrchestrator object
	 */
	public PersistenceOrchestrator createPO(Object sessionContext, Object data);

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
