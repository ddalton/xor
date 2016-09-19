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

package tools.xor.util;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import tools.xor.AggregateAction;
import tools.xor.BasicType;
import tools.xor.BusinessEdge;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ImmutableBO;
import tools.xor.MapperDirection;
import tools.xor.MutableBO;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.service.DataAccessService;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.util.graph.ObjectGraph;

/**
 * This class is used to create the appropriate instances of the copy, given the
 * class of the original object.
 * 
 * @author Dilip Dalton
 */
public class ObjectCreator {
	private static final Logger logger = LogManager.getLogger(new Exception()
	.getStackTrace()[0].getClassName());

	// Map of the source instance and the target DataObject
	// This map also records the target instance and the target DataObject
	private Map<Object, BusinessObject>    instanceDataObjectMap = new Reference2ReferenceOpenHashMap<Object, BusinessObject>();
	private Map<EntityKey, BusinessObject> entitiesByKey = new Object2ReferenceOpenHashMap<EntityKey, BusinessObject>();
	private DataAccessService                  das;
	private PersistenceOrchestrator            persistenceOrchestrator;
	private TypeMapper                         typeMapper;
	private boolean                            share; // when registering if another object with the same id and type is found, it will be returned
	private CreationStrategy                   creationStrategy; // JSON or POJO?
	private boolean                            readOnly; // The objects are read only
	private ObjectGraph                        objectGraph; // represents the Object graph of the object we need to persist
	private BusinessObject                     root; // represents the root object

	public ObjectCreator(DataAccessService das, PersistenceOrchestrator po, MapperDirection direction) {
		this.das = das;
		this.persistenceOrchestrator = po;
		this.typeMapper = das.getTypeMapper().newInstance(direction);
		this.creationStrategy = this.typeMapper.getCreationStrategy(this);
	}
	
	public boolean isReadOnly() {
		return this.readOnly;
	}
	
	public void setReadOnly(boolean value) {
		this.readOnly = true;
	}
	
	public boolean doShare() {
		return share;
	}

	public void setShare(boolean share) {
		this.share = share;
	}	
	
	public PersistenceOrchestrator getPersistenceOrchestrator() {
		return persistenceOrchestrator;
	}

	public void setPersistenceOrchestrator(
			PersistenceOrchestrator persistenceOrchestrator) {
		this.persistenceOrchestrator = persistenceOrchestrator;
	}	
	
	public CreationStrategy getCreationStrategy() {
		return this.creationStrategy;
	}

	public DataAccessService getDAS() {
		return das;
	} 

	public TypeMapper getTypeMapper() {
		return typeMapper;
	}

	public Type getType(Class<?> clazz) {
		clazz = ClassUtil.getUnEnhanced(clazz);
		if(typeMapper.isDomain(clazz))
			return das.getType(clazz);
		else 
			return das.getExternalType(clazz);
	}		
	
	public Type getType(Class<?> inputClass, Type domainType) {
		inputClass = ClassUtil.getUnEnhanced(inputClass);
		if(typeMapper.isDomain(inputClass)) {
			return das.getType(inputClass);
		} else { 
			if(domainType != null) {
				return das.getExternalType(typeMapper.getExternalTypeName(inputClass, domainType));
			} else {
				return das.getExternalType(inputClass);
			}
		}
	}	

	public void addByEntityKey(EntityKey key, BusinessObject entity) {
		if(key == null) {
			return;
		}
		entitiesByKey.put(key, entity);
	}

	public void removeByEntityKey(EntityKey key) {
		entitiesByKey.remove(key);
	}	

	public BusinessObject getByEntityKey(EntityKey key) {
		return entitiesByKey.get(key);		
	}	

	public Set<BusinessObject> getDataObjects() {
		// Retrieve an Identity set
		Set<BusinessObject> result = Collections.newSetFromMap(new IdentityHashMap<BusinessObject, Boolean>());

		for(Object potentialDataObject: instanceDataObjectMap.values()) {
			if(BusinessObject.class.isAssignableFrom(potentialDataObject.getClass()) ) 
				result.add((BusinessObject) potentialDataObject);
		}

		return result;
	}

	public void clearState() {
		instanceDataObjectMap = new IdentityHashMap<Object, BusinessObject>(); 
	}

	/**
	 * Returns true if the given class is under a package that starts with
	 * "java.".
	 * @param c class
	 * @return boolean value
	 */
	protected boolean isJavaOrAppPackage(Class<?> c) {
		if (c == null)
			return false;
		Package p = c.getPackage();
		return p != null
				&& (p.getName().startsWith("java.") || p.getName().startsWith(
						"org.LWSDO."));
	}

