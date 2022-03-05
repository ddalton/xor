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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import tools.xor.AbstractBO;
import tools.xor.AbstractProperty;
import tools.xor.AbstractTypeMapper;
import tools.xor.AggregateAction;
import tools.xor.BasicType;
import tools.xor.BusinessEdge;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.ImmutableBO;
import tools.xor.ListType;
import tools.xor.MutableBO;
import tools.xor.NaturalEntityKey;
import tools.xor.ProcessingStage;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.SurrogateEntityKey;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.providers.jdbc.CustomPersister;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.service.DataModel;
import tools.xor.service.DataStore;
import tools.xor.service.Shape;
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

	// For those business objects that have a natural key, keep track of it
	// The natural key can change during XOR operation for a Business Object as the state changes
	// when moving data between the two models (External <-> Domain).
	// and this data structure is used to keep the entitiesByKey data structure
	// in sync.
	private Map<BusinessObject, List<EntityKey>> naturalKeyRegistrations = new Reference2ReferenceOpenHashMap<BusinessObject, List<EntityKey>>();


	private DataModel               dataModel;
	private Shape                   shape;
	private DataStore dataStore;
	private TypeMapper              typeMapper;
	private boolean                 share; // when registering if another object with the same id and type is found, it will be returned
	private CreationStrategy        creationStrategy; // JSON or POJO?
	private boolean                 readOnly; // The objects are read only
	private ObjectGraph             objectGraph; // represents the Object graph of the object we need to persist
	private BusinessObject          root; // represents the root object
	private Settings                settings; // the criteria under which this instance operates

	public ObjectCreator(Settings settings, DataStore po, TypeMapper typeMapper) {
		this.settings = settings;
		this.dataStore = po;
		this.typeMapper = typeMapper;
		this.creationStrategy = this.typeMapper.getCreationStrategy(this);
		
		this.shape = this.typeMapper.getShape();
		this.dataModel = this.typeMapper.getModel();
	}

	public Settings getSettings() {
		return this.settings;
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
	
	public DataStore getDataStore() {
		return dataStore;
	}

	public void setDataStore(
			DataStore dataStore) {
		this.dataStore = dataStore;
	}	
	
	public CreationStrategy getCreationStrategy() {
		return this.creationStrategy;
	}

	public Shape getShape() {
		return this.shape;
	}

	public DataModel getDataModel() {
		return dataModel;
	} 

	public TypeMapper getTypeMapper() {
		return typeMapper;
	}

	public Type getType(Class<?> clazz) {
		clazz = ClassUtil.getUnEnhanced(clazz);
		return shape.getType(clazz);
	}

	/**
	 * Convert it to type based on the java class
	 * @param bo business object whose java class type we need to infer
	 * @return type
	 */
	public Type getDomainType(BusinessObject bo) {
		// We have the same type for JDBC
		if(getDataStore() instanceof JDBCDataStore) {
			return bo.getType();
		}

		Object instance = ClassUtil.getInstance(bo);
		Class<?> clazz = ClassUtil.getUnEnhanced(instance.getClass());

		return typeMapper.getDomainShape().getType(clazz);		
	}
	
    public Type getType(String typeName, Type domainType) {
        // We have the same type for JDBC
        if (getDataStore() instanceof JDBCDataStore) {
            return domainType;
        }

        if (domainType != null && domainType instanceof EntityType) {
            return shape.getType(((EntityType) domainType).getEntityName());
        } else {
            return shape.getType(typeName);
        }
    }	

    public Type getType(Class<?> inputClass, Type domainType) {
        // We have the same type for JDBC
        if (getDataStore() instanceof JDBCDataStore) {
            return domainType;
        }

        inputClass = ClassUtil.getUnEnhanced(inputClass);
        if (domainType != null && domainType instanceof EntityType) {
            return shape.getType(((EntityType) domainType).getEntityName());
        } else {
            return shape.getType(inputClass);
        }
    }

	public void addByNaturalKey(BusinessObject entity, String anchor) {
		if(naturalKeyRegistrations.containsKey(entity)) {
			for(EntityKey ek: naturalKeyRegistrations.get(entity)) {
				entitiesByKey.remove(ek);
			}
		}

		List<EntityKey> naturalKeys = entity.getNaturalKey(anchor);
		for(EntityKey naturalKey: naturalKeys) {
			entitiesByKey.put(naturalKey, entity);
		}
		naturalKeyRegistrations.put(entity, naturalKeys);
	}

	public void addBySurrogateKey(BusinessObject entity, String anchor) {
		EntityKey entityKey = entity.getSurrogateKey(anchor);
		if(entityKey != null) {
			entitiesByKey.put(entityKey, entity);
		}
	}

	public void removeByEntityKey(EntityKey key) {

		if(key instanceof NaturalEntityKey) {
			naturalKeyRegistrations.remove(key);
		}

		entitiesByKey.remove(key);
	}

	/**
	 * Retrieve the entity given an entity key
	 * @param key entity key
	 * @param entityType additional information to retrieve an entity successfully if it is
	 *                   part of an inheritance hierarchy
	 * @return BusinessObject indexed by the key
	 */
	public BusinessObject getByEntityKey(EntityKey key, Type entityType) {

		/* Currently this is not needed
		if(key instanceof SurrogateEntityKey) {
			BusinessObject result = null;
			do {
				result = entitiesByKey.get(key);
				if(entityType instanceof EntityType) {
					entityType = ((EntityType) entityType).getSuperType();
				} else {
					entityType = null;
				}
				// If this is a subtype try to locate it using it's parent type if possible
			} while (result == null && entityType != null);
			return result;
		} else {
			return entitiesByKey.get(key);
		}
		*/

		return entitiesByKey.get(key);
	}

	/**
	 * 	if anchor is set then we get ObjectResolver.Type#DISTINCT objects
	 *  else if path is null we get ObjectResolver.Type#SHARED objects
	 * @param idValue surrogate key value if present
	 * @param naturalKeyValues natural key values if the entity does not have a surrogate key
	 * @param type of the entity
	 * @param anchor where this type is anchored. Different type shapes can be present at different
	 *               anchors.
	 * @return businessObject instance
	 */
	public BusinessObject findEntity(Object idValue, Map<String, Object> naturalKeyValues, Type type, String anchor) {

		BusinessObject bo = null;

		if(naturalKeyValues.size() > 0) {
			bo = getByEntityKey(
				new NaturalEntityKey(
					naturalKeyValues,
					type.getName(),
					anchor), type);
		}

		if(bo != null) {
			return bo;
		}

		if(idValue != null && ((EntityType)type).getIdentifierProperty() != null) {
			bo = getByEntityKey(new SurrogateEntityKey(idValue,
				AbstractTypeMapper.getSurrogateKeyTypeName(type), anchor), type);
		}

		return bo;
	}

	public Set<BusinessObject> 	getDataObjects() {
		// Retrieve an Identity set
		Set<BusinessObject> result = Collections.newSetFromMap(new IdentityHashMap<BusinessObject, Boolean>());

		for(Object potentialDataObject: instanceDataObjectMap.values()) {
			if(BusinessObject.class.isAssignableFrom(potentialDataObject.getClass()) ) 
				result.add((BusinessObject) potentialDataObject);
		}

		return result;
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

		if(property == null) {
			throw new RuntimeException("property is null, probably the object is a subtype instance and if so, the narrow flag needs to be set in settings");
		}

		Class<?> toClass = null;
		if(!property.isMany()) {
			toClass = (instance == null) ? null : instance.getClass();
		}

		Object result = creationStrategy.newInstance(instance, (BasicType) property.getType(), toClass);

		if(logger.isDebugEnabled()) {
			//logger.debug("ObjectCreator#createDataType instance: " + (instance==null?"null":instance.toString())
			logger.debug("ObjectCreator#createDataType instance: " + (instance==null?"null":instance.getClass().getName())
					+ ", property: " + property.getType().getName()
					+ ", propertyName: " + property.getName() 
					+ ", result: " + (result==null?"null":result.toString()));
		}
		return result;
	}

	public BusinessObject createDataObject(Object targetInstance, Type targetType, BusinessObject container, Property containmentProperty) {
		return createDataObject(targetInstance, targetType, container, containmentProperty, null);
	}

	public BusinessObject createDataObject(Object targetInstance, Type targetType, BusinessObject container, Property containmentProperty, String anchor) {
		BusinessObject result = null;
		
		if(container != null && containmentProperty == null && (container.getContainmentProperty() == null || !container.getContainmentProperty().isMany()) )
			throw new RuntimeException("Cannot access the database without a containment property from the container [containmentProperty: " 
					+ (containmentProperty == null ? "null": containmentProperty.getName()) + "]");

		// We always treat the collection object as not-shareable. So its container and containmentProperty is always populated.
		if(containmentProperty != null && !containmentProperty.isContainment() && !containmentProperty.isMany()) {
			container = null;
			containmentProperty = null;
		}

		if(targetInstance != null) {
			if(BusinessObject.class.isAssignableFrom(targetInstance.getClass()))
				throw new RuntimeException("targetInstance is a Data Object");
			
			result = getExistingDataObject(targetInstance);
			if(result != null && (result.getContainmentProperty() == null || !result.getContainmentProperty().isContainment()) ) {
				result.setContainer(container);
				result.setContainmentProperty(containmentProperty);
			}
		}

		if(result == null) {
			result = this.createDataObject(null, targetInstance, targetType, container, containmentProperty, anchor);
		}

		if(getSettings().getDetector() != null) {
			getSettings().getDetector().notifyCreate(null, result, targetInstance);
		}

		return result;
	}
	
	private void recordIO(Object sourceInstance, BusinessObject dataObject) {
		instanceDataObjectMap.put(sourceInstance, dataObject);
	}

	/**
	 * This method ensures that all derived and reference persistence instance point to the same reference data object 
	 * @param sourceBO the provided source BusinessObject
	 * @param targetInstance the provided target instance or a persistence managed instance
	 * @param targetType the type of the instance
	 * @param container the parent BusinessObject
	 * @param containmentProperty the property referring to the target instance
	 * @param anchor the path in the state tree graph where this object is created
	 * @return the new BusinessObject
	 */
	private BusinessObject createDataObject(Object sourceBO, Object targetInstance, Type targetType, BusinessObject container, Property containmentProperty, String anchor) {

		Object sourceInstance = ClassUtil.getInstance(sourceBO);
		BusinessObject dataObject = instanceDataObjectMap.get(targetInstance);
		if(dataObject != null) {
			if(sourceInstance != null) {
				recordIO(sourceInstance, dataObject);
			}
			return dataObject;
		}

		if(readOnly) {
			dataObject = new ImmutableBO(targetType,
					container,
					containmentProperty,
					this);
		} else {
			// coerce type to the correct shape
			if(targetType instanceof EntityType && ((EntityType)targetType).isDomainType() ) {
				targetType = shape.getType(targetType.getName());
			}
			dataObject = new MutableBO(targetType,
					container,
					containmentProperty,
					this);
		}
		dataObject.setInstance(targetInstance);

		// NOTE: dataObject is registered only after its primitive typed properties have
		// been populated. See AbstractOperation#process
		if(targetInstance != null) {
			// Handle the case where the natural key points to entities
			// Needed for createWrapper to work properly
			if(targetType instanceof EntityType && ((EntityType)targetType).getNaturalKey() != null) {
				for(String key: ((EntityType)targetType).getNaturalKey()) {
					Property pKey = targetType.getProperty(key);
					if(pKey.getType() instanceof EntityType) {
						Object keyInstance = ((AbstractProperty)ClassUtil.getDelegate(pKey)).query(targetInstance);
						if(keyInstance != null) {
							String keyAnchor = anchor == null ? null : anchor + Settings.PATH_DELIMITER +pKey.getName();
							this.createDataObject(null, keyInstance, pKey.getType(), null, null, keyAnchor);
						}
					}
				}
			}

			dataObject = register(dataObject, sourceBO, false, anchor);
		}

		if(sourceInstance != null) {
			recordIO(sourceInstance, dataObject);
		}

		return dataObject;
	}

	// anchor is not relevant for GraphTraversal
	public void unregister(BusinessObject dataObject) {
		unregister(dataObject, null);
	}

	public void unregister(BusinessObject dataObject, String anchor) {
		instanceDataObjectMap.remove(dataObject.getInstance());
		dataObject.removeEntity(dataObject, null);
	}
	
	public void updateInstance(BusinessObject existingBO, Object oldInstance) {
		if(oldInstance != null) {
			instanceDataObjectMap.remove(oldInstance);
		}
		if(existingBO.getInstance() != null) {
			instanceDataObjectMap.put(existingBO.getInstance(), existingBO);
		}
		
		if(MutableBO.class.isAssignableFrom(existingBO.getClass()) && objectGraph != null) {
			objectGraph.replaceInstance(existingBO, existingBO.getInstance());
		}
	}

	private String getNaturalKeyString(EntityType entityType) {

		StringBuilder naturaKeyString = new StringBuilder();
		for(String key: entityType.getExpandedNaturalKey()) {
			if(naturaKeyString.length() == 0) {
				naturaKeyString.append(" {");
			} else {
				naturaKeyString.append(",");
			}
			naturaKeyString.append(key);
		}
		naturaKeyString.append("}");

		return new StringBuilder().append(entityType.getName()).append(naturaKeyString).toString();
	}

	private void evictTemporaryDomainObject(BusinessObject temporaryDomainObject) {
		if(temporaryDomainObject == null || temporaryDomainObject.getInstance() == null) {
			return;
		}

		if(temporaryDomainObject instanceof MutableBO) {
			((MutableBO)temporaryDomainObject).setEvicted(true);
		}

		// Make sure to evict the domain object from the persistence
		// layer as it is going to be swizzled
		// We don't want this swizzled out object to be saved for any reason
		Set objectsToClear = new HashSet();
		objectsToClear.add(temporaryDomainObject);
		getDataStore().clear(objectsToClear);
	}

	/**
	 * Register the user provided object in the object creator cache. Used if the object is deemed shareable.
	 * During registration the following issues are handled
	 * 1. Multiple objects with the same id (controlled by the share flag)
	 * 2. Multiple objects with the same id but of different types due to polymorphism. The
	 *    more specific instance is honored
	 *
	 * @param newDataObject to register
	 * @param sourceBO source BusinessObject
	 * @param naturalKeysPopulated flag to indicate if the natural key fields have already been populated in the newDataObject
	 * @param anchor state tree path that determines the shape we are interested in
	 * @return resolved BusinessObject
	 */
	public BusinessObject register (BusinessObject newDataObject, Object sourceBO, boolean naturalKeysPopulated, String anchor) {
		Object instance = newDataObject.getInstance();
		if (instance == null)
			throw new IllegalStateException("No persistent state found - possible data corruption");

		// External model objects, may create multiple copies of the same object for serialization purposes
		// So they should be de-duplicated and point to a single domain model object.
		boolean swizzle = false;
		if (naturalKeysPopulated &&
			sourceBO != null &&
			newDataObject.getType() instanceof EntityType &&
			sourceBO instanceof BusinessObject &&
			AggregateAction.isModificationAction(settings.getAction())) {
			if (!((BusinessObject)sourceBO).isReference()) {

				// Note: Swizzling should be done only after the natural keys have
				// been populated.
				// Also swizzling should occur only during the CREATE stage
				swizzle = true;
			}
		}

		// Embedded instances are not shareable, so we don't register them and
		// External model objects in a modification operation should not lose information by de-duplicating in the external model itself
		if (!newDataObject.getType().isDataType() && (
			((EntityType)newDataObject.getType()).isEmbedded() ||
				(!((EntityType)newDataObject.getType()).isDomainType()
					&& AggregateAction.isModificationAction(settings.getAction())))) {
			recordIO(newDataObject.getInstance(), newDataObject);
			return newDataObject;
		}

		// If an existing persistent object is already present, check if it is of a more general type, if so it has to be replaced
		// with the current object
		BusinessObject existing = newDataObject.getEntity(newDataObject, anchor);
		BusinessObject result = newDataObject;
		if (existing != newDataObject) {
			if (existing != null) {
				EntityType existingRootType = ((EntityType)existing.getType()).getRootEntityType();
				EntityType newRootType = ((EntityType)newDataObject.getType()).getRootEntityType();
				if (!swizzle) {
					if (existingRootType == newRootType) {
						// Embedded data objects without natural keys are not shareable
						if (share && !(existingRootType.isEmbedded()
							&& ((EntityType)newDataObject.getType()).getNaturalKey() == null)) {
							if (newDataObject.getInstance() != existing.getInstance()) {
								// Make sure we can re-fetch the BO by the other instance also
								recordIO(newDataObject.getInstance(), existing);
								if (newDataObject.isDomainType()) {
									evictTemporaryDomainObject(newDataObject);
								}
							}
							result = existing;
						}
						else {
							if (((EntityType)newDataObject.getType()).getNaturalKey() != null) {
								throw new IllegalStateException(
									"NaturalKey field(s) "
										+ getNaturalKeyString((EntityType)newDataObject.getType())
										+ " is either not populated or has duplicate values. Please check.");
							}
							else {
								throw new IllegalStateException(
									"There is more than 1 dataObject instance representing the same entity (same id and root type). Please check if XOR.id is populated. [type: "
										+ existingRootType.getEntityName() + ", id: "
										+ newDataObject.getIdentifierValue() + "]" + (
										existingRootType.isEmbedded() ?
											". Check your data as sharing of embedded objects is not allowed." :
											""));
							}
						}
					}
					else if (existingRootType.isSameOrSupertypeOf(newRootType)) {
						swizzle = true;
					}
					else {
						result = existing; // new data object is of a more general type, so we use the existing data object
					}
				}

				// We swizzle-out the exiting  instance
				// and swizzle-in the newDataObject instance
				if (swizzle) {

					// Evict the old instance from the persistence layer first
					// as it is going to be swizzled with the instance from the newDataObject
					evictTemporaryDomainObject(existing);

					// New data object is of a more specific type (This could be a proxy created by the persistent mechanism)
					// or the old object was just a reference association and the new data object is the real object
					Object oldInstance = existing.getInstance();
					existing.setInstance(instance);

					// preserve the link from the old instance as it refers to the same object,
					// the only difference being it now points to a more specific subtype, or the real object instead of a reference
					recordIO(oldInstance, existing);

					result = existing;
				}

				// Fix the source instance to the existing BO
				if (ClassUtil.getInstance(sourceBO) != null) {
					recordIO(ClassUtil.getInstance(sourceBO), result);
				}
			}

			// Do the actual registration
			recordIO(result.getInstance(), result);
		}

		addBySurrogateKey(result, anchor);

		// We delay natural key registration until they are assigned
		if(naturalKeysPopulated) {
			//result.register();
			addByNaturalKey(result, anchor);
		}

		// Mark the BusinessObject as reference or actual object
		if(result instanceof MutableBO && sourceBO != null) {
			if (swizzle) {
				((MutableBO)result).setReference(false);
			} else if(((AbstractBO)sourceBO).isReference()) {
				((MutableBO)result).setReference(true);
			} else {
				((MutableBO)result).setReference(false);
			}
		}

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
		Object sourceBO = ci.getInput();
		Property sourceContainmentProperty = ci.getInputProperty();
		BusinessObject container = null;
		if(ci.getParent() != null && !ci.getParent().isBulkInput()) {
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

		boolean createdInstance = false;
		try {
			Class<?> targetInstanceClass = null;
			if(targetInstance == null) {
				//targetInstanceClass = sourceInstance != null ? ClassUtil.getUnEnhanced(sourceInstance.getClass()) : null;

				if(domainEntityType == null) {
                    if (sourceInstance != null) {
                        String mappedClassName = typeMapper.getMappedType(sourceInstance.getClass().getName(), ci);
                        targetInstanceClass = mappedClassName != null ? Class.forName(mappedClassName) : null;
                    } else {
                        targetInstanceClass = typeMapper.toExternal((Type)null);
                    }
				} else {
					if(typeMapper.isExternalSide()) {
						
						targetInstanceClass = typeMapper.toExternal(domainEntityType);
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
			if(sourceInstance != null && typeMapper.isDomain( ClassUtil.getUnEnhanced(sourceInstance.getClass()).getName())) {
				Type domainType = getDomainType((BusinessObject)ci.getInput());
				if(typeMapper.isOpen(targetInstanceClass)) {
					if(domainEntityType != null) {
						domainType = domainEntityType;
					}
				}
				targetType = getType(targetInstanceClass, domainType );
			} else {
				try {
					targetType = getType(targetInstanceClass, domainEntityType);
				}
				catch (NullPointerException e) {
					System.out.println("NullPointerException: " + targetInstanceClass);
				}
			}
			Property containmentProperty = (container == null || sourceContainmentProperty == null) ? null : container.getInstanceProperty(sourceContainmentProperty.getName()); 	

			/* 
			 * We need to fetch a persistent instance for the following:
			 * 1. AggregateAction is CREATE, CLONE or DELETE and the object being fetched is not part of the aggregate
			 * 2. Any UPDATE or MERGE action
			 */
			AggregateAction action = ci.getSettings().getAction();
			boolean isDependent = false;
			if(ci.getInput() != null)
				isDependent = ((BusinessObject)ci.getInput()).isDependent();
			boolean fetchPersistent =
				((action == AggregateAction.CREATE || action == AggregateAction.CLONE
					|| action == AggregateAction.DELETE) && !isDependent) ||
					(action == AggregateAction.MERGE || action == AggregateAction.UPDATE
						|| action == AggregateAction.LOAD);
			if( fetchPersistent && targetInstance == null) {

				if(result == null) {
					targetInstance = ci.getSettings().getAssociationStrategy().execute(ci, this);
					if(!ci.isDataType() && ci.getInputObjectCreator().getExistingDataObject(targetInstance) != null) {
						if(action != AggregateAction.LOAD && action != AggregateAction.CLONE) {
							logger.info("Directly modifying a persistence managed object.");
						}
					}
					
					if(targetInstance != null) {
						result = createDataObject(sourceBO, targetInstance, targetType, container, containmentProperty, null);
						result.setPersistent(true);
					}
				}
			}

			if(result == null) {

				if(targetInstance == null) {
					// Exception for Bulk processing
					if(targetType == null && domainEntityType instanceof ListType) {
						targetType = domainEntityType;
					}

					if(settings.isShouldCreate(targetType)) {
						targetInstance = createInstance(
							sourceInstance,
							targetInstanceClass,
							targetType,
							container,
							containmentProperty);

						createdInstance = true;

						if (targetInstance == null) {
							logger.error(
								"ObjectCreator#createTarget Unable to create targetInstance of class: "
									+ targetInstanceClass.getName()
									+ " and source instance: " + sourceInstance.getClass().getName()
									+ " and target type: " + targetType.getName()
									+ ". The instance has probably been deleted"
							);
						}
					}
				}

				if(targetInstance != null) {
					result = createDataObject(
						sourceBO,
						targetInstance,
						targetType,
						container,
						containmentProperty,
						null);
				}
			}

			// Give opportunity for reference objects to have any post processing done by the DAS
			if(targetInstance != null) {
				dataModel.postProcess(targetInstance, ci.getSettings().isAutoWire());
			}
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		if(createdInstance && settings.getDetector() != null) {
			settings.getDetector().notifyCreate(sourceInstance, result, targetInstance);
		}

		return result;
	}	

	protected Object createInstance(Object source, Class<?> targetClass, Type targetType, BusinessObject container, Property containmentProperty) throws Exception {
		Object instance = creationStrategy.newInstance(source, (BasicType) targetType, targetClass, container, containmentProperty);
		//setEmbeddedInstance(instance, targetType);

		if(settings.getDetector() != null) {
			settings.getDetector().notifyCreate(source, null, instance);
		}

		return instance;
	}

	public Object createInstance(Type targetType) throws Exception {
		Object instance = creationStrategy.newInstance(null, (BasicType) targetType, null);
		//setEmbeddedInstance(instance, targetType);

		if(settings.getDetector() != null) {
			settings.getDetector().notifyCreate(null, null, instance);
		}

		return instance;
	}

	public Object patchInstance(EntityType targetType) throws Exception {
		return creationStrategy.patchInstance(targetType);
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
		if(settings.getDataStore() instanceof JDBCDataStore) {
			CustomPersister cp = ((JDBCDataStore)settings.getDataStore()).getSessionContext();
			cp.persistGraph(this, settings);
		} else {
			objectGraph.persistGraph(this, settings);
		}
	}

	public void deleteGraph(Settings settings) {
		if(settings.getDataStore() instanceof JDBCDataStore) {
			CustomPersister cp = ((JDBCDataStore)settings.getDataStore()).getSessionContext();
			cp.deleteGraph(this, settings);
		} else {
			objectGraph.deleteGraph(this, settings);
		}
	}

	public static class CreateInstanceTracker implements Detector
	{
		private Class clazz;
		private String propertyName;
		private List<Object> skippedInstances;
		private List<BusinessObject> bos;
		private Map<Object, Object> allInstances;

		public CreateInstanceTracker(Class clazz, String propertyName) {
			this.clazz = clazz;
			this.propertyName = propertyName;
			this.skippedInstances = new ArrayList<>();
			this.bos = new ArrayList<>();
			this.allInstances = new IdentityHashMap<>();
		}

		@Override public void notifyCreate (Object source, BusinessObject createdBO, Object createdInstance)
		{
			if( createdInstance != null && clazz.isAssignableFrom(createdInstance.getClass())  ) {
				allInstances.put(createdInstance, null);
			}

			if(createdBO == null) {
				return;
			}

			if(clazz.isAssignableFrom(createdBO.getInstance().getClass())  ) {
				bos.add(createdBO);
				if(createdBO.getInstance() != createdInstance) {
					skippedInstances.add(createdInstance);
				}
			}
		}

		@Override public void performCall (CallInfo ci)
		{

		}

		@Override public void investigate (Object object)
		{

			List<String> keys = new ArrayList<>();
			System.out.println("No of skipped instances: " + skippedInstances.size());
			for(BusinessObject bo: bos) {
				keys.add(bo.get(propertyName).toString());
				if(allInstances.containsKey(bo.getInstance())) {
					allInstances.remove(bo.getInstance());
				}
			}
			System.out.println("No of extra instances: " + allInstances.size());

			Collections.sort(keys);

			for(String key: keys) {
				System.out.println("Key: " + key);
			}
		}
	}

	public static class DuplicateUniqueKeyByCall implements Detector {
		private Class clazz;
		private String propertyName;

		private Map<Object, List<Object>> duplicateInstances;
		private Map<Object, List<BusinessObject>> duplicateBusinessObjects;

		public DuplicateUniqueKeyByCall(Class clazz, String propertyName) {
			this.clazz = clazz;
			this.propertyName = propertyName;
			this.duplicateInstances = new HashMap<>();
			this.duplicateBusinessObjects = new HashMap<>();
		}

		@Override public void notifyCreate (Object source, BusinessObject createdBO, Object instance)
		{

		}

		/**
		 * Method should be invoked after the property has been set.
		 * @param ci details about the call
		 */
		@Override
		public void performCall(CallInfo ci) {

			if(ci.getStage() != ProcessingStage.UPDATE) {
				return;
			}

			if(ci.getParent() != null && ci.getParent().getOutput() instanceof BusinessObject) {
				if(ci.getInputProperty() != null && ci.getInputProperty().getName().equals(propertyName)) {
					BusinessObject outputBO = (BusinessObject)ci.getParent().getOutput();
					if(clazz.isAssignableFrom(outputBO.getType().getInstanceClass()) ) {
						Object keyValue = outputBO.get(propertyName);
						List<BusinessObject> bos = duplicateBusinessObjects.get(keyValue);
						if(bos == null) {
							bos = new ArrayList<>();
						}

						// Add only if the object is different from the others
						boolean existing = false;
						for(BusinessObject bo: bos) {
							if(bo == outputBO) {
								existing = true;
								break;
							}
						}
						if(!existing) {
							bos.add(outputBO);
						}

						// Check the actual instances to see if there are duplicates
						List<Object> instances = duplicateInstances.get(keyValue);
						if(instances == null) {
							instances = new ArrayList<>();
						}
						existing = false;
						for(Object instance: instances) {
							if(instance == outputBO.getInstance()) {
								existing = true;
								break;
							}
						}
						if(!existing) {
							instances.add(outputBO.getInstance());
						}
					}
				}
			}
		}

		@Override public void investigate (Object object)
		{
			Set<Object> dupInstanceKeys = new HashSet<>();
			Set<Object> dupBOKeys = new HashSet<>();

			// check for duplicate instances
			for(Map.Entry<Object, List<Object>> entry: duplicateInstances.entrySet()) {
				if(entry.getValue().size() > 1) {
					dupInstanceKeys.add(entry.getKey());
				}
			}

			for(Map.Entry<Object, List<BusinessObject>> entry: duplicateBusinessObjects.entrySet()) {
				if(entry.getValue().size() > 1) {
					dupBOKeys.add(entry.getKey());
				}
			}

			System.out.println("Keys of duplicate instances:");
			for(Object key: dupInstanceKeys) {
				System.out.println(key);
			}
			System.out.println("Keys of duplicate BusinessObjects:");
			for(Object key: dupBOKeys) {
				System.out.println(key);
			}
		}
	}

	public static class DuplicateUniqueKey implements Detector {

		private Class clazz;
		private String propertyName;
		private Map<Object, EntityKey> examined;
		private Map<Object, List<EntityKey>> duplicateKeys;
		private Map<Object, Set<BusinessObject>> duplicateObjects;

		public DuplicateUniqueKey(Class clazz, String propertyName) {
			this.clazz = clazz;
			this.propertyName = propertyName;
			this.examined = new HashMap<>();
			this.duplicateKeys = new HashMap<>();
			this.duplicateObjects = new HashMap<>();
		}

		@Override public void notifyCreate (Object source, BusinessObject createdBO, Object instance)
		{

		}

		@Override public void performCall (CallInfo ci)
		{
			// no functionality in this detector
		}

		@Override public void investigate (Object object)
		{
			if(! (object instanceof ObjectCreator)) {
				return;
			}

			ObjectCreator oc = (ObjectCreator) object;
			for(Map.Entry<EntityKey, BusinessObject> entry: oc.entitiesByKey.entrySet()) {
				if(clazz.isAssignableFrom(entry.getValue().getInstance().getClass()) ) {
					Object obj = entry.getValue().get(propertyName);
					EntityKey entityKey = entry.getKey();
					BusinessObject entity = entry.getValue();

					if(entity instanceof MutableBO && ((MutableBO)entity).isEvicted()) {
						continue;
					}

					if(examined.containsKey(obj)) {
						duplicateKeys.get(obj).add(entityKey);
						duplicateObjects.get(obj).add(entity);
					} else {
						examined.put(obj, entry.getKey());
						List<EntityKey> values = new ArrayList<EntityKey>();
						values.add(entityKey);
						duplicateKeys.put(obj, values);
						Set<BusinessObject> distinctObjects = new HashSet<>();
						distinctObjects.add(entity);
						duplicateObjects.put(obj, distinctObjects);
					}
				}
			}

			// Retain only those keys which has different objects for the same key
			for(Object key: duplicateKeys.keySet()) {
				if(duplicateObjects.containsKey(key) && duplicateObjects.get(key).size() == 1) {
					duplicateObjects.remove(key);
				}
			}

			if(duplicateObjects.size() > 0) {
				System.out.println("Duplicate objects found");
			}
		}
	}
}
