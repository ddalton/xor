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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.operation.DenormalizedQueryOperation;
import tools.xor.operation.QueryOperation;
import tools.xor.operation.ReadOperation;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.Edge;
import tools.xor.util.ObjectCreator;
import tools.xor.util.State;
import tools.xor.util.graph.TypeGraph;
import tools.xor.view.QueryFragment;
import tools.xor.view.QueryTree;
import tools.xor.view.QueryTreeInvocation;

public abstract class AbstractBO implements BusinessObject {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final long serialVersionUID = 1L;
	
	public static final String CURRENT = ".";

	// SDO Path related constants
    // This delimiter is used when entity name for downcast properties need to be provided
	// since the entity name usually contains Settings.PATH_DELIMITER, we use this instead
	public static final String SDO_PATH_DELIMITER = "/";
	public static final String INDEX_FROM_0   = ".";
	public static final String INDEX_START    = "[";
	public static final String INDEX_END      = "]";	
	public static final String PATH_CONTAINER = "..";
	public static final String ATTR_DELIMITER = "=";	
	public static final String INSERT_OPERATOR = "+";  // insert at index for a set operation. Returns the element at index for a get operation.
	public static final String APPEND_OPERATOR = "<<"; // append at end of list for a set operation. Returns the last element in a GET operation.

	protected Object               instance;            // The object containing the actual data. This can be a JPA/Hibernate/Javabean object.
	protected Type                 type;                // Represents the type of the entity, that holds the properties that make up this entity
	protected Type                 propertyType;        // If the instance is an object of a subtype, then type will hold the actual subtype and propertyType will hold
														// the type of the property referencing this object
	protected boolean              persistent;          // This flag is set to true, if the object was created from the persistence store
	protected boolean              modified;            // This flag is set to true, if any fields or collections have been modified
	protected ObjectCreator        objectCreator;       // Cache to hold objects. Useful to set multiple references to an object and providing repeatable read capability. object.

	// TODO: Move to ObjectGraph
	protected DataObject      container;                // A reference to owning object of this entity in an aggregate
	protected Property        containmentProperty;      // Represents the property referenced from the owning entity to this entity
	protected boolean         visited;                  // Used to prevent loops when traversing the graph	
	
	// TODO: Move to ObjectCreator
	protected ObjectPersister objectPersister;          // Mechanism used to resolve dependencies and property order persistence related actions without causing data integrity conflicts.

	
	@Override
	public boolean isPersistent() {
		return this.persistent;
	}
	