	protected boolean immutable(Class<?> c) {
		if (c == null)
			return false;
		return c == String.class || c.isPrimitive() || c.isEnum()
				|| Number.class.isAssignableFrom(c)
				|| Boolean.class == c;
	}

	public BusinessObject getExistingDataObject(Object source) {
		if(source == null)
			return null;

		// The source is based on the instance object so extract if necessary 
		if(source instanceof BusinessObject)
			source = ((BusinessObject)source).getInstance();

		return instanceDataObjectMap.get(source);
	}

	public Object createDataType(Object instance, Property property) throws Exception {
		Object result = creationStrategy.newInstance(instance, (BasicType) property.getType(), null);

		if(logger.isDebugEnabled()) {
			logger.debug("ObjectCreator#createDataType instance: " + (instance==null?"null":instance.toString())
					+ ", property: " + property.getType().getInstanceClass().getName()
					+ ", propertyName: " + property.getName() 
					+ ", result: " + (result==null?"null":result.toString()));
		}
		return result;
	}

	public BusinessObject createDataObject(Object targetInstance, Type targetType, BusinessObject container, Property containmentProperty) {
		BusinessObject result = null;
		
		if(container != null && containmentProperty == null && (container.getContainmentProperty() == null || !container.getContainmentProperty().isMany()) )
			throw new RuntimeException("Cannot access the database without a containment property from the container [containmentProperty: " 
					+ (containmentProperty == null ? "null": containmentProperty.getName()) + "]");
		
		if(containmentProperty != null && !containmentProperty.isContainment()) {
			container = null;
			containmentProperty = null;
		}

		if(targetInstance != null) {
			if(BusinessObject.class.isAssignableFrom(targetInstance.getClass()))
				throw new RuntimeException("targetInstance is a Data Object");
			
			result = (BusinessObject) getExistingDataObject(targetInstance);
			if(result != null && (result.getContainmentProperty() == null || !result.getContainmentProperty().isContainment()) ) {
				result.setContainer(container);
				result.setContainmentProperty(containmentProperty);
			}
		}

		if(result == null) {
			result = this.createDataObject(null, targetInstance, targetType, container, containmentProperty);
		}

		return result;
	}
	
	private void recordIO(Object sourceInstance, BusinessObject dataObject) {
		instanceDataObjectMap.put(sourceInstance, dataObject);
	}

	/**
	 * This method ensures that all derived and reference persistence instance point to the same reference data object 
	 * @param sourceInstance the provided source instance
	 * @param targetInstance the provided target instance or a persistence managed instance
	 * @param targetType the type of the instance
	 * @param container the parent BusinessObject
	 * @param containmentProperty the property referring to the target instance
	 * @return the new BusinessObject
	 */
	private BusinessObject createDataObject(Object sourceInstance, Object targetInstance, Type targetType, BusinessObject container, Property containmentProperty) {

		BusinessObject dataObject = instanceDataObjectMap.get(targetInstance);
		if(dataObject != null && sourceInstance != null) {
			recordIO(sourceInstance, dataObject);
			return dataObject;
		}

		if(readOnly) {
			dataObject = new ImmutableBO(targetType,
					container,
					containmentProperty,
					this);
		} else {
			dataObject = new MutableBO(targetType,
					container,
					containmentProperty,
					this);
		}
		dataObject.setInstance(targetInstance);	

		if(targetInstance != null) {
			dataObject = register(dataObject);
		}

		if(sourceInstance != null)
			recordIO(sourceInstance, dataObject);

		return dataObject;
	}

	public void unregister(BusinessObject dataObject) {
		instanceDataObjectMap.remove(dataObject.getInstance());
		dataObject.removeEntity(dataObject);
	}
	
	public void updateInstance(BusinessObject existingBO, Object oldInstance) {
		instanceDataObjectMap.remove(oldInstance);
		instanceDataObjectMap.put(existingBO.getInstance(), existingBO);
		
		if(MutableBO.class.isAssignableFrom(existingBO.getClass())) {
			objectGraph.replaceInstance((MutableBO) existingBO, existingBO.getInstance());
		}
	}

