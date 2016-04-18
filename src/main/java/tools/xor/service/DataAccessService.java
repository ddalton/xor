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

import java.util.List;
import java.util.Map;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.TypeNarrower;
import tools.xor.util.PersistenceType;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryBuilder;

/**
 * 
 * @author Dilip Dalton
 *
 */
public interface DataAccessService {
	/**
	 * Build the Type and Property objects for the static API
	 */
	public void define();
	
	/**
	 * Return the type associated with a particular DataObject
	 * @param The java class whose Type object we need
	 * @return The Type object
	 */
	public Type getType(Class<?> clazz);
	
	/**
	 * Returns the External type for this class.
	 * TODO: Needs to support dynamic types, i.e., extract schema from a JSONObject etc...
	 * 
	 * @param clazz
	 * @return
	 */
	public Type getExternalType(Class<?> clazz);		
	
	/**
	 * Get the type first by looking for the class name and then fall-back to the entity name
	 * @param name
	 * @return
	 */
	public Type getType(String name);

	/**
	 * Return the External type with the specific name
	 * @param name
	 * @return
	 */
	public Type getExternalType(String name);
	
	/**
	 * Return all the types managed by this service
	 */
	public List<Type> getTypes();
	
	/**
	 * This method uses the persistence mechanism to do any post-processing activity
	 * @param newInstance
	 * @param autoInject  true, if the model needs to be autowired 
	 * @return
	 */
	public void postProcess(Object newInstance, boolean autoWire);
	
	/**
	 * The mapper used for creating the external types
	 * @return
	 */
	public TypeMapper getTypeMapper();
	
	/**
	 * Gets the builder responsible for forming queries
	 * @return
	 */
	public QueryBuilder getQueryBuilder();
	
	/**
	 * Lists the names of the aggregates
	 * @return
	 */
	public List<String> getAggregateList();
	
	/**
	 * Returns the access type
	 */
	public PersistenceType getAccessType();

	/**
	 * Synchronize any modified views with the view objects
	 * held in main memory
	 * @param am
	 * @param avVersions
	 */
	public void sync(AggregateManager am,
			Map<String, List<AggregateView>> avVersions);

	/**
	 * Refresh the view mapping with the entity based on the
	 * attributes referenced in the view
	 * @param abstractTypeNarrower
	 */
	public void refresh(TypeNarrower typeNarrower);

	/**
	 * Populate the view mapping with the entity based on the
	 * attributes referenced in the view
	 * @param entityClass
	 * @param abstractTypeNarrower
	 */
	public void populateNarrowedClass(Class<?> entityClass,
			TypeNarrower typeNarrower);

	/**
	 * Get the narrowed class for the given view and the 
	 * actual class. This is a heuristic algorithm and if the
	 * desired class is not found, then the user should override it
	 * manually.
	 * 
	 * @param entityClass
	 * @param viewName
	 * @return
	 */
	public Class<?> getNarrowedClass(Class<?> entityClass, String viewName);

	/**
	 * Get the view meta of the given view name
	 * @param viewName
	 * @return
	 */
	public AggregateView getView(String viewName);

	/**
	 * Get the default view for the aggregate rooted at entityType
	 * This contains all the attributes of the aggregate
	 * @param entityType
	 * @return
	 */
	public AggregateView getView(EntityType entityType);

	/**
	 * Add a view programmatically.
	 * @param view
	 */
	public void addView(AggregateView view);
	

	/**
	 * Get a list of all the default generated views
	 */
	public List<AggregateView> getViews();	
	
	/**
	 * Get the default view of just the base properties of the 
	 * aggregate rooted at entityType, i.e., the direct properties of
	 * the entityType
	 * 
	 * @param entityType
	 * @return
	 */
	public AggregateView getBaseView(EntityType entityType);	

	/**
	 * Get a list of all the views in the system
	 * @return
	 */
	public List<String> getViewNames();

	/**
	 * Creates the PersistenceOrchestrator appropriate for this
	 * DAS
	 * @param sessionContext required if manually creating the session/entityManager
	 * @param data any additional data required by the PersistenceOrchestrator, e.g., 
	 *        persistence unit name
	 * @return
	 */
	public PersistenceOrchestrator createPO(Object sessionContext, Object data);

	/**
	 * Used to extend the property for a type. e.g., add a new open property
	 * @param taskType
	 * @param openProperty
	 */
	public void addProperty (EntityType taskType, Property openProperty);
}
