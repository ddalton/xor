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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OrderBy;
import javax.persistence.UniqueConstraint;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.annotation.XorAlways;
import tools.xor.annotation.XorDataService;
import tools.xor.annotation.XorDomain;
import tools.xor.annotation.XorExternal;
import tools.xor.annotation.XorExternalData;
import tools.xor.annotation.XorResult;
import tools.xor.annotation.XorVersion;
import tools.xor.event.PropertyEvent;
import tools.xor.generator.Generator;
import tools.xor.service.DataAccessService;
import tools.xor.service.PersistenceOrchestrator;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.I18NUtils;


public abstract class AbstractProperty implements ExtendedProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final String EMPTY_TAG = "_EMPTY_";
	public static final String GETTER_TAG = "_GETTER_";	
	public static final String SETTER_TAG = "_SETTER_";	

	protected AccessType accessType;
	protected boolean    alwaysInitialized;
	private boolean      readOnly;
	private Map<String, Object> constraints;
	
	private   VersionInfo versionInfo;

	// The following fields are used to set/retrieve value
	protected Method     getterMethod;
	protected Method     setterMethod;  
	
	protected Field      field;
	
	/**
	 * Business logic related annotations.
	 * Please note that these events are related to the data state in application
	 * memory and not Database state since we have no control on when that happens
	 * due to factors out of our control since we want to be DB independent.
	 * 
	 *  1. CREATE: 
	 *     Refers to a custom object constructor, that 
	 *     takes care of transient field initialization etc.
	 *     This event is triggered before the created object is linked
	 *  2. READ: 
	 *     Refers to a business logic getter method 
	 *  3. UPDATE: 
	 *     Refers to a direct update on the property and refers
	 *     to a business logic setter method
	 *  4. DELETE
	 *     Invoked when the last reference to the object is removed
	 */
	// Business logic related annotations 
	protected List<MethodInfo>     lambdas;
	protected String           name; // If this is used, it represents an open property
	protected RelationshipType relType; // The type of relationship this open property models
	
	/**
	 * The set of fields comprising the relationship for TO_ONE and TO_MANY types
	 * Modeled as a map since the value might be referred by different names on the two sides fo the relationship
	 */
	protected Map<String, String> keyFields; 
	  

	protected ExtendedProperty positionProperty;
	protected ExtendedProperty mapKeyOf;
	protected ExtendedProperty indexOf;
	protected boolean          hasIdAnnotation;	 // Convert to Boolean type for lazy initialization and caching
	private String       mapPath;
	private Property     mappedBy;
	private Property     mapOf;
	private Boolean      unique; 
	private Type         type;
	private EntityType   parentType;
	private Generator generator;

	// Collection related
	protected Type       keyType;
	protected Type       elementType;	
	protected Set<String> collectionKey; 

	private List<String> aliasNames = new ArrayList<String>();

	public AbstractProperty(Type type, EntityType parentType) {
		this.type = type;
		this.parentType = parentType;		
	}
	
	public AbstractProperty(String name, Type type, EntityType parentType) {
		this(type, parentType);
		this.name = name;
		this.relType = RelationshipType.CUSTOM;
		
		// This is needed to map the open property with its appropriate dataX methods
		initBusinessLogicAnnotations();		
	}	

	public AbstractProperty(String name, Type type, EntityType parentType, RelationshipType relType, EntityType elementType) {
		this(name, type, parentType);
		this.relType = relType;
		
		if(this.relType == RelationshipType.TO_MANY && elementType == null) {
			throw new IllegalStateException("Element type is required for a collection relationship");
		}
		
		this.elementType = elementType;
	}

	@Override
	public void addKeyMapping(String[] thisSet, String[] thatSet) {
		if(this.keyFields == null) {
			this.keyFields = new HashMap<String, String>();
		}
		if(thisSet == null || thatSet == null ||
				thisSet.length == 0 || thatSet.length == 0) {
			throw new IllegalArgumentException("OpenMapping set cannot be null or empty");
		}
		if(thisSet.length != thatSet.length) {
			throw new IllegalArgumentException("The relationship should have the same number of composite key parts");
		}
		for(int i = 0; i < thisSet.length; i++) {
			this.keyFields.put(thisSet[i], thatSet[i]);	
		}
	}

	@Override
	public boolean isOpenContent() {
		if(name != null) {
			return true;
		}
		return false;
	}

	@Override
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public List<?> getAliasNames() {
		return aliasNames;
	}	

	@Override
	public Type getKeyType() {
		return keyType;
	}

	@Override
	public Type getElementType() {
		return elementType;
	}	

	public static boolean isCascadable(Property property) {
		if(property == null)
			return true;
		return property.isContainment();
	}

	public static boolean isRequired(Property property) {
		return property != null && !property.isNullable();	
	}	

	public static boolean isCollection(Property property) {
		return property != null && property.isMany();
	}	
	
	@Override
	public boolean isIdentifier() {
		return hasIdAnnotation;
	}

	@Override
	public Property getOpposite() {
		if(getMappedBy() != null)
			return getMappedBy();

		if(getMapOf() != null)
			return getMapOf();

		return null;
	}

	@Override
	public Property getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(Property property, String mapPath) {
		this.mappedBy = property;

		if(property != null) {
			((ExtendedProperty)mappedBy).setMapPath(mapPath);
			((AbstractProperty)property).setMapOf(this);
		}
	}

	@Override
	public PersistentAttributeType getAssociationType() {
		if( getType().isDataType() )
			return null;
			
		if( ((EntityType)getType()).isEmbedded() )
			return null;
		
		
		if(isMany()) {
			return PersistentAttributeType.ONE_TO_MANY;
		}
		return PersistentAttributeType.MANY_TO_ONE;
	}	

	@Override
	public Property getMapOf() {
		return mapOf;
	}	

	public void setMapOf(Property property) {
		this.mapOf = property;
	}	

	@Override
	public EntityType getContainingType() {
		return parentType;
	}	
	
	protected boolean isAnnotationPresent(Class annotationClass) {
		if(getterMethod != null && getterMethod.isAnnotationPresent(annotationClass))
			return true;
		else if(field != null && field.isAnnotationPresent(annotationClass))
			return true;

		return false;
	}	

	protected Annotation getAnnotation(Class annotationClass) {
		if(getterMethod != null && getterMethod.isAnnotationPresent(annotationClass))
			return getterMethod.getAnnotation(annotationClass);
		else if(field != null && field.isAnnotationPresent(annotationClass))
			return field.getAnnotation(annotationClass);

		return null;
	}	

	public Method getGetterMethod() {
		return getterMethod;
	}

	protected void initBusinessLogicAnnotations() {
		
		if(getContainingType().getLambdas(getName()) != null) {
			lambdas = getContainingType().getLambdas(getName());
		}
		
		/*
		 * If this is an open field. The user should provide a custom
		 * setter and getter. Ensure that these methods are present
		 */
		if(getAccessType() == AccessType.USERDEFINED) {
			// Check for a custom getter
			String[] getterTag = {GETTER_TAG};
			boolean getterFound = false;
			for(MethodInfo mi: lambdas) {
				getterFound = ClassUtil.intersectsTags(getterTag, mi.getTags());
				if(getterFound) {
					break;
				}
			}
			
			// Check for a custom getter
			String[] setterTag = {SETTER_TAG};
			boolean setterFound = false;
			for(MethodInfo mi: lambdas) {
				setterFound = ClassUtil.intersectsTags(getterTag, mi.getTags());
				if(setterFound) {
					break;
				}
			}
			
			if(!getterFound || !setterFound) {
				logger.warn("Custom setter and getters are not present for the open property: " + getName());
			}
		}
	}
	
	private void initApiVersion() {
		field = getContainingType().getField(getName());
		if(field == null && getterMethod != null) // try the method name
			field = getContainingType().getField(getterMethod.getName());
		if(field != null) {
			XorVersion version = field.getAnnotation(XorVersion.class);
			if(version != null) {
				versionInfo = (new VersionInfo(version.fromVersion(), version.untilVersion()));
			}
		}
	}

	protected void init ()
	{
		Class<?> instanceClass = getContainingType().getInstanceClass();

		// getterMethod from getContainingType 

		getterMethod = getContainingType().getGetterMethod(getName());
		logger.debug("Class name: " + instanceClass.getName() + ", property name: " + getName());

		initBusinessLogicAnnotations();
		
		if(getAccessType() == AccessType.USERDEFINED) {
			return;
		}

		if (getterMethod == null) { // No point in continuing as the field is not present
			if (
				!ImmutableJsonProperty.class.isAssignableFrom(this.getClass()) &&
					!MutableJsonProperty.class.isAssignableFrom(this.getClass())

				) {
				logger.warn("Found null for " + instanceClass.getName() + "#" + getName());
			}
			return;
		}
		setterMethod = getContainingType().getSetterMethod(getName());		

		initApiVersion();
		initByAnnotations();
		initColumnName();

		// Check if it is an Identifier property
		hasIdAnnotation = isAnnotationPresent(Id.class);		

		if (getterMethod == null || field == null) {
			logger.warn("getter or field name is not accessible: " + getName());
		}

		if ((getterMethod != null && getterMethod.isAnnotationPresent(XorAlways.class)) ||
			(field != null && field.isAnnotationPresent(XorAlways.class))
			) {
			alwaysInitialized = true;
		}

	}

	protected void initByAnnotations() {

		try {
			javax.persistence.Access access = getterMethod.getAnnotation(javax.persistence.Access.class);
			if (access != null)
				accessType = AccessType.valueOf(access.value().name());
			else {
				if (field != null) {
					access = field.getAnnotation(javax.persistence.Access.class);
					if (access != null)
						accessType = AccessType.valueOf(access.value().name());
				}
				else
					logger.warn(
						"Unable to get Field object. The field name is not the same as getter property name "
							+ getName() + ", skipping field access check.");
			}
		} catch (NoClassDefFoundError e) {
			// Ignore the JPA annotation code for persistence mechanisms that do not use JPA
		}
	}

	/**
	 * Used in JPA annotated persistence stores, to map the index column
	 */
	private void initColumnName ()
	{
		Column column = (Column) getAnnotation(Column.class);
		if (column != null) {
			aliasNames.add(column.name());
		}
	}

	@Override
	public void setMapKeyOf(ExtendedProperty property) {
		this.mapKeyOf = property;
	}

	@Override
	public void setIndexOf(ExtendedProperty property) {
		this.indexOf = property;
	}	

	@Override
	public void initPositionProperty() {

		try {
			MapKey mapKey = (MapKey) getAnnotation(MapKey.class);
			if (mapKey != null && (mapKey.name() == null
				|| "".equals(mapKey.name()))) {// return the elements identifier value
				positionProperty = (ExtendedProperty) ((EntityType) getElementType()).getIdentifierProperty();
				return;
			}

			OrderBy orderBy = (OrderBy) getAnnotation(OrderBy.class);
			if (orderBy != null && (orderBy.value() == null || "".equals(orderBy.value()))) {
				positionProperty = (ExtendedProperty) ((EntityType) getElementType()).getIdentifierProperty();
				return;
			}

			if (getElementType() != null) {
				if (mapKey != null) {
					positionProperty = (ExtendedProperty) ((EntityType) getElementType()).getProperty(
						mapKey.name());
					if (positionProperty != null)
						positionProperty.setMapKeyOf(this);
				}

				if (orderBy != null) {
					String orderByPropertyName = orderBy.value().split("\\s+")[0];
					positionProperty = (ExtendedProperty) ((EntityType) getElementType()).getProperty(
						orderByPropertyName);
					logger.debug(
						"initPositionProperty: orderBy is not null => " + getName()
							+ " , orderByPropertyName: " + orderByPropertyName
							+ " isPositionProperty: " + (positionProperty != null));
					if (positionProperty != null) {
						positionProperty.setIndexOf(this);
						if (getKeyType() == null)
							keyType = positionProperty.getType();
					}
				}
			}
		} catch (NoClassDefFoundError e) {
			// Ignore the JPA annotation code for persistence mechanisms that do not use JPA
		}
	}	

	@Override
	public Property getPositionProperty() {
		return positionProperty;
	}

	@Override
	public List<MethodInfo> getLambdas(Settings settings, String[] tags, Phase phase, ProcessingStage stage) {
		List<MethodInfo> result = new LinkedList<MethodInfo>();

		if(lambdas != null) {
			for(MethodInfo mi: lambdas) {
				if(mi.isRelevant(settings, tags, phase, stage)) {
					result.add(mi);
				} 
			}
		}
		return result;
	}	

	public abstract void init(DataAccessService das, Shape shape);

	protected void executePrePropertyLogic(CallInfo ci, Method prePropertyLogic) 
	{
		if(prePropertyLogic == null)
			return;

		Object instance = ClassUtil.getInstance(ci.getOutput());

		Object[] args = {ci};
		try {
			ClassUtil.invokeMethodAsPrivileged(instance, prePropertyLogic, args);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}		

	@Override
	public AccessType getAccessType() {
		if(accessType == null) {
			if(getContainingType() != null)
				return getContainingType().getAccessType();
			else
				return AccessType.PROPERTY;
		} else {
			return accessType;
		}
	}
	
	// TODO: What happens if one of the key fields represents an Open Property?
	private Map<String, Object> getForeignKeyFromSource(BusinessObject sourceBO) {
		Map<String, Object> uniqueKey = new HashMap<String, Object>();
		for(Map.Entry<String, String> entry: this.keyFields.entrySet()) {
			uniqueKey.put(entry.getValue(), sourceBO.get(entry.getKey()));
		}
		return uniqueKey;
	}		
	
	// TODO: What happens if one of the key fields represents an Open Property?
	private Map<String, Object> getForeignKeyFromTarget(BusinessObject targetBO) {
		Map<String, Object> uniqueKey = new HashMap<String, Object>();
		for(Map.Entry<String, String> entry: this.keyFields.entrySet()) {
			uniqueKey.put(entry.getKey(), targetBO.get(entry.getValue()));
		}
		return uniqueKey;
	}	
	
	// Used to find the primary key for collection owner
	private Map<String, Object> getPrimaryKeyFromTarget(BusinessObject targetBO) {
		Map<String, Object> uniqueKey = new HashMap<String, Object>();
		for(Map.Entry<String, String> entry: this.keyFields.entrySet()) {
			uniqueKey.put(entry.getValue(), targetBO.get(entry.getValue()));
		}
		return uniqueKey;
	}	

	/**
	 * TODO: What happens if one of the key fields represents an Open Property?
	 * Set the foreign key from bo to propertyValue keys for TO_ONE relationship
	 * and from bo (element) to owner keys for To_MANY relationship
	 *
	 * @param bo The target of the foreign key
	 * @param foreignKey The key/value mapping(s) for the foreign key
	 */
	private void setForeignKey(BusinessObject bo, Map<String, Object> foreignKey) {

		for(Map.Entry<String, Object> entry: foreignKey.entrySet()) {
			bo.set(entry.getKey(), entry.getValue());
		}

	}

	@Override
	public String getStringValue(BusinessObject dataObject) {
		Object value = getValue(dataObject);

		if(isMany() && value != null && "".equals(value.toString())) {
			value = null;
		}

		return (value == null) ? (String)value : value.toString();
	}

	/**
	 * @param dataObject here refers to a persistence managed object as the external objects
	 *  are handled by subclass overridden methods.
	 * @return value of the property in the given dataObject
	 */
	@Override
	public Object getValue(BusinessObject dataObject)
	{
		PrefetchCache cache = dataObject.getSettings().getPrefetchCache();
		if(field == null && getterMethod == null) {
			if(isManaged()) {
				return query(dataObject);
			}

			if(isOpenContent() && dataObject != null) {
				// We should always receive a persistence managed dataObject
				BusinessObject bo = (BusinessObject) dataObject;
				Object value = bo.getOpenPropertyValue(getName());
				
				// Value was already set and we discovered this using the Object Graph
				if(value != null) {
					return value;
				}
				
				// load the object by id first as it might already have been cached
				// So we need to check if we are referencing an id
				if(getRelationshipType() == RelationshipType.TO_ONE) {

					// Try and obtain the value from the prefetch cache
					if(cache != null) {
						value = cache.getEntity(getType(), getForeignKeyFromSource(bo));
						if(value != null) {
							return value;
						}
					}
					
					Object idValue = null;
					boolean byId = false;
					if(((EntityType)getType()).getIdentifierProperty() != null) {

						if(this.keyFields.size() == 1) {
							Map.Entry<String, String> entry = this.keyFields.entrySet().iterator().next();
							
							if (((EntityType)getType()).getIdentifierProperty().getName().equals(entry.getValue())) {
								byId = true;
								idValue = bo.get(entry.getKey());
							}
						}
						if(idValue != null) {
							value = bo.getBySurrogateKey(idValue, getType());							
						}
					}					
					
					PersistenceOrchestrator po = bo.getObjectCreator().getPersistenceOrchestrator();
					if(value == null) {
						if(idValue != null) {
							value = po.findById(getType().getInstanceClass(), idValue);
						} else if(!byId) {
							value = po.findByProperty(getType(), getForeignKeyFromSource(bo));
						}
					}
				} else if(getRelationshipType() == RelationshipType.TO_MANY) {

					// Try and obtain the value from the prefetch cache
					if(cache != null) {
						value = cache.getCollection(this, getPrimaryKeyFromTarget(bo));
						if(value != null) {
							return value;
						}					
					} 

					// Return a collection of elements. Right now we support only Set. 
					PersistenceOrchestrator po = bo.getObjectCreator().getPersistenceOrchestrator();
					value = po.getCollection(this.getElementType(), getForeignKeyFromSource(bo));
				}
				
				return value;
			}

			// The object does not have the desired property
			String[] params = new String[2];
			params[0] = getName();
			params[1] = getType().getInstanceClass().getName();
			logger.warn(
				I18NUtils.getResource(
					"exception.propertyNotFound",
					I18NUtils.CORE_RESOURCES,
					params));
			return null;
		} else {		
			return query(dataObject);
		}		
	}

	@Override public boolean isReadOnly ()
	{
		return this.readOnly;
	}

	@Override public void setReadOnly (boolean value)
	{
		this.readOnly = value;
	}

	@Override
	public void setValue(BusinessObject dataObject, Object propertyValue) {
		setValue(dataObject.getSettings(), dataObject, propertyValue);
	}

	@Override
	public void setValue(Settings settings, Object dataObject, Object propertyValue)
	{	
		if(field == null && setterMethod == null) {
			if(isManaged()) {
				executeUpdate(dataObject, propertyValue);
			}
			
			if(isOpenContent() && dataObject != null) {
				// We should always receive a persistence managed propertyValue object
				if(getRelationshipType() == RelationshipType.TO_ONE) {
					BusinessObject boValue = (BusinessObject) propertyValue;
					setForeignKey((BusinessObject) dataObject, getForeignKeyFromTarget((BusinessObject) boValue));
				} else if(getRelationshipType() == RelationshipType.TO_MANY) {
					Set<BusinessObject> elementSet = (Set) propertyValue;
					for(BusinessObject element: elementSet) {
						setForeignKey((BusinessObject) element, getForeignKeyFromSource((BusinessObject) element));
					}
				}
			} else {
			
				// This field is not present, could be a field missing between Data transfer and POJO data models
				String[] params = new String[2];
				params[0] = getName();
				params[1] = getType().getInstanceClass().getName();
				logger.warn(I18NUtils.getResource(  "exception.propertyNotFound",I18NUtils.CORE_RESOURCES, params));
				return;
			}
		} else {
			executeUpdate(dataObject, propertyValue);
		}
	}	

	@Override
	public void addElement(BusinessObject dataObject, Object element) {
		java.util.Collection targetCollection = (java.util.Collection) ((BusinessObject) dataObject).getInstance();
		targetCollection.add(element);
	}
	
	@Override
	public void addMapEntry(Object dataObject, Object key, Object value) {
		Map targetMap = (java.util.Map) ((BusinessObject) dataObject).getInstance();
		targetMap.put(key, value);
	}
	
	private Object[] getArgs(Method method, BusinessObject dataObject, PropertyEvent event, Object resultPreviousCallback) {
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		Object[] result = null;

		if(paramAnnotations.length > 0) {
			result = new Object[paramAnnotations.length];
			
			// check that each of the paramAnnotations array has either an Input/Output annotation but not both
			for(int i = 0; i < paramAnnotations.length; i++) {
				Annotation[] paramA = paramAnnotations[i];
				for(Annotation annotation: paramA) {
					if(XorDomain.class.isAssignableFrom(annotation.getClass()) && event.getDomainParent() != null) {
						XorDomain domain = (XorDomain) annotation;
						if(domain.path().equals(AbstractBO.PATH_CONTAINER)) {
							result[i] = domain.wrapper() ? event.getDomainParent() : ClassUtil.getInstance(event.getDomainParent());
						} else if(domain.path().equals(AbstractBO.CURRENT)) {
							result[i] = domain.wrapper() ? event.getDomain() : ClassUtil.getInstance(event.getDomain());
						} else {
							result[i] = domain.wrapper() ? event.getDomainParent().get(domain.path()) : ClassUtil.getInstance(event.getDomainParent().get(domain.path()));
						}
					} else if(XorExternal.class.isAssignableFrom(annotation.getClass()) && event.getExternalParent() != null) {
						XorExternal external = (XorExternal) annotation;
						if(external.path().equals(AbstractBO.PATH_CONTAINER)) {
							result[i] = external.wrapper() ? event.getExternalParent() : ClassUtil.getInstance(event.getExternalParent());						
						} else if(external.path().equals(AbstractBO.CURRENT)) {
							result[i] = external.wrapper() ? event.getExternal() : ClassUtil.getInstance(event.getExternal());
						} else {
							result[i] = external.wrapper() ? event.getExternalParent().get(external.path()) : ClassUtil.getInstance(event.getExternalParent().get(external.path()));
						}
					} else if(XorResult.class.isAssignableFrom(annotation.getClass())) {
						result[i] = resultPreviousCallback;
					} else if(XorDataService.class.isAssignableFrom(annotation.getClass())) {
						result[i] = event.getDomainParent().getObjectCreator().getPersistenceOrchestrator();
					} else if(XorExternalData.class.isAssignableFrom(annotation.getClass())) {
						result[i] = event.getSettings().getExternalData();
					}
				}
			}
		}

		
		return result;
	}
	
	public static class LambdaResult {
		/**
		 * Used to short circuit further processing
		 */
		private boolean capture;
		
		/**
		 * Result from a getter method
		 */
		private Object result;
		
		public LambdaResult(boolean capture, Object result) {
			this.capture = capture;
			this.result = result;
		}
		
		public boolean isCapture() {
			return capture;
		}
		public void setCapture(boolean capture) {
			this.capture = capture;
		}
		public Object getResult() {
			return result;
		}
		public void setResult(Object result) {
			this.result = result;
		}
	}

	@Override
	public LambdaResult evaluateLambda(PropertyEvent event) 
	{
		boolean result = false;
		BusinessObject dataObject = event.getDomainParent();
		Object instance = ClassUtil.getInstance(dataObject);

		List<MethodInfo> dataUpdaters = getLambdas(event.getSettings(), event.getTags(), event.getPhase(), event.getStage());
		Object resultPreviousCallback = event.getValue();
		for(int i = 0; i < dataUpdaters.size(); i++) {
			MethodInfo du = dataUpdaters.get(i);
			Object[] args = getArgs(du.getMethod(), dataObject, event, resultPreviousCallback);

			try {
				// First argument is null since we are invoking a static method
				Object invokee = instance;
				if(Modifier.isStatic(du.getMethod().getModifiers())) {
					invokee = null;
				}
				resultPreviousCallback = ClassUtil.invokeMethodAsPrivileged(invokee, du.getMethod(), args);
			} catch (Exception e) {
				throw ClassUtil.wrapRun(e);
			}
			
			if(du.isCapture() && i < (dataUpdaters.size()-1)) {
				logger.warn("Short circuiting updater callback before other callbacks are called: " + du.getMethod().getName());
				return new LambdaResult(du.isCapture(), resultPreviousCallback);
			}
			result = du.isCapture();
		}
		
		return new LambdaResult(result, resultPreviousCallback);
	}

	/**
	 * This method is public since not all use cases pass in a DataObject instance
	 * @param dataObject instance
	 * @return value of the property
	 */
	public Object query(Object dataObject)
	{
		Object instance = ClassUtil.getInstance(dataObject);
		//if(isOpenContent() && dataObject instanceof BusinessObject) {
		//	instance = ClassUtil.getInstance(((BusinessObject)dataObject).getContainer());
		//}

		// Set the value in the field  
		try {
			if(AccessType.PROPERTY.equals(getAccessType())) {
				try {
					return ClassUtil.invokeMethodAsPrivileged(instance, getterMethod);
					//return getterFunction.apply(instance);
				} catch (Exception e) { // fallback to field access
					logger.warn("Falling back to field access for method : " + getterMethod.getName());
					return ClassUtil.invokeFieldAsPrivileged(instance, field, null, true);
				}
			} else {
				try {
					return ClassUtil.invokeFieldAsPrivileged(instance, field, null, true);
				} catch (Exception e) { // fallback to method access
					logger.warn("Falling back to method access for field : " + field.getName());
					//return getterFunction.apply(instance);
					return ClassUtil.invokeMethodAsPrivileged(instance, getterMethod);	
				}

			} 
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}	

	protected <T> void executeUpdate(Object dataObject, Object propertyValue)
	{
		// We need to work with the instance objects and not the DataObject objects
		Object instance = ClassUtil.getInstance(dataObject);
		propertyValue = ClassUtil.getInstance(propertyValue);

		// Set the value in the field  
		try {
			if(AccessType.PROPERTY.equals(getAccessType())) {
				try {
					ClassUtil.invokeMethodAsPrivileged(instance, setterMethod, propertyValue);
					//setterFunction.accept(instance, propertyValue);
				} catch (Exception e) { // fallback to field access
					if(setterMethod != null) {
						logger.warn(
							"Falling back to field access for method : " + setterMethod.getName());
					}
					ClassUtil.invokeFieldAsPrivileged(instance, field, propertyValue, false);
				}
			} else {
				try {
					ClassUtil.invokeFieldAsPrivileged(instance, field, propertyValue, false);
				} catch (Exception e) { // fallback to method access
					if(field != null) {
						logger.warn("Falling back to method access for field : " + field.getName());
					}
					//setterFunction.accept(instance, propertyValue);
					ClassUtil.invokeMethodAsPrivileged(instance, setterMethod, propertyValue);	
				}
			} 
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override
	public boolean isAlwaysInitialized() {
		return alwaysInitialized;
	}

	@Override
	public void setAlwaysInitialized(boolean alwaysInitialized) {
		this.alwaysInitialized = alwaysInitialized;
	}

	@Override
	public boolean isTransient() {	
		return (field != null) ? Modifier.isTransient(field.getModifiers()) : Modifier.isTransient(getterMethod.getModifiers());

	}	

	@Override
	public boolean isBiDirectional() {
		return getOpposite() != null;
	}

	@Override
	public boolean isSymmetricalBiDirectionalType() {
		if(!isBiDirectional())
			return false;

		ExtendedProperty opposite = (ExtendedProperty) getOpposite();

		if(!isMany() && !opposite.isMany()) 
			return this.type == opposite.getType();

		if(isMany()) {
			if(opposite.isMany()) {
				return getElementType() == opposite.getElementType();
			} else {
				return getElementType() == opposite.getType();
			}
		} else {
			if(opposite.isMany()) {
				return this.type == opposite.getElementType();
			} else {
				return this.type == opposite.getType();
			}
		}
	}

	@Override
	public void setMapPath(String mapPath) {
		this.mapPath = mapPath;
	}

	@Override
	public String getMapPath() {
		return this.mapPath;
	}

	@Override
	public boolean isDataType() {
		boolean result;
		
		if(isMany()) {
			if(getElementType() != null)
				result = getElementType().isDataType();
			else
				result = getType().isDataType();
		} else
			result = getType().isDataType();
		
		return result;
	}

	@Override
	public void linkBackPointer(BusinessObject dataObject) {
		ExtendedProperty backPointer = (ExtendedProperty) getOpposite();
		if(backPointer == null)
			return;

		BusinessObject otherSide = (BusinessObject) dataObject.getDataObject(this);
		if(otherSide == null)
			return;

		if(isMany()) {
			for(BusinessObject otherSideElement: otherSide.getList()) {
				if(backPointer.isMany()) {
					if(backPointer.isSet()) // Only Set is supported
						((Set) backPointer.getValue(otherSideElement)).add(dataObject.getInstance());
				} else
					backPointer.setValue(otherSideElement, dataObject.getInstance());
			}
		} else {
			if(backPointer.isMany()) {
				if(backPointer.isSet()) // Only Set is supported
					((Set) backPointer.getValue(otherSide)).add(dataObject.getInstance());
			} else
				backPointer.setValue(otherSide, dataObject.getInstance());
		}

	}

	@Override
	public void unlinkBackPointer(BusinessObject dataObject) {
		ExtendedProperty backPointer = (ExtendedProperty) getOpposite();
		if(backPointer == null)
			return;

		BusinessObject otherSide = (BusinessObject) dataObject.getDataObject(this);
		if(otherSide == null)
			return;

		if(isMany()) {
			for(BusinessObject otherSideElement: otherSide.getList()) {
				if(backPointer.isMany()) {
					if(backPointer.isSet()) // Only Set is supported
						((Set) backPointer.getValue(otherSideElement)).remove(dataObject.getInstance());
				} else
					backPointer.setValue(otherSideElement, null);
			}
		} else {
			if(backPointer.isMany()) {
				if(backPointer.isSet()) // Only Set is supported
					((Set) backPointer.getValue(otherSide)).remove(dataObject.getInstance());
			} else
				backPointer.setValue(otherSide, null);
		}
	}
	
	@Override
	public String getMappedByName() {	
		String result = null;

		javax.persistence.ManyToOne manyToOne = null;
		javax.persistence.OneToOne oneToOne = null;
		javax.persistence.OneToMany oneToMany = null;
		javax.persistence.ManyToMany manyToMany = null;
		
		if(getterMethod != null) {
			manyToOne = getterMethod.getAnnotation(javax.persistence.ManyToOne.class);	
			oneToOne = getterMethod.getAnnotation(javax.persistence.OneToOne.class);
			oneToMany = getterMethod.getAnnotation(javax.persistence.OneToMany.class);
			manyToMany = getterMethod.getAnnotation(javax.persistence.ManyToMany.class);
		}
		
		if(oneToOne == null && oneToMany == null && manyToMany == null && field != null) {
			manyToOne = field.getAnnotation(javax.persistence.ManyToOne.class);				
			oneToOne = field.getAnnotation(javax.persistence.OneToOne.class);
			oneToMany = field.getAnnotation(javax.persistence.OneToMany.class);
			manyToMany = field.getAnnotation(javax.persistence.ManyToMany.class);
		}
		
		if(manyToOne != null) // this annotation does not have a mappedBy field
			return result;		

		if(oneToOne != null && oneToOne.mappedBy() != null)
			result = oneToOne.mappedBy();		
		else if(oneToMany != null && oneToMany.mappedBy() != null)
			result = oneToMany.mappedBy();
		else if(manyToMany != null && manyToMany.mappedBy() != null)
			result = manyToMany.mappedBy();

		return result;
	}	
	
	private Column getColumnAnnotation() {
		javax.persistence.Column col = null;
		
		if(getterMethod != null) {
			col = getterMethod.getAnnotation(javax.persistence.Column.class);	
		}
		
		if(col == null && field != null) {
			col = field.getAnnotation(javax.persistence.Column.class);		
		}

		return col;
	}
	
	@Override
	public boolean isUnique() {
		
		// optimization
		if(unique != null) {
			return unique;
		}
		unique = Boolean.FALSE;
		
		// Check Column annotation first
		javax.persistence.Column col = getColumnAnnotation();
		
		if(col != null) {
			unique = col.unique();
		}

		// Check unique constraints
		if(!unique && getType() instanceof EntityType) {
			EntityType entityType = (EntityType) getType();
			for(Set<String> uniqueConstraint: entityType.getCandidateKeys()) {
				if(uniqueConstraint.size() == 1 && uniqueConstraint.iterator().next().equals(getName())) {
					unique = Boolean.TRUE;
					break;
				}
			}
		}

		return unique;
	}
	
	@Override
	public int getLength() {
		javax.persistence.Column col = getColumnAnnotation();
		if(col != null) {
			return col.length();
		}
		
		return StringType.DEFAULT_LENGTH;
	}
	
	@Override
	public boolean isUpdatable() {
		boolean result = true;

		javax.persistence.Column column = getterMethod.getAnnotation(javax.persistence.Column.class);
		if(column == null)
			column = field.getAnnotation(javax.persistence.Column.class);

		if(column != null && !column.updatable())
			result = false;

		return result;
	}

	public abstract void initMappedBy(DataAccessService das);

	@Override
	public boolean isApplicable(int apiVersion) {
		if(versionInfo == null || (apiVersion >= versionInfo.getFromVersion() && apiVersion <= versionInfo.getUntilVersion()) ) {
			return true;
		}
		
		return false;
	}

	@Override
	public Property getDomainProperty() {
		return this;
	}

	@Override
	public List<String> expand(Set<Type> examined) {
		List<String> result = new LinkedList<String>();
		
		if(getType() instanceof EntityType) {
			if(((EntityType)getType()).isEmbedded() && !examined.contains(getType())) {
				examined.add(getType());
				for(Property p: getType().getProperties()) {
					for(String embeddedPropertyName: p.expand(examined)) {
						result.add(getName() + Settings.PATH_DELIMITER + embeddedPropertyName);
					}
				}
			} else {
				/*
				// This can additionally contain the user key
				result.add(getName() + Settings.PATH_DELIMITER + ((EntityType)getType()).getIdentifierProperty().getName());
				if(((EntityType)getType()).getUserKey() != null) {
					result.add(getName() + Settings.PATH_DELIMITER + ((EntityType)getType()).getUserKey().getName());
				}
				*/
				result.add(Constants.XOR.OBJECTREF + getName());
			}
		} else {
			result.add(getName());
		}
		
		return result;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return relType;
	}

	@Override
	public Map<String, String> getKeyFields() {
		return keyFields;
	}

	public Set<String> getCollectionKey() {
		return this.collectionKey;
	}
	
	public void setCollectionKey(Set<String> value) {
		this.collectionKey = Collections.unmodifiableSet(value);
	}

	public void setGenerator(Generator generator) {
		this.generator = generator;
	}

	public Generator getGenerator() {
		return this.generator;
	}


	@Override
	public boolean isContainment() {
		if(isDataType()) {
			return true;
		} else {
			if(isMany()) {
				if((EntityType.class.isAssignableFrom(getElementType().getClass())))
					return ((EntityType)getElementType()).isEmbedded();
			} else if((EntityType.class.isAssignableFrom(getType().getClass()))) {
				return ((EntityType)getType()).isEmbedded();
			}
		}

		return false;
	}

	@Override
	public Map<String, Object> getConstraints() {
		return Collections.unmodifiableMap(constraints == null ? new HashMap<String, Object>() : constraints);
	}

	public void addConstraint(String key, Object value) {
		if(constraints == null) {
			constraints = new HashMap<String, Object>();
		}
		constraints.put(key, value);
	}

	@Override
	public boolean isManaged() {
		return false;
	}
}
