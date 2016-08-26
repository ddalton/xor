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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import tools.xor.operation.DenormalizedQueryOperation;
import tools.xor.operation.QueryOperation;
import tools.xor.operation.ReadOperation;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;
import tools.xor.view.QueryViewProperty;

public abstract class AbstractBO implements BusinessObject {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	private static final long serialVersionUID = 1L;
	
	public static final String CURRENT = ".";

	// SDO Path related constants
	// Do we need to support this? Is this used?
	// Seems like duplicate functionality to view (Probably needed to support collections)
	// Should we move this to AggregateView? and support them in views?
	public static final String PATH_DELIMITER = "/";
	public static final String INDEX_FROM_0   = ".";
	public static final String INDEX_START    = "[";
	public static final String INDEX_END      = "]";	
	public static final String PATH_CONTAINER = "..";
	public static final String ATTR_DELIMITER = "=";	
	public static final String INSERT_OPERATOR = "+";  // insert at index for a set operation. Returns the element at index for a get operation.
	public static final String APPEND_OPERATOR = "<<"; // append at end of list for a set operation. Returns the last element in a GET operation.

	protected Object               instance;            // The object containing the actual data. This can be a JPA/Hibernate/Javabean object.
	protected Type                 type;                // Represents the type of the entity, that holds the properties that make up this entity
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
		return getContainmentProperty() != null;
	}	

	@Override
	public ObjectPersister getObjectPersister() {
		return objectPersister;
	}

	public AbstractBO(Type type, DataObject container, Property containmentProperty, ObjectCreator objectCreator) {
		this.type                = type;
		this.container           = container;
		this.containmentProperty = containmentProperty;
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
	public void addEntity(BusinessObject entity) {
		Object id = entity.getIdentifierValue();
		if(id != null) {
			EntityKey entityKey = getObjectCreator().getTypeMapper().getEntityKey(id, entity);
			getObjectCreator().addByEntityKey(entityKey, entity);
		}
	}

	@Override
	public void removeEntity(BusinessObject entity) {
		Object id = entity.getIdentifierValue();
		if(id != null) { 
			EntityKey entityKey = getObjectCreator().getTypeMapper().getEntityKey(id, entity);
			getObjectCreator().removeByEntityKey(entityKey);
		}
	}

	/**
	 * Get the derived data object based off a reference data object
	 * <tt>EntityKey</tt>
	 */
	@Override
	public BusinessObject getEntity(BusinessObject entity) {
		Object id = entity.getIdentifierValue();
		if(id != null && !"".equals(id))
			return getByEntityKey(id, entity);

		return null;
	}

	public BusinessObject getByEntityKey(Object id, Type type) {
		EntityKey entityKey = getObjectCreator().getTypeMapper().getEntityKey(id, type);
		return getObjectCreator().getByEntityKey(entityKey);		
	}

	@Override
	public BusinessObject getByEntityKey(Object id, BusinessObject bo) {
		EntityKey entityKey = getObjectCreator().getTypeMapper().getEntityKey(id, bo);
		return getObjectCreator().getByEntityKey(entityKey);		
	}	

	@Override
	public ExtendedProperty getCollectionKeyProperty() {
		Type type = ((ExtendedProperty)containmentProperty).getElementType();
		if(EntityType.class.isAssignableFrom(type.getClass()))
			return (ExtendedProperty) ((EntityType)type).getCollectionUserKey();

		return null;
	}

	@Override
	/**
	 * Invoked on the Collection Element data object
	 */
	public String getCollectionElementId() {
		if(getContainer() == null || !getContainer().getContainmentProperty().isMany())
			throw new RuntimeException("The getCollectionElementId can only be invoked on a collection element");

		Property collectionUserKey = ((BusinessObject)getContainer()).getCollectionKeyProperty();
		if( collectionUserKey != null ) {
			return this.get(collectionUserKey).toString();
		} else { // fallback to id
			Type elementType = ((ExtendedProperty)getContainer().getContainmentProperty()).getElementType();
			Property identifier = ((EntityType)elementType).getIdentifierProperty();
			return (this.get(identifier) == null) ? null : this.get(identifier).toString();
		}
	}

	@Override
	/**
	 * Invoked on the Collection data object
	 */
	public String getCollectionElementId(Object collectionElement) {
		if(!getContainmentProperty().isMany())
			throw new RuntimeException("The getCollectionElementId can only be invoked on a collection object");

		Property collectionUserKey = getCollectionKeyProperty();
		if( collectionUserKey != null ) {
			return ((ExtendedProperty)collectionUserKey).getValue(collectionElement).toString();
		} else { // fallback to id
			Type elementType = ((ExtendedProperty)getContainmentProperty()).getElementType();
			Property identifier = ((EntityType)elementType).getIdentifierProperty();
			return (this.get(identifier) == null) ? null : this.get(identifier).toString();
		}
	}
	
	@Override
	public String getInstanceClassName() {
		if(instance == null) {
			return getType().getInstanceClass().getName();
		}
		
		if(instance != JSONObject.class || !((JSONObject)instance).has(Constants.XOR.TYPE)) {
			return objectCreator.getTypeMapper().toDomain(getType()).getName();
		} else {
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
		
		if(oldInstance != null) {
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
	 * @return
	 */
	private Object getPathObject(String path) {

		if(path == null || "".equals(path))
			throw new IllegalArgumentException("Path cannot be empty or null");

		if(path.equals(PATH_CONTAINER))
			return getContainer();

		if(path.equals(PATH_DELIMITER)) { // root
			DataObject result = this;
			while(result.getContainer() != null)
				result = result.getContainer();
			return result;
		}

		// multiple components
		if(path.contains(PATH_DELIMITER)) {
			if(path.startsWith(PATH_DELIMITER)) {
				return ((DataObject)getPathObject("/")).get(path.substring(path.indexOf(PATH_DELIMITER)+1));
			} else {
				String firstComponent = path.substring(0, path.indexOf(PATH_DELIMITER));
				String remainingPath = path.substring(path.indexOf(PATH_DELIMITER)+1);
				
				// Get the property name related to the first path component
				Object anchor = getPathObject(firstComponent);
				if(anchor != null) {
					BusinessObject anchorBO = objectCreator.getExistingDataObject(anchor);
					if(anchorBO != null) {
						return anchorBO.get(remainingPath);
					} else {
						throw new RuntimeException("Cannot find BusinessObject in object creator");
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
			return getByEntityKey(attributeValue, ((ExtendedProperty)containmentProperty).getElementType());
		} else {
			for(DataObject dataObject: values) {
				if(dataObject.get(attributeProperty).toString().equals(attributeValue))
					return dataObject;
			}		
		}

		return null;
	}

	@Override
	public Object get(String path) {
		return getPathObject(path);
	}

	protected DataObject getDeepestContainer(String path) {
		if(path == null || "".equals(path))
			throw new IllegalArgumentException("Path cannot be empty or null");

		if(path.equals(PATH_CONTAINER) || path.equals(PATH_DELIMITER))
			throw new IllegalArgumentException("Path should refer to a property");

		// Get the container
		AbstractBO container = this;
		if(path.contains(PATH_DELIMITER)) { // find the correct container
			String containerPath = path.substring(0, path.lastIndexOf(PATH_DELIMITER));
			container = (AbstractBO) getPathObject(containerPath);
		}	

		return container;
	}

	@Override
	public BusinessObject createDataObject(Object id, Type instanceType, Property property) throws Exception {
		Object propertyInstance = createInstance(objectCreator, id, instanceType);
		BusinessObject result = objectCreator.createDataObject(propertyInstance, instanceType, this, property);

		if(property != null)
			((ExtendedProperty)property).setValue(this, propertyInstance);

		return result;
	}
	
	public static Object createInstance(ObjectCreator oc, Object id, Type instanceType) throws Exception {
		Object propertyInstance = null;
		
		// We will use the already loaded object in session if one is available
		// This is done only for the query operation that retrieves managed objects
		if(instanceType == null)
			logger.error("!!!!! instanceType is null for object id: " + id);
		if(oc.isReadOnly() && oc.getTypeMapper().isDomain(instanceType.getInstanceClass())) {
			propertyInstance = oc.getPersistenceOrchestrator().getCached(instanceType.getInstanceClass(), id);
			
			if(propertyInstance != null) {
				logger.info("Found in cache for type: " + instanceType.getInstanceClass().getName() + " and id: " + id.toString());
			}
		}
		
		if(propertyInstance == null) {
			propertyInstance = oc.createInstance(instanceType);
			if(!instanceType.isDataType() && ((EntityType)instanceType).getIdentifierProperty() != null )
				((ExtendedProperty)((EntityType)instanceType).getIdentifierProperty()).setValue(propertyInstance, id);
		}
		
		return propertyInstance;
	}

	@Override
	public BusinessObject createDataObject(Object id, Type instanceType) throws Exception {
		Object propertyInstance = createInstance(objectCreator, id, instanceType);
		return objectCreator.createDataObject(propertyInstance, instanceType, null, null);
	}	

	@Override
	public void set(String propertyPath, Map<String, Object> propertyResult) throws Exception {

		// If we are setting a null value then nothing needs to be done
		if( propertyResult.get(QueryViewProperty.qualifyProperty(propertyPath) ) == null)
			return;

		// Since this builds the path for objects already persisted, the identifier value will not be null
		String[] pathSteps = propertyPath.split(Settings.PATH_DELIMITER_REGEX);
		BusinessObject current = this;
		StringBuilder currentPath = new StringBuilder(QueryViewProperty.ROOT_PROPERTY_NAME);
		for(String step: pathSteps) {
			if(currentPath.length() > 0)
				currentPath.append(Settings.PATH_DELIMITER);
			currentPath.append(step);

			Property property = current.getInstanceProperty(step);
			if(property == null)
				throw new RuntimeException("Unable to resolve property: " + propertyPath);

			//Object propertyDO = current.get(property);
			Object propertyDO = current.getDataObject(property);
			if(!property.isMany()) { 
				if(property.getType().isDataType() ) {
					// Populate the field and return the data object
					((ExtendedProperty)property).setValue(current, propertyResult.get(QueryViewProperty.qualifyProperty(propertyPath) ));
					return;
				}

				if( ((EntityType)property.getType()).isEmbedded() )
					continue; // Embedded objects are automatically created as part of its lifecycle owner

				if(propertyDO == null) {
					// Check if there is a data object with the given key
					// Get the identifier value
					Object idValue = null;
					if(((EntityType)property.getType()).getIdentifierProperty() != null) {
						idValue = propertyResult.get(currentPath + Settings.PATH_DELIMITER + ((EntityType)property.getType()).getIdentifierProperty().getName());
						propertyDO = getByEntityKey(idValue, property.getType());
					}
					if(propertyDO == null) { // create and set the instance object
						// check if we are narrowing
						String narrowToType = (String) propertyResult.get(currentPath + Settings.PATH_DELIMITER + QueryViewProperty.ENTITYNAME_ATTRIBUTE);
						EntityType objectType = (EntityType) property.getType();
						if(narrowToType != null)
							objectType = (EntityType) getObjectCreator().getDAS().getType(narrowToType);
						propertyDO = current.createDataObject(idValue, objectType, property);
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
				if(propertyDO == null) { // create and set the collection/map object
					propertyDO = current.createDataObject(null, property.getType(), property);
				} else if(!BusinessObject.class.isAssignableFrom(propertyDO.getClass())) {
					propertyDO = objectCreator.createDataObject(propertyDO, property.getType(), current, property);
				}
				current = (BusinessObject) propertyDO;

				// Get the identifier value from the collection element
				Type elementType = ((ExtendedProperty)property).getElementType();
				Object idValue = null;
				if(((EntityType) elementType).getIdentifierProperty() != null) {
					idValue = propertyResult.get(currentPath + Settings.PATH_DELIMITER + ((EntityType) elementType).getIdentifierProperty().getName());
					propertyDO = getByEntityKey(idValue, elementType);

					// check flag to see if the containment should be set
					if(propertyDO != null && property.isContainment())
						((BusinessObject)propertyDO).setContainer(current); // Containment property is null for a collection element					
				}

				if(propertyDO == null) { // create and set the instance object

					// Get the property instance if possible
					Object elementInstance = null;

					if( ((ExtendedProperty)property).isMap() ) {
						Object keyValue = propertyResult.get(currentPath + Settings.PATH_DELIMITER + QueryViewProperty.MAP_KEY_ATTRIBUTE);
						Map map = (Map) current.getInstance();
						elementInstance = map.get(keyValue);
					} else if ( ((ExtendedProperty)property).isList() || ((ExtendedProperty)property).isSet() ) {

						// If the collection element is an embeddable look for a user key otherwise throw a operation not supported. 
						// TODO: This check should be done in view validation	

						// TODO: performance optimization. Create an EntityKey object where Id is the collection owner id + property name + user key 
						Object colUserKeyValue = propertyResult.get(currentPath + Settings.PATH_DELIMITER + QueryViewProperty.COL_USERKEY_ATTRIBUTE);
						if(colUserKeyValue != null) {
							Collection collection = (Collection) current.getInstance();
							ExtendedProperty colUserKey = (ExtendedProperty) (EntityType.class.isAssignableFrom(elementType.getClass()) ? ((EntityType)elementType).getCollectionUserKey() : null);	
							for(Object obj: collection) {
								if( colUserKey != null && colUserKeyValue.equals(colUserKey.getValue(obj)) ) {
									elementInstance = obj;
									break;
								} 
							}
						}
					}

					if(elementInstance == null) {
						if(idValue != null)
							propertyDO = current.createDataObject(idValue, (EntityType) elementType, null);							
						else
							return; // Does not have a collection element
					} else
						// create the data object using the instance
						propertyDO = objectCreator.createDataObject(elementInstance, elementType, current, null);

					// check flag to see if the containment should be set
					if(property.isContainment())
						((BusinessObject)propertyDO).setContainer(current); // Containment property is null for a collection element
				}

				// Add the element
				Object elementInstance = ((BusinessObject)propertyDO).getInstance();				
				if( ((ExtendedProperty)property).isMap() ) {
					// If this is a map, get the key
					Object keyValue = propertyResult.get(currentPath + Settings.PATH_DELIMITER + QueryViewProperty.MAP_KEY_ATTRIBUTE);
					Map map = (Map) current.getInstance();
					map.put(keyValue, elementInstance);
				} else if ( ((ExtendedProperty)property).isList() ) {
					Object indexValue = propertyResult.get(currentPath + Settings.PATH_DELIMITER + QueryViewProperty.LIST_INDEX_ATTRIBUTE);					
					List list = (List) current.getInstance();
					int index = Integer.parseInt(indexValue.toString());
					if(index >= list.size() || list.get(index) != elementInstance)
						list.add(elementInstance);
				} else if ( ((ExtendedProperty)property).isSet() ) {
					Set set = (Set) current.getInstance();
					set.add(elementInstance);
				}				

				current = (BusinessObject) propertyDO;	
			}
		}

		return;
	}

	@Override
	public Property getPropertyByPath(String path) {

		// Get the container
		AbstractBO container = (AbstractBO) getDeepestContainer(path);
		String propertyPath = path.substring(path.lastIndexOf(PATH_DELIMITER)+1);

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

		if(path.equals(PATH_CONTAINER) || path.equals(PATH_DELIMITER))
			throw new IllegalArgumentException("Path should refer to a property");

		// Get the container
		AbstractBO container = this;
		if(path.contains(PATH_DELIMITER)) { // find the correct container
			String containerPath = path.substring(0, path.lastIndexOf(PATH_DELIMITER));
			container = (AbstractBO) getPathObject(containerPath);
			path = path.substring(path.lastIndexOf(PATH_DELIMITER)+1);
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
			DataAccessService das = ((BusinessObject)getRootObject()).getObjectCreator().getDAS();
			Type type = das.getType(javaClass);

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
			DataAccessService das = ((BusinessObject)getRootObject()).getObjectCreator().getDAS();
			Type type = das.getType(javaClass);

			Property property = (Property) getType().getProperties().get(propertyIndex);			
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
		return (DataObject) get(PATH_DELIMITER);
	}

	@Override
	public ObjectCreator getObjectCreator() {
		return objectCreator;
	}

	@Override
	public BusinessObject load(Settings settings) {
		CallInfo callInfo = new CallInfo(this, null, null, null);
		settings.setAction(AggregateAction.LOAD);
		callInfo.setSettings(settings);		

		ObjectCreator oc = new ObjectCreator(getObjectCreator().getDAS(), getObjectCreator().getPersistenceOrchestrator(), MapperDirection.DOMAINTODOMAIN);
		return oc.createTarget(callInfo);
	}
	

	@Override
	public DataObject read(Settings settings) {
		// invoke read only on the root object
		if(!isRoot())
			throw new RuntimeException("Read needs to be invoked on the root data object");

		CallInfo callInfo = new CallInfo(this, null, null, null);
		settings.setAction(AggregateAction.READ);
		callInfo.setSettings(settings);		

		// Create an object creator for the target root
		ObjectCreator oc = new ObjectCreator(getObjectCreator().getDAS(), getObjectCreator().getPersistenceOrchestrator(), MapperDirection.EXTERNALTOEXTERNAL);
		oc.setReadOnly(true);
		
		callInfo.setOutputObjectCreator(oc);
		ReadOperation operation = new ReadOperation();
		callInfo.setOperation(operation);

		try {
			Object target = operation.createTarget(callInfo, null);
			callInfo.setOutput(target);
			operation.execute(callInfo);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		return (BusinessObject) callInfo.getOutput();
	}

	@Override
	public List<?> query(Settings settings) {
		if(settings.getView() == null)
			throw new RuntimeException("A view needs to be specified to use the query method");

		CallInfo callInfo = new CallInfo(this, null, null, null);
		AggregateAction savedAction = settings.getAction();
		settings.setAction(AggregateAction.READ);
		callInfo.setSettings(settings);		

		MapperDirection direction = MapperDirection.EXTERNALTOEXTERNAL;
		if(settings.doBaseline()) // We need to return a domain object
			direction = direction.toDomain();
		
		ObjectCreator oc = new ObjectCreator(getObjectCreator().getDAS(), getObjectCreator().getPersistenceOrchestrator(), direction);
		oc.setReadOnly(true);
		
		callInfo.setOutputObjectCreator(oc);
		if(settings.isDenormalized()) {
			callInfo.setOperation(new DenormalizedQueryOperation());
		} else {
			callInfo.setOperation(new QueryOperation());
		}
		
		try {
			Class<?> desiredClass = settings.doBaseline() ? settings.getNarrowedClass() : getObjectCreator().getDAS().getTypeMapper().toExternal(settings.getNarrowedClass());
			callInfo.setOutput(callInfo.getOperation().createTarget(callInfo, desiredClass));
			callInfo.getOperation().execute(callInfo);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
		settings.setAction(savedAction);

		return (List<Object>) callInfo.getOperation().getResult();
	}

	@Override
	public List<BusinessObject> getList() {
		//if(listStore != null)
		//	return listStore;
		
		return (new DataObjectList(this)).list();
	}

	@Override
	public int hashCode() {
		int h = super.hashCode();

		if(getType().isDataType()) {
			h = 31 * h + instance.hashCode(); 
		}else { 
			ExtendedProperty identifierProperty = (ExtendedProperty) ((EntityType)type).getIdentifierProperty();
			Object id = identifierProperty != null ? identifierProperty.getValue(this) : null;	

			if(id != null) {
				h = 31 * h + id.hashCode();
			} else {
				// iterate through the properties
				for(Property property: type.getProperties()) {
					ExtendedProperty extended = (ExtendedProperty) property;
					Object value = extended.getValue(this);
					h = 31 * h + ((value != null) ? value.hashCode() : 0);
				}
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

		if(getType().isDataType()) {
			return instance.equals(other.getInstance());
		}

		// Check if id is the same - then they are equal
		ExtendedProperty identifierProperty = (ExtendedProperty) ((EntityType)type).getIdentifierProperty();
		Object thisId = identifierProperty.getValue(this);
		Object otherId = identifierProperty.getValue(other);

		if(thisId != null)
			return thisId.equals(otherId);

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
		return objectCreator.getTypeMapper().isDomain(getType().getInstanceClass());
	}

	@Override
	public Type getDomainType() {
		return getObjectCreator().getDAS().getType(getObjectCreator().getTypeMapper().toDomain(getType().getInstanceClass()));
	}

	@Override
	public Type getExternalType() {
		return getObjectCreator().getDAS().getType(getObjectCreator().getTypeMapper().toExternal(getType().getInstanceClass()));
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
			((ExtendedProperty)getProperty(propertyName)).setValue(copy, propertyValue);
		}
		
		return copy;
	}
	
	@Override
	public void createAggregate() {

		// Loop through the data object properties and if it is not a data type, then create a Data Object wrapper and recurse
		objectCreator.clearVisited();
		createWrapper(this);

		objectCreator.clearVisited();
	}	
	
	protected void createWrapper(BusinessObject parent) {
		
		for(BusinessObject child: parent.getList()) {
			if(parent.getContainmentProperty().isContainment()) {
				child.setContainer(parent);
				child.setContainmentProperty(parent.getContainmentProperty());
			}
			createWrapper(child);
		}

		for(Property property: parent.getType().getProperties()) {	
			if(!((ExtendedProperty) property).isDataType()) {
				Object propertyInstance = ((ExtendedProperty)property).getValue(parent);
				if(propertyInstance == null)
					continue;

				Object target = objectCreator.getExistingDataObject(propertyInstance);
				if(target != null && !BusinessObject.class.isAssignableFrom(target.getClass()))
					throw new IllegalStateException("Property refers to a DataObject, but the object is not a DataObject");

				BusinessObject child = null;
				if(target != null)
					child = (BusinessObject) target;
				else {
					BusinessObject container = parent;
					Property containmentProperty = property;
					if(!property.isContainment()) {
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

				createWrapper(child);				
			}
		}
	}		
}
