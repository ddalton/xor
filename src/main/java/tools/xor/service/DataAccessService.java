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

import tools.xor.EntityType;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.util.Edge;
import tools.xor.util.PersistenceType;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
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
	 * Build the Type and Property objects for the static API
	 * Rebuilds an existing shape.
	 *
	 * @param name of the Shape represting the type system
	 * @return shape that was added
	 */
	public Shape addShape(String name);

	/**
	 * Build the Type and Property objects for the static API
	 * Rebuilds an existing shape.
	 *
	 * @param name of the Shape representing the type system
	 * @param extension code to add new types/properties to the shape
	 * @return shape that was added
	 */
	public Shape addShape(String name, SchemaExtension extension);

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
	 * Populate the view mapping with the entity based on the
	 * attributes referenced in the view
	 *
	 * @param shape of the type system
	 * @param entityClass by which the views need to be grouped
	 * @param typeNarrower instance
	 */
	public void populateNarrowedClass(Shape shape, Class<?> entityClass,
			TypeNarrower typeNarrower);

	/**
	 * Get the narrowed class for the given view and the 
	 * actual class. This is a heuristic algorithm and if the
	 * desired class is not found, then the user should override it
	 * manually.
	 *
	 * @param shape of the type system
	 * @param entityClass super class
	 * @param viewName potentially contains attributes from sub classes
	 * @return narrowed class
	 */
	public Class<?> getNarrowedClass(Shape shape, Class<?> entityClass, String viewName);

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
}