	/**
	 * Register the user provided object in the object creator cache.
	 * During registration the following issues are handled
	 * 1. Multiple objects with the same id (controlled by the share flag)
	 * 2. Multiple objects with the same id but of different types due to polymorphism. The
	 *    more specific instance is honored
	 *    
	 * @param newDataObject to register
	 * @return resolved BusinessObject
	 */
	public BusinessObject register(BusinessObject newDataObject) {
		BusinessObject result = newDataObject;

		Object instance = newDataObject.getInstance();
		if(instance == null) 
			throw new IllegalStateException("No persistent state found - possible data corruption");

		// If an existing persistent object is already present, check if it is of a more general type, if so it has to be replaced
		// with the current object
		BusinessObject existing = newDataObject.getEntity(newDataObject);
		if(existing != newDataObject) {
			if(existing != null) {
				EntityType existingRootType = ((EntityType)existing.getType()).getRootEntityType();
				EntityType newRootType = ((EntityType)newDataObject.getType()).getRootEntityType();
				if(existingRootType == newRootType) {
					if(share)
						return existing;
					else
						throw new IllegalStateException("Creating two data objects for the same id and root type");
				} else if(existingRootType.getInstanceClass().isAssignableFrom(newRootType.getInstanceClass())) {
					// New data object is of a more specific type (This could be a proxy created by the persistent mechanism)
					Object oldInstance = existing.getInstance();
					existing.setInstance(instance);

					// Clear up references to the more general proxy instance
					instanceDataObjectMap.remove(oldInstance);
					if(objectGraph != null) {
						objectGraph.replaceInstance(existing, instance);
					}
					result = existing; // we have replace the instance with the new instance
				} else {
					result = existing; // new data object is of a more general type, so we use the existing data object				
				}
			} else {
				// Register if it is a persistent instance
				newDataObject.addEntity(newDataObject);
			}
		} else {
			throw new IllegalStateException("New data object was registered by a different means!");
		}

		// Do the actual registration
		recordIO(result.getInstance(), result);

		return result;
	}
	
	public BusinessObject createTarget(CallInfo ci) {
		return createTarget(ci, null, null);
	}		
	