	@Override
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}
	
	@Override
	public boolean isDependent() {
		return getContainmentProperty() != null && getContainmentProperty().isContainment();
	}

	@Override
	public Type getPropertyType() {
		return this.propertyType;
	}

	@Override
	public ObjectPersister getObjectPersister() {
		return objectPersister;
	}

	public AbstractBO(Type type, DataObject container, Property containmentProperty, ObjectCreator objectCreator) {
		this.type                = type;
		this.container           = container;
		this.containmentProperty = containmentProperty;
		this.propertyType = type;

		// Can be null - then the business object is light weight
		// as it will not "bloat" the object creator
		this.objectCreator       = objectCreator;
	}

	@Override
	public boolean isRoot() {
		return this == getRootObject();
	}

	@Override
	public boolean isVisited() {
		return visited;
	}

	@Override
	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	
	/**
	 * This is also used in the External Business Object for performance reasons
	 * to walk only those paths that have been modified
	 */
	@Override
	public boolean isModified() {
		return modified;
	}

	@Override
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	@Override
	public EntityKey getSurrogateKey () {
		return getSurrogateKey(null);
	}

	@Override
	public List<EntityKey> getNaturalKey() {
		return getNaturalKey(null);
	}

	@Override
	public List<EntityKey> getNaturalKey(String anchor) {
		return getObjectCreator().getTypeMapper().getNaturalKey(this, anchor);
	}

	@Override
	public EntityKey getSurrogateKey (String anchor) {
		return getObjectCreator().getTypeMapper().getSurrogateKey(getIdentifierValue(), getType(), anchor);
	}

	@Override
	public void removeEntity(BusinessObject entity, String anchor) {
		/*
		EntityKey entityKey = getObjectCreator().getTypeMapper().getEntityKey(
			entity.getIdentifierValue(),
			entity);
		getObjectCreator().removeByEntityKey(entityKey);
		*/

		EntityKey surrogateKey = getObjectCreator().getTypeMapper().getSurrogateKey(entity.getIdentifierValue(), entity.getType(), anchor);
		getObjectCreator().removeByEntityKey(surrogateKey);

		List<EntityKey> naturalKeys = getObjectCreator().getTypeMapper().getNaturalKey(entity, anchor);
		for(EntityKey naturalKey: naturalKeys) {
			getObjectCreator().removeByEntityKey(naturalKey);
		}
	}

	/**
	 * Check if the existingEntity is a valid for the given naturalKey
	 * @param naturalKey derived from newEntity
	 * @param newEntity used to check the existence of an entity based on its natural keys
	 * @param existingEntity if any already registered
	 * @param anchor state tree path that determines the shape of the entity
	 * @return true if an existingEntity is found with the same natural key
	 */
	private boolean isValidEntity(EntityKey naturalKey, BusinessObject newEntity, BusinessObject existingEntity, String anchor) {
		boolean result = true;

		if (existingEntity != newEntity) {
			// The natural field values could have changed during the creation/clone/update
			// operations for the existingEntity. So check its Natural Key.
			List<EntityKey> existingNK = existingEntity.getNaturalKey();
			if (existingNK == null || !existingNK.contains(naturalKey)) {
				// Remove the incorrect natural key mapping to the existing BO
				getObjectCreator().removeByEntityKey(naturalKey);

				// Update the existing BO with the correct natural key mapping
				if (existingNK != null && existingEntity != null) {
					getObjectCreator().addByNaturalKey(existingEntity, anchor);
				}

				result = false;
			}
		}

		return result;
	}

	@Override
	public BusinessObject getEntity (BusinessObject entity, String anchor)
	{
		BusinessObject existingEntity = null;

		// First retrieve by Natural Key
		List<EntityKey> naturalKeys = entity.getNaturalKey(anchor);
		for (EntityKey naturalKey: naturalKeys) {
			existingEntity = getObjectCreator().getByEntityKey(naturalKey, entity.getType());
			if(existingEntity != null && isValidEntity(naturalKey, entity, existingEntity, anchor)) {
				break;
			}
		}

		// try using the surrogate key
		if (existingEntity == null) {
			EntityKey entityKey = entity.getSurrogateKey(anchor);
			if (entityKey != null) {
				existingEntity = getObjectCreator().getByEntityKey(entityKey, entity.getType());
				if(existingEntity != null) {
					EntityKey existingSK = existingEntity.getSurrogateKey(anchor);

					// If for some reason the id has changed
					if (existingSK == null || !existingSK.equals(entityKey)) {
						getObjectCreator().addBySurrogateKey(existingEntity, anchor);

						// The existing BO is mapped to the incorrect surrogate key
						existingEntity = null;
					}

					// make sure it is found by its natural key
					getObjectCreator().addByNaturalKey(existingEntity, anchor);
				}
			}
		}

		return existingEntity;
	}
	
	@Override
	public BusinessObject getBySurrogateKey(Object id, Type type) {
		return getObjectCreator().getByEntityKey(new SurrogateEntityKey(id, AbstractTypeMapper.getSurrogateKeyTypeName(type)), type);
	}

	@Override
	public BusinessObject getBySurrogateKey(Object id, Type type, String path) {
		return getObjectCreator().getByEntityKey(new SurrogateEntityKey(id, AbstractTypeMapper.getSurrogateKeyTypeName(type), path), type);
	}

	@Override
	public BusinessObject getByNaturalKey(Map<String, Object> naturalKeyValues, Type type) {
		return getObjectCreator().getByEntityKey(
			new NaturalEntityKey(
				naturalKeyValues,
				AbstractTypeMapper.getNaturalKeyTypeName(type)), type);
	}

	@Override
	public BusinessObject getByNaturalKey(Map<String, Object> naturalKeyValues, Type type, String path) {
		return getObjectCreator().getByEntityKey(
			new NaturalEntityKey(
				naturalKeyValues,
				AbstractTypeMapper.getNaturalKeyTypeName(type),
				path), type);
	}

	private Map<String, Object> getKeyValue(Set<String> keys) {
		Map<String, Object> keyValues = new HashMap<String, Object>();
		for(String key: keys) {
			keyValues.put(key, get(key));
		}
		return keyValues;
	}	

	@Override
	/**
	 * Invoked on the Collection Element data object
	 */
	public Object getCollectionElementKey(Property property) {

		Set<String> collectionKey = ((ExtendedProperty)property).getCollectionKey();
		if( collectionKey != null ) {
			return getKeyValue(collectionKey);
		} else if ( ((ExtendedProperty)property).getPositionProperty() != null) {
			return this.get(((ExtendedProperty)property).getPositionProperty());
		} else { // fallback to id
			/* NOTE: the passed in property may be of a subtype or a sibling type of the
		     *  actual collection element object, so we cannot use the property type's identifier property
		     */
			if(! (getType() instanceof EntityType)) {
				throw new RuntimeException("Unable to obtain a key for the collection element, please define a collection key for: " + getType().getName());
			}
			Property identifier = ((EntityType)getType()).getIdentifierProperty();
			return (this.get(identifier) == null) ? null : this.get(identifier).toString();
		}
	}

	@Override
	public String getInstanceClassName ()
	{
		if(instance == null) {
			return null;
		}

		if (instance.getClass() != JSONObject.class || !((JSONObject)instance).has(Constants.XOR.TYPE)) {
			if (instance.getClass() != JSONObject.class) {
				return ClassUtil.getUnEnhanced(instance.getClass()).getName();
			}
			else {
				// Cannot get the specific type, so return the interface type
				return getType().getName();
			}
		}
		else {
			return ((JSONObject)instance).getString(Constants.XOR.TYPE);
		}
	}
	
	@Override
	public String getOpenProperty(String propertyName) {
		if(instance instanceof JSONObject) {
			if(((JSONObject)instance).has(propertyName)) {
				return ((JSONObject)instance).getString(propertyName);
			}
		}
		
		return null;
	}

	@Override
	public Object getInstance() {
		return instance;
	}
	
	@Override
	public Object getNormalizedInstance(Settings settings) {
		return objectCreator.getCreationStrategy().getNormalizedInstance(this, settings);
	}	

	@Override
	public void setInstance(Object instance) {
		if(this.instance == instance)
			return;
		
		if(instance != null && this.getContainmentProperty() != null) {
			ExtendedProperty property = (ExtendedProperty) this.getContainmentProperty();
			
			// We skip check for dynamic types
			if(!property.getType().isOpen() && !objectCreator.getTypeMapper().isOpen(instance.getClass())) {
				if( (property.isSet() && !Set.class.isAssignableFrom(instance.getClass())) ||
						(property.isList() && !List.class.isAssignableFrom(instance.getClass())) ||
						(property.isMap() && !Map.class.isAssignableFrom(instance.getClass())) ||
						( !property.isSet() && !property.isList() && !property.isMap() && !type.getInstanceClass().isAssignableFrom(instance.getClass())) ) {
					logger.error("Instance class and type conflict: Instance class: " 
							+ instance.getClass().getName() + ", type: " 
							+ getType().getName() + ", containment property type: " 
							+ this.getContainmentProperty().getType().getName() + ", containment property class: "
							+ this.getContainmentProperty().getType().getInstanceClass().getName() + ", containment property name: "
							+ this.getContainmentProperty().getName());

					throw new IllegalArgumentException("The DataObject instance class " + instance.getClass().getName() 
							+ " is not compatible with its type " + property.getType().getName()
							+ " property: " + property.getName()
							+ " type class: " + property.getType().getInstanceClass().getName()
							+ " property class: " + property.getClass().getName()
							+ " type class: " + property.getType().getClass().getName());
				}
			}
		}

		if(BusinessObject.class.isAssignableFrom(instance.getClass()))
			throw new RuntimeException("Cannot assign a data object as an instance");
		
		Object oldInstance = this.instance;
		this.instance = instance;

		if(objectCreator != null) {
			objectCreator.updateInstance(this, oldInstance);
		}
	}

	private boolean isIndexOperation(String indexStr) {
		if(indexStr.trim().endsWith(INSERT_OPERATOR) || indexStr.trim().equals(APPEND_OPERATOR))
			return true;

		return false;
	}

	private Object getIndexObject(String indexStr, String propertyName) {
		if(indexStr.trim().endsWith(INSERT_OPERATOR)) {
			int index = Integer.parseInt(indexStr.substring(0, indexStr.indexOf(INSERT_OPERATOR)).trim() );
			return ((List<?>)get(getType().getProperty(propertyName))).get(index);
		} else if(indexStr.trim().equals(APPEND_OPERATOR)) {
			List<?> listObj = (List<?>)get(getType().getProperty(propertyName));
			return listObj.get(listObj.size()-1);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void setIndexObject(String indexStr, String propertyName, Object value) {
		List<Object> valueList = (List<Object>) get(propertyName);		
		if(indexStr.trim().endsWith(INSERT_OPERATOR)) {
			int index = Integer.parseInt(indexStr.substring(0, indexStr.indexOf(INSERT_OPERATOR)).trim() );
			valueList.add(index, value);			

		} else if(indexStr.trim().equals(APPEND_OPERATOR))
			valueList.add(value);
	}	

	/**
	 *  SDO Path expression. Enhancements made for XOR.
	 * 
	 *   path ::= (scheme ':')? '/'? (step '/')*   step
	 *   scheme ::= [^:]+
	 *   step ::= '@'? property
	 *          | property '[' index_from_1 ']'
	 *          | property '.' index_from_0
	 *          | reference '[' attribute '=' value ']'
	 *          | ".."
	 *   property ::= NCName ;; may be simple or complex type
	 *   attribute ::= NCName ;; must be simple type
	 *   reference :: NCName ;; must be DataObject type
	 *   index_from_0 ::= Digits ('+')? | "<<"
	 *   index_from_1 ::= NotZero (Digits)? ('+')? | "<<"
	 *   value ::= Literal
	 *          | Number
	 *          | Boolean
	 *   Literal ::= '"' [^"]* '"'
	 *          | "'" [^']*   "'"
	 *   Number ::= Digits ('.' Digits?)?
	 *          | '.' Digits
	 *   Boolean ::= true
	 *          | false
	 *   NotZero ::= [1-9]
	 *   Digits ::= [0-9]+
	 *   
	 *   ;; leading '/' begins at the root
	 *   ;; ".." is the containing DataObject, using containment properties
	 *   ;; Only the last step have an attribute as the property
	 *   
	 *   The scheme is an extension mechanism for supporting additional path expressions 
	 *   in the future. No schema and a scheme of "sdo:" are equivalent, representing this syntax.
	 *   
	 *   If more than one property shares the same name, only the first is matched by the path expression, 
	 *   using property.name for name matching. If there are alias names assigned,
	 *   those are also used to match. Also, names including any of the special characters of the
	 *   syntax (./[]='"@) are not accessible. Each step of the path before the last must return a
	 *   single DataObject. When the property is a Sequence, the values returned are those of the
	 *   getValue() accessor.
	 *   
	 * 
	 * @param path
	 * @return value at path
	 */
	private Object getPathObject(String path) {

		if(path == null || "".equals(path))
			throw new IllegalArgumentException("Path cannot be empty or null");

		if(path.equals(PATH_CONTAINER))
			return getContainer();

		if(path.equals(SDO_PATH_DELIMITER)) { // root
			DataObject result = this;
			Map loopDetector = new IdentityHashMap<>();
			loopDetector.put(this, null);
			while(result.getContainer() != null) {
				result = result.getContainer();
				if(loopDetector.containsKey(result)) {
					throw new RuntimeException("Container loop detected - check if id is getting overwritten");
				}
				loopDetector.put(result, null);
			}
			return result;
		}

		// multiple components
		if(path.contains(SDO_PATH_DELIMITER)) {
			if(path.startsWith(SDO_PATH_DELIMITER)) {
				return ((DataObject)getPathObject("/")).get(path.substring(path.indexOf(
						SDO_PATH_DELIMITER)+1));
			} else {
				String firstComponent = path.substring(0, path.indexOf(SDO_PATH_DELIMITER));
				String remainingPath = path.substring(path.indexOf(SDO_PATH_DELIMITER)+1);
				
				// Get the property name related to the first path component
				Object anchor = getPathObject(firstComponent);
				if(anchor != null) {
					BusinessObject anchorBO = objectCreator.getExistingDataObject(anchor);
					if(anchorBO != null) {
						return anchorBO.get(remainingPath);
					} else {
						// This can happen when the object has just been created
						// and the natural keys are not yet processed.
						return null;
					}
				} else {
					return null;
				}
			}
		}

		// single component
		// List element indexed from 0
		if(path.contains(INDEX_FROM_0)) {
			String propertyName = path.substring(0, path.indexOf(INDEX_FROM_0));
			String indexStr = path.substring(path.indexOf(INDEX_FROM_0)+1);
			try {
				int index = Integer.parseInt(indexStr);
				return ((List<?>)get(getType().getProperty(propertyName))).get(index);
			} catch (NumberFormatException e) {
				if(isIndexOperation(indexStr))
					return getIndexObject(indexStr, propertyName);

				// Map collection
				Property property = getType().getProperty(propertyName);
				if(Map.class.isAssignableFrom(property.getType().getInstanceClass())) {
					Map<?, ?> values = (Map<?, ?>) get(property);
					return values.get(indexStr);					
				}
			}
		}

		// List element indexed from 1
		if(path.contains(INDEX_START)) {
			String propertyName = path.substring(0, path.indexOf(INDEX_START));
			String indexStr = path.substring(path.indexOf(INDEX_START)+1, path.indexOf(INDEX_END));

			try {
				int index = Integer.parseInt(indexStr);
				return ((List<?>)getPathObject(propertyName)).get(index-1);
			} catch (NumberFormatException e) {
				if(isIndexOperation(indexStr))
					return getIndexObject(indexStr, propertyName);

				// List element indexed by attribute value
				return getByPropertyFilter(path);
			}
		}		

		return get(getType().getProperty(path));
	}

	@SuppressWarnings("unchecked")
	protected Object getByPropertyFilter(String path) {

		String propertyName = path.substring(0, path.indexOf(INDEX_START));
		String indexStr = path.substring(path.indexOf(INDEX_START)+1, path.indexOf(INDEX_END));

		if( ((ExtendedProperty)containmentProperty).isMap() && indexStr.indexOf(ATTR_DELIMITER) == -1) {
			Map<?, ?> values = (Map<?, ?>) instance;
			Object valueInstance = values.get(indexStr);
			return objectCreator.createDataObject(valueInstance, ((ExtendedProperty)containmentProperty).getElementType(), (BusinessObject) container, containmentProperty);
		}

		// TODO: Check path validity 

		if(((ExtendedProperty)containmentProperty).getElementType().isDataType())
			throw new IllegalArgumentException("The property '" + propertyName + "' refers to a non-DataObject collection");

		if(!Collection.class.isAssignableFrom(containmentProperty.getType().getInstanceClass()))	
			throw new IllegalStateException("Expecting a collection instance");

		Collection<DataObject> values = (Collection<DataObject>) getList(containmentProperty);

		String attributeName = indexStr.substring(0, indexStr.indexOf(ATTR_DELIMITER));
		Property attributeProperty = ((ExtendedProperty)containmentProperty).getElementType().getProperty(attributeName);
		if(!attributeProperty.getType().isDataType())
			throw new IllegalArgumentException("Attribute '" + attributeName + "' cannot have a value of type DataObject");

		String attributeValue = indexStr.substring(indexStr.indexOf(ATTR_DELIMITER)+1);
		attributeValue = attributeValue.replaceAll("^\"|\"$|^'|'$", "");

		// TODO: ensure the string representation is according to the SDO spec

		// Retrieve by identifier if possible
		if( ((EntityType)((ExtendedProperty)containmentProperty).getElementType()).getIdentifierProperty() == attributeProperty ) {
			return getBySurrogateKey(attributeValue, ((ExtendedProperty)containmentProperty).getElementType());
		} else {
			for(DataObject dataObject: values) {
				if(dataObject.get(attributeProperty).toString().equals(attributeValue))
					return dataObject;
			}		
		}

		return null;
	}

	@Override
	public String getString(String path) {
		ExtendedProperty ep = (ExtendedProperty)getType().getProperty(path);
		if(ep == null) {
			// This can be null if the path represents a built-in property like XOR. or
			// |XOR|
			return null;
		}

		// Refers to an embedded object
		if(path.contains(Settings.PATH_DELIMITER)) {
			if (!(instance instanceof JSONObject)) {
				throw new RuntimeException(
					"The getString() API for embedded objects is intended for use only with JSONObject instances. See get() method for other objects types");
			}
			JSONObject embedded = (JSONObject)instance;
			while(path.contains(Settings.PATH_DELIMITER)) {
				String componentAttribute = path.substring(0, path.indexOf(Settings.PATH_DELIMITER));
				path = path.substring(path.indexOf(Settings.PATH_DELIMITER) + 1);
				if(!embedded.has(componentAttribute)) {
					return null;
				}
				embedded = embedded.getJSONObject(componentAttribute);
			}
			if(!embedded.has(path)) {
				return null;
			}
			Object value = embedded.get(path);
			if(value instanceof String) {
				return embedded.getString(path);
			} else {
				return value == null ? null : value.toString();
			}
		}
		else {
			return ep.getStringValue(this);
		}
	}

	@Override
	public Object get(String path) {
		if(path.indexOf(Settings.PATH_DELIMITER) != -1) {
			return getPathObject(Settings.convertToBOPath(path));
		} else {
			return getPathObject(path);
		}
	}

	protected DataObject getDeepestContainer(String path) {
		if(path == null || "".equals(path))
			throw new IllegalArgumentException("Path cannot be empty or null");

		if(path.equals(PATH_CONTAINER) || path.equals(SDO_PATH_DELIMITER))
			throw new IllegalArgumentException("Path should refer to a property");

		// Get the container
		AbstractBO container = this;
		if(path.contains(SDO_PATH_DELIMITER)) { // find the correct container
			String containerPath = path.substring(0, path.lastIndexOf(SDO_PATH_DELIMITER));
			container = (AbstractBO) getPathObject(containerPath);
		}	

		return container;
	}
	
	@Override
	public BusinessObject createDataObject(Object id, Type instanceType, Property property, String anchor) throws Exception {
		return this.createDataObject(id, null, instanceType, property, anchor);
	}

	public BusinessObject createDataObject(Object id, Map<String, Object> naturalKeyValues, Type instanceType, Property property) throws Exception {
		return createDataObject(id, naturalKeyValues, instanceType, property, null);
	}

	public BusinessObject createDataObject(Object id, Map<String, Object> naturalKeyValues, Type instanceType, Property property, String anchor) throws Exception {
		Object propertyInstance = createInstance(objectCreator, id, naturalKeyValues, instanceType, false, anchor);
		BusinessObject result = objectCreator.createDataObject(propertyInstance, instanceType, property == null ? null : this, property, anchor);

		if(property != null)
			((ExtendedProperty)property).setValue(this, propertyInstance);

		return result;
	}
	
	public static Object createInstance(ObjectCreator oc, Object id, Type instanceType) throws Exception {
		return createInstance(oc, id, null, instanceType, false, null);
	}

	public static Object createInstance(ObjectCreator oc, Object id, Map<String, Object> naturalKeyValues, Type instanceType) throws Exception {
		return createInstance(oc, id, naturalKeyValues, instanceType, false, null);
	}

	/**
	 * Create a java object based on the Object creator.
	 *
	 * @param oc used to create the java object
	 * @param id of the java object
	 * @param naturalKeyValues business key
	 * @param instanceType type of the object
	 * @param isPatch true if the object is being created solely for the purpose of patching. This is an update optimization.
	 * @param anchor needed to find objects if the natural key points to entities
	 * @return new domain instance
	 * @throws Exception during object creation
	 */
	public static Object createInstance(ObjectCreator oc, Object id, Map<String, Object> naturalKeyValues, Type instanceType, boolean isPatch, String anchor) throws Exception {
		Object propertyInstance = null;
		
		// We will use the already loaded object in session if one is available
		// This is done only for the query operation that retrieves managed objects
		if(instanceType == null)
			logger.error("!!!!! instanceType is null for object id: " + id);
		if(oc.isReadOnly() && oc.getTypeMapper().isDomain(instanceType.getName()) && oc.getTypeMapper().isPersistenceManaged() && !instanceType.isDataType()) {
			propertyInstance = oc.getDataStore().getCached(instanceType.getInstanceClass(), id);
			
			if(propertyInstance != null) {
				logger.info("Found in cache for type: " + instanceType.getInstanceClass().getName() + " and id: " + id.toString());
			}
		}
		
		if(propertyInstance == null) {
			if(isPatch) {
				propertyInstance = oc.patchInstance((EntityType)instanceType);
			} else {
				propertyInstance = oc.createInstance(instanceType);
			}

			if(!instanceType.isDataType()) {
				createInstance(
					oc,
					(EntityType)instanceType,
					propertyInstance,
					id,
					naturalKeyValues,
					isPatch,
					anchor);
			}
		}
		
		return propertyInstance;
	}

	private static void createInstance(ObjectCreator oc, EntityType instanceType, Object instance, Object id, Map<String, Object> naturalKeyValues, boolean isPatch, String anchor) throws
		Exception
	{
		// For entity objects we also set the surrogate/natural key values
		EntityType entityType = instanceType;
		if(entityType.getIdentifierProperty() != null ) {
			((ExtendedProperty)entityType.getIdentifierProperty()).setValue(oc.getSettings(), instance, id);
		}

		if(entityType.getNaturalKey() != null && naturalKeyValues != null && naturalKeyValues.size() > 0) {

			for(String key: entityType.getNaturalKey()) {
				Type keyType = entityType.getProperty(key).getType();
				if(!keyType.isDataType()) {
					// First try to find entity using ObjectCreator#findEntity
					// If the entity cannot be found then we create and set the instance
					// Set the natural key if there is one
					Object idValue = null;
					Map<String, Object> childNKV = new HashMap<>();
					String prefix = key + Settings.PATH_DELIMITER;
					if(((EntityType)keyType).getIdentifierProperty() != null) {
						String idKey = prefix + ((EntityType)keyType).getIdentifierProperty().getName();
						idValue = naturalKeyValues.get(idKey);
					} else if (((EntityType)keyType).getNaturalKey() != null) {
						for(Map.Entry<String, Object> entry: naturalKeyValues.entrySet()) {
							if(entry.getKey().startsWith(prefix)) {
								childNKV.put(entry.getKey().substring(prefix.length()), entry.getValue());
							}
						}
					}
					String childAnchor = anchor + Settings.PATH_DELIMITER + key;
					BusinessObject childBO = oc.findEntity(idValue, childNKV, keyType, getAnchor(childAnchor));
					Object keyInstance = childBO != null ? childBO.getInstance() : createInstance(oc, idValue, childNKV, keyType, isPatch, getAnchor(childAnchor));
					((ExtendedProperty)entityType.getProperty(key)).setValue(oc.getSettings(), instance, keyInstance);

					// recurse
					createInstance(
						oc,
						(EntityType)keyType,
						keyInstance,
						idValue,
						childNKV,
						isPatch,
						getAnchor(anchor + Settings.PATH_DELIMITER + key));
				} else {
					((ExtendedProperty)entityType.getProperty(key)).setValue(oc.getSettings(), instance, naturalKeyValues.get(key));
				}
			}
		}
	}

	public BusinessObject createDataObject(Object id, Type instanceType, String anchor) throws Exception {
		Object propertyInstance = createInstance(objectCreator, id, instanceType);
		BusinessObject result = objectCreator.createDataObject(propertyInstance, instanceType, null, null, anchor);

		if(getSettings().getDetector() != null) {
			getSettings().getDetector().notifyCreate(id, result, propertyInstance);
		}

		return result;
	}

	@Override
	public BusinessObject createDataObject(Object id, Type instanceType) throws Exception {
		return createDataObject(id, instanceType, null);
	}

	// Anchor uniquely identifies the shape of the object at the end of the anchor
	// Collection objects are always considered as distinct shapes
	private void addToAnchor(StringBuilder anchor, String step, Property property, Object collection) {

		if(anchor.length() > 0) {
			anchor.append(Settings.PATH_DELIMITER);
		}

		if(!property.isMany()) {
			anchor.append(step);
		} else {
			// Each collection is considered to have a distinct shape
			// as it can differ in the element list
			anchor.append(System.identityHashCode(collection));
		}
	}

	@Override
	public void reconstitute (String fullPropertyPath,
							  Map<String, Object> propertyResult,
							  QueryTree queryTree,
							  ReconstituteRecordVisitor visitor,
							  QueryTreeInvocation qti) throws Exception {

		// If we are setting a null value then nothing needs to be done
		if( propertyResult.get(fullPropertyPath) == null) {
			return;
		}

		// adjust for ancestor path in child queries
		String propertyPath = queryTree.makeRelative(fullPropertyPath);
		String anchorPath = ((QueryFragment)queryTree.getRoot()).getAnchorPath();

		// Since this builds the path for objects already persisted, the identifier value will not be null
		String[] pathSteps = propertyPath.split(Settings.PATH_DELIMITER_REGEX);
		BusinessObject current = this;
		StringBuilder currentPath = new StringBuilder("");
		StringBuilder anchor = new StringBuilder("");

		for(String step: pathSteps) {
			if(currentPath.length() > 0) {
				currentPath.append(Settings.PATH_DELIMITER);
			}
			currentPath.append(step);
			Property property = current.getInstanceProperty(step);

			//Property domainProperty = domainEntityType.getProperty(currentPath.toString());
			Property domainProperty = queryTree.getProperty(currentPath.toString());

			if(property == null) {
				if(getType().isOpen()) {
					QueryTree.FragmentAnchor anchorFragment = queryTree.findFragment(currentPath.toString());
					if (anchorFragment != null) {
						EntityType type = anchorFragment.fragment.getEntityType();
						// We need the external type
						type = (EntityType)type.getShape().getType(type.getEntityName());
						if(type.isSubtypeOf((EntityType)current.getType())) {
							current.downcast(type);
							property = current.getInstanceProperty(step);
						}
					}

					// This is not driven by meta-data
					if(property == null) {
						((EntityType)getType()).setOpenProperty(
							this.getInstance(),
							propertyPath,
							propertyResult.get(fullPropertyPath));
						return;
					}

				} else {
					throw new RuntimeException("Unable to resolve property: " + propertyPath);
				}
			}

			addToAnchor(anchor, step, property, current.getInstance());

			//Object propertyDO = current.get(property);
			Object propertyDO = current.getDataObject(property);
			if(!property.isMany()) {

				if(property.getType().isDataType() ) {
					// Populate the field and return the data object
					((ExtendedProperty)property).setValue(current, propertyResult.get(fullPropertyPath));
					return;
				}

				if( ((EntityType)property.getType()).isEmbedded() ) {
					// TODO: What about collection of embedded objects?
					continue; // Embedded objects are automatically created as part of its lifecycle owner
				}

				if(propertyDO == null) {
					// Set the natural key if there is one
					Map<String, Object> naturalKeyValues = new HashMap<String, Object>();
					if(((EntityType)property.getType()).getNaturalKey() != null) {
						Collection<String> naturalKey = ((EntityType)property.getType()).getExpandedNaturalKey();
						if(naturalKey != null) {
							for(String key: naturalKey) {
								Object keyValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + key);
								naturalKeyValues.put(key, keyValue);
							}
						}				
					}
					// Check if there is a data object with the given key
					// Get the identifier value
					Object idValue = null;
					if(((EntityType)property.getType()).getIdentifierProperty() != null) {
						idValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + ((EntityType)property.getType()).getIdentifierProperty().getName());
					}
					//propertyDO = getObjectCreator().findEntity(idValue, naturalKeyValues, domainProperty.getType(), getAnchor(anchor.toString()));
					propertyDO = findQueryObject(fullPropertyPath, idValue, domainProperty.getType(), qti);

					if(propertyDO == null) { // create and set the instance object
						// check if we are narrowing
						String narrowToType = (String) propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + QueryFragment.ENTITY_TYPE_ATTRIBUTE);
						EntityType objectType = (EntityType) property.getType();
						if(narrowToType != null)
							objectType = (EntityType) getObjectCreator().getDataModel().getShape().getType(
								narrowToType);

						propertyDO = createQueryObject(null, current, fullPropertyPath, idValue, naturalKeyValues, objectType, property, getAnchor(anchor.toString()), qti);
						//propertyDO = current.createDataObject(idValue, naturalKeyValues, objectType, property, getAnchor(anchor.toString()));
					}
				}
				if(property.isContainment()) {
					((BusinessObject)propertyDO).setContainer(current);
					((BusinessObject)propertyDO).setContainmentProperty(property);
				}

				// Set the instance value in the container
				((ExtendedProperty)property).setValue(current, ((BusinessObject)propertyDO).getInstance());

				current = (BusinessObject) propertyDO;		
			} else  {

				//System.out.println("propertyDO class: " + propertyDO.getClass() + ", property: " + property.getName());
				propertyDO = createQueryObject(propertyDO, current, fullPropertyPath, null, null, property.getType(), property, getAnchor(anchor.toString()), qti);

				((ExtendedProperty)property).setValue(current, ((BusinessObject)propertyDO).getInstance());
				current = (BusinessObject) propertyDO;
				Object elementDO = null;

				// Get the identifier value from the collection element
				Type elementType = ((ExtendedProperty)property).getElementType();
				Type domainElementType = ((ExtendedProperty)domainProperty).getElementType();
				
				// Is this a collection of simple values
                if(elementType.isDataType() ) {
                    // Currently supported only for JSON types
                    if(current.getInstance() instanceof JSONArray) {
                        // add it in the order we see it
                        JSONArray jsonArray = (JSONArray) current.getInstance();  
                        jsonArray.put(propertyResult.get(fullPropertyPath));
                    }
                    return;
                }
				
				// Get by the natural key if there is one
				Map<String, Object> naturalKeyValues = new HashMap<String, Object>();	
				if(((EntityType)elementType).getNaturalKey() != null) {
					Collection<String> naturalKey = ((EntityType)elementType).getExpandedNaturalKey();
					for(String key: naturalKey) {
						Object keyValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + key);
						naturalKeyValues.put(key, keyValue);
					}
				}
				Object idValue = null;
				if(((EntityType) elementType).getIdentifierProperty() != null) {
					idValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + ((EntityType) elementType).getIdentifierProperty().getName());
				}
				// find the element
				//elementDO = getObjectCreator().findEntity(idValue, naturalKeyValues, domainElementType, getAnchor(anchor.toString()));
				elementDO = findQueryObject(fullPropertyPath, idValue, domainElementType, qti);

				// check flag to see if the containment should be set
				if(elementDO != null && property.isContainment()) {
					((BusinessObject)elementDO).setContainer(current); // Containment property is null for a collection element
				}					

				if(elementDO == null) { // create and set the instance object

					// Get the property instance if possible
					Object elementInstance = null;

					if( ((ExtendedProperty)property).isMap() ) {
						Object keyValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + QueryFragment.MAP_KEY_ATTRIBUTE);
						Map map = (Map) current.getInstance();
						elementInstance = map.get(keyValue);
					} 

					if(elementInstance == null) {
						if(idValue != null || naturalKeyValues.size() > 0) {
							/*elementDO = current.createDataObject(
								idValue, naturalKeyValues, elementType, null, getAnchor(
									anchor.toString()));*/
							elementDO = createQueryObject(elementInstance, current, fullPropertyPath, idValue, naturalKeyValues, elementType, null, getAnchor(anchor.toString()), qti);
						} else {
							return; // Does not have a collection element
						}
					} else {
						// create the data object using the instance
						elementDO = createQueryObject(elementInstance, current, fullPropertyPath, idValue, naturalKeyValues, elementType, null, getAnchor(anchor.toString()), qti);
						/*
						elementDO = objectCreator.createDataObject(
							elementInstance,
							elementType,
							current,
							null,
							getAnchor(anchor.toString()));*/
					}

					// check flag to see if the containment should be set
					if(property.isContainment())
						((BusinessObject)elementDO).setContainer(current); // Containment property is null for a collection element
				}

				// Add the element
				Object elementInstance = ((BusinessObject)elementDO).getInstance();				
				if( ((ExtendedProperty)property).isMap() ) {
					// If this is a map, get the key
					Object keyValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + QueryFragment.MAP_KEY_ATTRIBUTE);
					Map map = (Map) current.getInstance();
					map.put(keyValue, elementInstance);
				} else if ( ((ExtendedProperty)property).isList() ) {
					Object indexValue = propertyResult.get(anchorPath+currentPath + Settings.PATH_DELIMITER + QueryFragment.LIST_INDEX_ATTRIBUTE);
					if(current.getInstance() instanceof JSONArray) {
						// add it in the order we see it
						JSONArray jsonArray = (JSONArray) current.getInstance();
						visitor.add(currentPath.toString(), new ReconstituteRecordVisitor.AddEvent(jsonArray, elementInstance));

					} else {
						List list = (List)current.getInstance();
						int index = Integer.parseInt(indexValue.toString());
						if (index >= list.size() || list.get(index) != elementInstance) {
							list.add(elementInstance);
						}
					}
				} else if ( ((ExtendedProperty)property).isSet() ) {
					// Currently Immutable JSON is treated as a set, so we should check for this
					if(current.getInstance() instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray) current.getInstance();

						// check if element is not present before adding it.
						boolean alreadyInSet = false;
						for(int i = 0; i < jsonArray.length(); i++) {
							if(jsonArray.get(i) == elementInstance) {
								alreadyInSet = true;
							}
						}

						if(!alreadyInSet) {
							jsonArray.put(elementInstance);
						}
					} else {
						Set set = (Set) current.getInstance();
						set.add(elementInstance);
					}
				}				

				current = (BusinessObject) elementDO;	
			}
		}

		return;
	}

	/*
	 * TODO: Currently supports only id. Need to support Natural key retrieval.
	 */
	private BusinessObject findQueryObject(String fullPath, Object idValue, Type type, QueryTreeInvocation queryTreeInvocation) {
		BusinessObject bo = null;

		String anchorPath = QueryFragment.extractAnchorPath(fullPath);
		if (idValue != null && ((EntityType)type).getIdentifierProperty() != null) {
			bo = queryTreeInvocation.getQueryObject(anchorPath, idValue, type);
		}

		return bo;
	}


	private BusinessObject createQueryObject(Object instance,
											 BusinessObject bo,
											 String queryPath,
											 Object idValue,
											 Map<String, Object> naturalKeyValues,
											 Type type,
											 Property property,
											 String anchorPath,
											 QueryTreeInvocation qti) throws Exception
	{
		BusinessObject queryObject;

		if(instance == null) {
			queryObject = bo.createDataObject(idValue, naturalKeyValues, type, property, anchorPath);
			if(property == null || !property.isMany()) {
				qti.visit(queryPath, queryObject);
			}
		} else if(!BusinessObject.class.isAssignableFrom(instance.getClass())) {
			queryObject = objectCreator.createDataObject(instance, type, bo, property, anchorPath);
		} else {
			queryObject = (BusinessObject)instance;
		}

		return queryObject;
	}

	public static String getAnchor(String path) {
		return path;
	}

	@Override
	public Property getPropertyByPath(String path) {

		// Get the container
		AbstractBO container = (AbstractBO) getDeepestContainer(path);
		String propertyPath = path.substring(path.lastIndexOf(SDO_PATH_DELIMITER)+1);

		// case 1: List element indexed from 0
		if(propertyPath.contains(INDEX_FROM_0)) {
			String propertyName = propertyPath.substring(0, propertyPath.indexOf(INDEX_FROM_0));
			return container.getType().getProperty(propertyName.trim());
		}

		// case 2: List element indexed from 1
		if(propertyPath.contains(INDEX_START)) {
			String propertyName = propertyPath.substring(0, propertyPath.indexOf(INDEX_START));
			return container.getType().getProperty(propertyName.trim());
		}

		return container.getType().getProperty(propertyPath);
	}
	
	@Override
	public void set(Property property, Object value) {
		((ExtendedProperty) property).setValue(this, value);
	}

	@Override
	public void set(String path, Object value) {
		if(path == null || "".equals(path))
			throw new IllegalArgumentException("Path cannot be empty or null");

		if(path.equals(PATH_CONTAINER) || path.equals(SDO_PATH_DELIMITER))
			throw new IllegalArgumentException("Path should refer to a property");

		// Get the container
		AbstractBO container = this;
		if(path.contains(SDO_PATH_DELIMITER)) { // find the correct container
			String containerPath = path.substring(0, path.lastIndexOf(SDO_PATH_DELIMITER));
			container = (AbstractBO) getPathObject(containerPath);
			path = path.substring(path.lastIndexOf(SDO_PATH_DELIMITER)+1);
			container.set(path, value);
			return;
		}

		// Set the value for the property
		// Path now contains the last component
		if(path.contains(INDEX_FROM_0) || path.contains(INDEX_START)) { // List/attribute property setter
			// List element indexed from 0
			if(path.contains(INDEX_FROM_0)) {
				String propertyName = path.substring(0, path.indexOf(INDEX_FROM_0));
				String indexStr = path.substring(path.indexOf(INDEX_FROM_0)+1);
				try {
					int index = Integer.parseInt(indexStr);
					List<Object> valueList = (List<Object>) get(propertyName);
					valueList.set(index, value);
				} catch (NumberFormatException e) {
					Property property = getType().getProperty(propertyName);

					if(isIndexOperation(indexStr)) {
						setIndexObject(indexStr, propertyName, value);
					} else if(Map.class.isAssignableFrom(property.getType().getInstanceClass())) {
						Map<String, Object> values = (Map<String, Object>) get(propertyName);
						values.put(indexStr, value);					
					}
				}
				return;
			}		

			// List element indexed from 1
			if(path.contains(INDEX_START)) {
				String propertyName = path.substring(0, path.indexOf(INDEX_START));
				String indexStr = path.substring(path.indexOf(INDEX_START)+1, path.indexOf(INDEX_END));

				try {
					int index = Integer.parseInt(indexStr);
					List<Object> valueList = (List<Object>) get(propertyName);
					valueList.set(index-1, value);
				} catch (NumberFormatException e) {
					if(isIndexOperation(indexStr))
						setIndexObject(indexStr, propertyName, value);
					else
						// List element indexed by attribute value
						setByPropertyFilter(path, value);
				}
			}
		} else { // simple property setter
			ExtendedProperty property = (ExtendedProperty) getType().getProperty(path);
			property.setValue(this, value);
		}
	}

	protected void setByPropertyFilter(String path, Object value) {

		if(!DataObject.class.isAssignableFrom(value.getClass()))
			throw new IllegalArgumentException("value should be of type DataObject");

		String propertyName = path.substring(0, path.indexOf(INDEX_START));
		String indexStr = path.substring(path.indexOf(INDEX_START)+1, path.indexOf(INDEX_END));

		Property property = getType().getProperty(propertyName);
		if(Map.class.isAssignableFrom(property.getType().getInstanceClass()) && indexStr.indexOf(ATTR_DELIMITER) == -1) {
			Map<String, Object> values = (Map<String, Object>) get(property);
			values.put(indexStr, value);
			return;
		}		
		if(!DataObject.class.isAssignableFrom(property.getType().getInstanceClass()))
			throw new IllegalArgumentException("The property '" + propertyName + "' refers to a non-DataObject collection");
		List<DataObject> values = (List<DataObject>) get(property);

		String attributeName = indexStr.substring(0, indexStr.indexOf(ATTR_DELIMITER));
		Property attributeProperty = getType().getProperty(attributeName);
		if(!attributeProperty.getType().isDataType())
			throw new IllegalArgumentException("Attribute '" + attributeName + "' cannot have a value of type DataObject");

		String attributeValue = indexStr.substring(indexStr.indexOf(ATTR_DELIMITER)+1);
		attributeValue = attributeValue.replaceAll("^\"|\"$|^'|'$", "");

		// TODO: ensure the string representation is according to the SDO spec
		for(int i = 0; i < values.size(); i++) {
			DataObject dataObject = values.get(i);
			if(dataObject.get(attributeProperty).toString().equals(attributeValue)) {
				values.set(i, (DataObject) value);
				break;
			}
		}		
	}	

	@Override
	public DataObject getExistingDataObject(String path) {
		Object instanceObject = getPathObject(path);
		BusinessObject result = (BusinessObject) ((BusinessObject)getRootObject()).getObjectCreator().getExistingDataObject(instanceObject);

		return result;
	}

	@Override
	public void set(int propertyIndex, Object value) {

		ExtendedProperty property = (ExtendedProperty) getType().getProperties().get(propertyIndex);
		set(property, value);
	}
	
	@Override
	public Object getDataObject(Property property) {
		if(this.instance == null)
			return null;

		Object value = ((ExtendedProperty)property).getValue(this);
		if(value == null)
			return null;

		Object dataObject = objectCreator.getExistingDataObject(value);
		if(dataObject == null) {
			if(((ExtendedProperty)property).isDataType()) {
				dataObject = value;
			} else {
				BusinessObject container = this;
				Property containmentProperty = property;
				if(!property.isContainment()) {
					container = null;
					containmentProperty = null;
				}
				if(logger.isDebugEnabled()) {
					if(SimpleType.class.isAssignableFrom(property.getType().getClass())) {
						logger.debug("TYPE: " + this.type.getName() + ", PRINTING: " + property.getName());
					}
				}
				dataObject = objectCreator.createDataObject(value, property.getType(), container, containmentProperty);
			}
		}

		return dataObject;
	}	

	@Override
	public Object get(Property property) {
		if(this.instance == null)
			return null;

		if(property == null) {
			// This can happen if additional properties are used by the system
			// such as UserKey and this field is not present while importing content.
			return null;
		}
		
		if(property.isOpenContent()) {
			return getOpenPropertyValue(property.getName());			
		}

		return ((ExtendedProperty)property).getValue(this);
		/*
		Object value = ((ExtendedProperty)property).getValue(this.instance);
		if(value == null)
			return null;

		Object dataObject = objectCreator.getExistingDataObject(value);
		if(dataObject == null) {
			if(((ExtendedProperty)property).isDataType()) {
				dataObject = value;
			} else {
				BusinessObject container = this;
				Property containmentProperty = property;
				if(!property.isContainment()) {
					container = null;
					containmentProperty = null;
				}
				if(logger.isDebugEnabled()) {
					if(SimpleType.class.isAssignableFrom(property.getType().getClass())) {
						logger.debug("TYPE: " + this.type.getName() + ", PRINTING: " + property.getName());
					}
				}
				dataObject = objectCreator.createDataObject(value, property.getType(), container, containmentProperty);
			}
		}

		return dataObject;
		*/
	}

	@Override
	public DataObject getExistingDataObject(Property property) {
		return objectCreator.getExistingDataObject(((ExtendedProperty)property).getValue(this));
	}

	@Override
	public List<?> getList(Property property) {
		return (new DataObjectList(this, property)).list();					
	}

	@Override
	public DataObject createDataObject(String propertyName) {
		Property property = getType().getProperty(propertyName);
		return createDataObject(property, property.getType());
	}

	@Override
	public DataObject createDataObject(int propertyIndex) {
		Property property = (Property) getType().getProperties().get(propertyIndex);
		return createDataObject(property, property.getType());
	}

	@Override
	public DataObject createDataObject(Property property) {
		return createDataObject(property, property.getType());
	}

	@Override
	public DataObject createDataObject(String propertyName,
			String namespaceURI, String typeName) {
		try {
			Class<?> javaClass = Class.forName(namespaceURI + typeName);
			Shape shape = ((BusinessObject)getRootObject()).getObjectCreator().getShape();
			Type type = shape.getType(javaClass);

			Property property = getType().getProperty(propertyName);		
			return createDataObject(property, type);
		} catch (ClassNotFoundException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public DataObject createDataObject(int propertyIndex, String namespaceURI,
			String typeName) {

		try {
			Class<?> javaClass = Class.forName(namespaceURI + typeName);
			Shape shape = ((BusinessObject)getRootObject()).getObjectCreator().getShape();
			Type type = shape.getType(javaClass);

			Property property = getType().getProperties().get(propertyIndex);
			return createDataObject(property, type);
		} catch (ClassNotFoundException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public DataObject createDataObject(Property property, Type type) {
		if(instance == null)
			return null;

		if(property.getType().isDataType())
			throw new IllegalStateException("A DataObject needs to have properties");

		Object childInstance = ((ExtendedProperty)property).getValue(this);
		BusinessObject dataObject = ((BusinessObject)getRootObject()).getObjectCreator().createDataObject(childInstance, (EntityType) type, this, property);

		return dataObject;
	}

	@Override
	public DataObject getContainer() {
		return container;
	}

	@Override
	public void setContainer(DataObject value) {
		this.container = value;
	}

	@Override
	public void setContainmentProperty(Property value) {
		this.containmentProperty = value;
	}

	@Override
	public Property getContainmentProperty() {
		return containmentProperty;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Property getInstanceProperty(String propertyName) {
		return getType().getProperty(propertyName);
	}

	@Override
	public Property getProperty(String propertyName) {
		return getInstanceProperty(propertyName);
	}

	@Override
	public DataObject getRootObject() {
		return (DataObject) get(SDO_PATH_DELIMITER);
	}

	@Override
	public ObjectCreator getObjectCreator() {
		return objectCreator;
	}

	@Override
	public BusinessObject load (Settings settings)
	{
		CallInfo callInfo = new CallInfo(this, null, null, null);
		settings.setAction(AggregateAction.LOAD);
		callInfo.setSettings(settings);

        TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(MapperSide.DOMAIN);
        ObjectCreator oc = new ObjectCreator(
            settings,
            getObjectCreator().getDataStore(),
            typeMapper);		
		return oc.createTarget(callInfo);
	}

	@Override
	public DataObject read(Settings settings) {
		// invoke read only on the root object
		if(!isRoot())
			throw new RuntimeException("Read needs to be invoked on the root data object");

		CallInfo callInfo = new CallInfo(this, null, null, null);
		callInfo.setSettings(settings);		

		// Create an object creator for the target root
        TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(MapperSide.EXTERNAL);        
		ObjectCreator oc = new ObjectCreator(settings, getObjectCreator().getDataStore(), typeMapper);
		oc.setReadOnly(true);
		
		callInfo.setOutputObjectCreator(oc);
		ReadOperation operation = new ReadOperation();
		callInfo.setOperation(operation);

		try {
			Object target = operation.createTarget(callInfo, null);
			callInfo.setOutput(target);
			
			// Needed to hold references to open property created objects
			if(oc.getCreationStrategy().needsObjectGraph()) {
				oc.setObjectGraph((BusinessObject) target);
			}

			oc.setShare(true);
			operation.execute(callInfo);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return (BusinessObject) callInfo.getOutput();
	}

	@Override
	public DataObject toDomain(Settings settings) {

		settings.setAction(AggregateAction.TO_DOMAIN);
		return create(settings);
	}

	@Override
	public DataObject toExternal(Settings settings) {
		settings.setAction(AggregateAction.TO_EXTERNAL);
		return read(settings);
	}

	@Override
	public List<?> query(Settings settings) {
		if(settings.getView() == null)
			throw new RuntimeException("A view needs to be specified to use the query method");

		CallInfo callInfo = new CallInfo(this, null, null, null);
		AggregateAction savedAction = settings.getAction();
		settings.setAction(AggregateAction.READ);
		callInfo.setSettings(settings);		

        TypeMapper typeMapper = getObjectCreator().getTypeMapper().newInstance(settings.doBaseline() ? MapperSide.DOMAIN : MapperSide.EXTERNAL);        
        ObjectCreator oc = new ObjectCreator(settings, getObjectCreator().getDataStore(), typeMapper);
		oc.setReadOnly(true);
		oc.setShare(true);
		
		callInfo.setOutputObjectCreator(oc);
		if(settings.isDenormalized()) {
			callInfo.setOperation(new DenormalizedQueryOperation());
		} else {
			callInfo.setOperation(new QueryOperation());
		}
		
		try {
			Type targetType = getObjectCreator().getDataModel().getType(typeMapper.getShape(), settings.getDowncastName(), settings.getEntityType());
			callInfo.setOutput(callInfo.getOperation().createTarget(callInfo, targetType));
			
			// Needed to hold references to open property created objects
			if(oc.getCreationStrategy().needsObjectGraph()) {
				oc.setObjectGraph((BusinessObject) callInfo.getOutput());
			}
			
			callInfo.getOperation().execute(callInfo);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
		settings.setAction(savedAction);

		return (List<Object>) callInfo.getOperation().getResult();
	}

	@Override
	public List<BusinessObject> getList() {
		return getBulkList(null);
	}

	@Override
	public List<BusinessObject> getBulkList(Settings settings) {
		return (new DataObjectList(this)).list(settings);
	}

	private boolean isMany(Object obj) {
		return instance instanceof Collection || instance instanceof Map;
	}

	@Override
	public int hashCode() {
		int h = super.hashCode();

		if(getType().isDataType()) {
			if(isMany(instance))
			{
				return  31 * h + System.identityHashCode(instance);
			}

			h = 31 * h + instance.hashCode(); 
		}else { 
			ExtendedProperty identifierProperty = (ExtendedProperty) ((EntityType)type).getIdentifierProperty();
			Object id = identifierProperty != null ? identifierProperty.getValue(this) : null;	

			if(id != null) {
				h = 31 * h + id.hashCode();
			}
		}

		return h;
	}

	@Override
	/**
	 * This can potentially be expensive depending on how much we check 
	 * So we shall keep it simple and only check by either memory identity or by the
	 * persistence identifier if it exists
	 */
	public boolean equals(Object o) {

		if(this == o)
			return true;

		if(!AbstractBO.class.isAssignableFrom(o.getClass()))
			return false;

		AbstractBO other = (AbstractBO) o;
		if(getType() != other.getType() )
			return false;

		if(getType().isDataType() ) {
			if(isMany(instance))
			{
				// We don't evaluate the child elements as that could be expensive
				return instance == other.getInstance();
			}

			return instance.equals(other.getInstance());
		}

		// Check if id is the same - then they are equal
		ExtendedProperty identifierProperty = (ExtendedProperty) ((EntityType)type).getIdentifierProperty();
		if(identifierProperty != null) {
			Object thisId = identifierProperty.getValue(this);
			Object otherId = identifierProperty.getValue(other);

			if(thisId != null) {
				if(thisId.equals(otherId)) {
					throw new RuntimeException("Cannot have two BusinessObjects with the same id.");
				}
			}
		}

		return false;
	}

	@Override
	public Object getIdentifierValue() {
		if(instance == null)
			return null;

		if(!EntityType.class.isAssignableFrom(getType().getClass()))
			return null;

		ExtendedProperty idProperty = (ExtendedProperty) ((EntityType)getType()).getIdentifierProperty();
		if(idProperty == null)
			return null;

		return idProperty.getValue(this);
	}

	@Override
	public Object getVersionValue() {
		if(instance == null)
			return null;

		if(!EntityType.class.isAssignableFrom(getType().getClass()))
			return null;

		ExtendedProperty versionProperty = (ExtendedProperty) ((EntityType)getType()).getVersionProperty();
		if(versionProperty == null)
			return null;

		return versionProperty.getValue(this);
	}

	@Override
	public void invokePostLogic(Settings settings) {
		((EntityType)this.getType()).invokePostLogic(settings, this.getInstance());
	}

	@Override
	public boolean isEntity() {
		if(EntityType.class.isAssignableFrom(getType().getClass()) && ((EntityType)getType()).isEntity())
			return true;

		return false;
	}

	@Override
	public boolean isDomainType() {
		return objectCreator.getTypeMapper().isDomain(getType().getName());
	}

	@Override
	public Type getDomainType() {
		return objectCreator.getTypeMapper().getDomainShape().getType(((EntityType)getType()).getEntityName());
	}

	@Override
	public void linkBackPointer() {
		for(Property p: getType().getProperties()) {	
			ExtendedProperty property = (ExtendedProperty) p;
			if(property.isContainment()) {
				if(property.getOpposite() != null)
					property.linkBackPointer(this);
				BusinessObject value = (BusinessObject)getExistingDataObject(property);
				if(value != null)
					value.linkBackPointer();
			}
		}
	}

	@Override
	public void unlinkBackPointer() {
		for(Property p: getType().getProperties()) {	
			ExtendedProperty property = (ExtendedProperty) p;
			if(property.isContainment()) {
				if(property.getOpposite() != null)
					property.unlinkBackPointer(this);
				BusinessObject value = (BusinessObject)getExistingDataObject(property);
				if(value != null)
					value.unlinkBackPointer();				
			}
		}
	}

	@Override
	public Object createReferenceCopy() {
		if(getInstance() == null)
			return null;
		
		Object copy = null;
		try {
			copy = getObjectCreator().createInstance(this.getType());
		} catch(Exception e) {
			throw ClassUtil.wrapRun(e);
		}
		
		for(String propertyName: ((EntityType)getType()).getInitializedProperties()) {
			Object propertyValue = ((ExtendedProperty)getProperty(propertyName)).getValue(this);
			((ExtendedProperty)getProperty(propertyName)).setValue(getSettings(), copy, propertyValue);
		}
		
		return copy;
	}

	@Override
	public void createAggregate() {
		this.createAggregate(null);
	}

	@Override
	public void createAggregate (Settings settings) {

		// Loop through the data object properties and if it is not a data type, then create a Data Object wrapper and recurse
		objectCreator.clearVisited();

		Type entityType = getType();
		if(entityType instanceof ListType) {
			entityType = settings.getEntityType();
		}

		if(!(entityType instanceof EntityType)) {
			throw new RuntimeException("BusinessObject external type is not recognized as an EntityType. Check if the correct TypeMapper is configured.");
		}

		State rootState = null;
		TypeGraph sg = null;
		if(settings != null && settings.getView() != null) {
			// Need to get the StateGraph from the domain type
			sg = settings.getTypeGraph();
			//sg = settings.getView().getTypeGraph(((EntityType)entityType).getDomainType());
			rootState = sg.getRootState();
		}
		createWrapper(settings, this, null, rootState, sg);

		objectCreator.clearVisited();
	}

	public void examine() {
		if (this.instance != null && this.instance instanceof JSONObject
			&& getType() instanceof EntityType && ((EntityType)getType()).getNaturalKey() != null) {

			if(((JSONObject)this.instance).has(Constants.XOR.KEYREF)) {
				return;
			}

			Iterator iter = ((JSONObject)instance).keys();

			Set<String> potentialKey = new HashSet<>();
			Set<String> naturalKey = new HashSet<>(((EntityType)getType()).getExpandedNaturalKey());

			// skip internal fields
			while(iter.hasNext()) {
				String key = (String)iter.next();
				if (key.startsWith(Constants.XOR.XOR_PREFIX)
					|| key.startsWith(Constants.XOR.IDREF)) {
					continue;
				}

				// We allow identifier property to be part of the reference object
				if (((EntityType)getType()).getIdentifierProperty() != null
					&& key.equals(((EntityType)getType()).getIdentifierProperty().getName())) {
					continue;
				}

				potentialKey.add(key);
			}
			if(potentialKey.size() > 0) {
				potentialKey.removeAll(naturalKey);
				// This is a reference association object, now mark it as such
				if(potentialKey.size() == 0) {
					((JSONObject)this.instance).put(Constants.XOR.KEYREF, true);
				}
			}
		}
	}

	@Override
	public boolean isReference () {
		if (this.instance != null && this.instance instanceof JSONObject && ((JSONObject)this.instance).has(Constants.XOR.KEYREF)) {
			return (((JSONObject)this.instance).getBoolean(Constants.XOR.KEYREF));
		}

		return false;
	}

	@Override
	public void downcast(EntityType subType) {
		this.type = subType;
	}
	
	protected void createWrapper(Settings settings, BusinessObject parent, Property support, State state, TypeGraph sg) {

		parent.examine();
		
		for(BusinessObject child: parent.getBulkList(settings)) {
			if(parent.getContainmentProperty() != null && parent.getContainmentProperty().isContainment()) {
				child.setContainer(parent);
				child.setContainmentProperty(parent.getContainmentProperty());
			}

			if(sg != null) {
				if(support != null) {
					// Is this property in view? yes then process it else skip it
					if (sg.getOutEdge(state, support.getName()) != null) {
						createWrapper(
							settings,
							child, support,
							(State)sg.getOutEdge(state, support.getName()).getEnd(), sg);
					}
					else {
						return;
					}
				} else {
					createWrapper(settings, child, null, state, sg);
				}
			} else {
				createWrapper(settings, child, null, null, null);
			}
		}

		if(support != null && support.isMany()) {
			return;
		}

		List<Property> properties = new LinkedList<>();
		if(sg != null && state != null) {
			for(Object outEdge: sg.getOutEdges(state)) {
				if(!((Edge)outEdge).isUnlabelled()) {
					Property p = parent.getType().getProperty(((Edge)outEdge).getName());
					if(p != null) {
						properties.add(p);
					}
				}
			}
		} else {
			properties = parent.getType().getProperties();
		}

		for(Property property: properties) {
			if(!((ExtendedProperty) property).isDataType()) {

				// Is this property in view? yes, process it else skip it
				if(sg != null && sg.getOutEdge(state, property.getName()) == null) {
					continue;
				}

				Object propertyInstance = ((ExtendedProperty)property).getValue(parent);
				if(propertyInstance == null)
					continue;

				Object target = objectCreator.getExistingDataObject(propertyInstance);
				if(target != null && !BusinessObject.class.isAssignableFrom(target.getClass()))
					throw new IllegalStateException("Property refers to a DataObject, but the object is not a DataObject");

				BusinessObject child = null;
				if(target != null) {
					child = (BusinessObject)target;
				} else {
					BusinessObject container = parent;
					Property containmentProperty = property;

					// We always treat the collection object as not-shareable. So its container and containmentProperty is always populated.
					if(!property.isContainment() && !property.isMany()) {
						container = null;
						containmentProperty = null;
					}
					child = objectCreator.createDataObject(propertyInstance, property.getType(), container, containmentProperty);
				}

				if(child.isVisited())
					continue;
				else
					child.setVisited(true);

				if(!property.isContainment()) 
					continue;

				if(sg != null) {
					// Is this property in view? yes, process it else skip it
					if (sg.getOutEdge(state, property.getName()) != null) {
						createWrapper(
							settings,
							child, property,
							(State)sg.getOutEdge(state, property.getName()).getEnd(), sg);
					} else {
						continue;
					}
				} else {
					createWrapper(settings, child, null, null, null);
				}
			}
		}
	}
	
	public BusinessObject getOpenPropertyValue(String name) {
		if(objectCreator.getObjectGraph() == null) {
			return null;
		}
		
		BusinessEdge<BusinessObject> edge = objectCreator.getObjectGraph().getOutEdge(this, name);
		return edge == null ? null : edge.getEnd();
	}

	public Settings getSettings() {
		return objectCreator != null ? objectCreator.getSettings() : null;
	}

	@Override
	public String toString() {
		// Use simple name for now so it is easier to view in large object graphs
		// Might need to go bar to FQDN for accuracy reasons

		// Prefer the actual object class name over type name due to polymorphism
		// NOTE: For a dynamic type (JSON) getInstanceClassName() could return null.
		String className = (getInstance() != null) ? AbstractType.getBaseName(getInstanceClassName()) : null;

		String result = (className != null) ? className : AbstractType.getBaseName(getType().getName());

		if(getType() instanceof EntityType && ((EntityType)getType()).getNaturalKey() != null) {
			StringBuilder keyStr = new StringBuilder("[");

			for(String propertyName: ((EntityType)getType()).getExpandedNaturalKey()) {
				if(keyStr.length() > 1) {
					keyStr.append(", ");
				}
				keyStr.append(propertyName + ": " + get(propertyName));
			}

			keyStr.append("]");

			result += keyStr.toString();
		}

		return result;
	}
}