	/**
	 * Create the target BusinessObject based on the TypeMapper direction target type
	 * 
	 * @param ci CallInfo object
	 * @param targetInstance A reference to an already created target instance
	 * @param domainEntityType The desired type for the target if present
	 * @return new BusinessObject
	 */	
	public BusinessObject createTarget(CallInfo ci, Object targetInstance, Type domainEntityType) {

		targetInstance = ClassUtil.getInstance(targetInstance);
		Object sourceInstance = ClassUtil.getInstance(ci.getInput());
		Property sourceContainmentProperty = ci.getInputProperty();
		BusinessObject container = null;
		if(ci.getParent() != null) {
			container = (BusinessObject)ci.getParent().getOutput();
		}

		// Get the target BusinessObject mapped to the source instance
		BusinessObject result = getExistingDataObject(sourceInstance);
		if(result != null && (targetInstance != null && targetInstance != ClassUtil.getInstance(result)) )
			throw new IllegalStateException("existing target instance not the same as the passed in target instance");

		// Create the BusinessObject wrapper for the passed in target instance
		if(targetInstance != null) {
			// Check if an existing BusinessObject already exists for this target instance
			BusinessObject potentialResult = getExistingDataObject(targetInstance);
			if(potentialResult != null && result != null && potentialResult != result)
				throw new IllegalStateException("Source and target instance data objects are different");

			if(result == null && potentialResult != null) {
				// Let the source instance be associated with the target BusinessObject
				if(sourceInstance != null)
					recordIO(sourceInstance, potentialResult);
				result = potentialResult;
			}
		}

		try {
			Class<?> targetInstanceClass = null;
			if(targetInstance == null) {
				//targetInstanceClass = sourceInstance != null ? ClassUtil.getUnEnhanced(sourceInstance.getClass()) : null;

				if(domainEntityType == null) {

					if(sourceInstance instanceof BusinessObject) {
						EntityType type = (EntityType) ((BusinessObject)sourceInstance).getType();
						targetInstanceClass = typeMapper.getTargetClass(type.getInstanceClass(), ci);
					} else {

							targetInstanceClass = typeMapper.getTargetClass(sourceInstance.getClass(), ci);
							/*
							// Unable to resolve the domain type
							// So we explicitly try and get it
							// TODO: narrowing
							Type type = ci.getInputProperty().getDomainProperty().getType();
							instanceClass = typeMapper.getSourceClass(type.getInstanceClass());
							*/
					}
					
				} else {
					if(typeMapper.isToExternal()) {
						
						targetInstanceClass = typeMapper.toExternal(domainEntityType.getInstanceClass());
						//Class<?> externalClass = typeMapper.toExternal(domainEntityType.getInstanceClass());
						//instanceClass = das.getExternalType(typeMapper.getExternalTypeName(externalClass, domainEntityType)).getInstanceClass();
					} else {
						targetInstanceClass = domainEntityType.getInstanceClass();
					}
				}
			} else {
				targetInstanceClass = targetInstance.getClass();
			}

			Type targetType = null;
			if(sourceInstance != null && typeMapper.isDomain( ClassUtil.getUnEnhanced(sourceInstance.getClass()))) {
				Type domainType = getType(ClassUtil.getUnEnhanced(sourceInstance.getClass()));
				if(typeMapper.isOpen(targetInstanceClass)) {
					if(domainEntityType != null) {
						domainType = domainEntityType;
					}
				}
				targetType = getType(targetInstanceClass, domainType );
			} else {
				try {
					targetType = getType(targetInstanceClass, domainEntityType);
				} catch(NullPointerException e) {
					System.out.println("NullPointerException: " + targetInstanceClass);
				}
			}
			Property containmentProperty = (container == null || sourceContainmentProperty == null) ? null : container.getInstanceProperty(sourceContainmentProperty.getName()); 	

			/* 
			 * We need to fetch a persistent instance for the following:
			 * 1. AggregateAction is CREATE or CLONE and the object being fetched is not part of the aggregate
			 * 2. Any UPDATE or MERGE action
			 */
			AggregateAction action = ci.getSettings().getAction();
			boolean isDependent = false;
			if(ci.getInput() != null)
				isDependent = ((BusinessObject)ci.getInput()).isDependent();
			boolean fetchPersistent =  ( (action == AggregateAction.CREATE || action == AggregateAction.CLONE) && !isDependent ) ||
					(action == AggregateAction.MERGE || action == AggregateAction.UPDATE || action == AggregateAction.LOAD);
			if( fetchPersistent && targetInstance == null) {

				if(result == null) {
					targetInstance = ci.getSettings().getAssociationStrategy().execute(ci, this);
					if(!ci.isDataType() && ci.getInputObjectCreator().getExistingDataObject(targetInstance) != null) {
						if(action != AggregateAction.LOAD && action != AggregateAction.CLONE) {
							logger.info("Directly modifying a persistence managed object.");
						}
					}
					
					if(targetInstance != null) {
						result = createDataObject(sourceInstance, targetInstance, targetType, container, containmentProperty);
						((BusinessObject)result).setPersistent(true);
					}
				}
			} 

			if(result == null) {
				if(targetInstance == null) {
					targetInstance = createInstance(sourceInstance, targetInstanceClass, targetType, container, containmentProperty);
					if(targetInstance == null) {
						logger.error(
								"ObjectCreator#createTarget Unable to create targetInstance of class: "	+ targetInstanceClass.getName() 
								+ " and source instance: " + sourceInstance.getClass().getName()
								+ " and target type: " + targetType.getName());
					}
				}
				result = createDataObject(sourceInstance, targetInstance, targetType, container, containmentProperty);
			}

			// Give opportunity for reference objects to have any post processing done by the DAS
			das.postProcess(targetInstance, ci.getSettings().isAutoWire());
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}	

		return result;
	}	

	protected Object createInstance(Object source, Class<?> targetClass, Type targetType, BusinessObject container, Property containmentProperty) throws Exception {
		Object instance = creationStrategy.newInstance(source, (BasicType) targetType, targetClass, container, containmentProperty);
		//setEmbeddedInstance(instance, targetType);

		return instance;
	}

	public Object createInstance(Type targetType) throws Exception {
		Object instance = creationStrategy.newInstance(null, (BasicType) targetType, null);
		//setEmbeddedInstance(instance, targetType);

		return instance;
	}	

	public void clearVisited() {
		for(Object object: instanceDataObjectMap.values()) {
			BusinessObject node = (BusinessObject) object;
			node.setVisited(false);
		}
	}
	
	public ObjectGraph getObjectGraph() {
		return this.objectGraph;
	}
	
	public void setRoot(BusinessObject root) {
		this.root = root;
	}
	
	/**
	 * This method sets the root for MutableBO Business objects
	 * in the output ObjectCreator
	 * @param root of the object graph
	 */
	public void setObjectGraph(BusinessObject root) {
		this.root = root;
		if(this.objectGraph == null) {
			this.objectGraph = new ObjectGraph<MutableBO, BusinessEdge>(root);
		}
	}
	
	public BusinessObject getRoot() {
		return root;
	}
	
	public void persistGraph(Settings settings) {
		objectGraph.persistGraph(this, settings);
	}
}
