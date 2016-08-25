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

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import tools.xor.annotation.XorAfter;
import tools.xor.annotation.XorEntity;
import tools.xor.annotation.XorInput;
import tools.xor.annotation.XorOutput;
import tools.xor.annotation.XorPromise;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;
import tools.xor.util.DFAtoRE;
import tools.xor.view.AggregateView;

public abstract class AbstractType implements EntityType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final int ALL = -1;
	
	private TypeMapper              typeMapper;   // Used to get the derivedClass from the referenceClass and vice versa
	private ClassResolver           classResolver;
	private boolean                 immutable;
	private boolean                 aggregate;    // Does this type represent an aggregate according to the model - marked by the Aggregate annotation
	protected Map<String, Property> properties;   // TODO: synchronize if properties are removed in future
	protected EntityType            rootEntityType;
	private   Set<EntityType>       subTypes;	
	private Map<Integer, List<Property>> propertiesByVersion = new Int2ObjectOpenHashMap<List<Property>>(); // properties by version
	private int                     order; //represents the topological sort order of the entity type
	private EntityType              superType;
	
	private Map<String, Method>     readerMethods    = new HashMap<String, Method>();
	private Map<String, Method>     updaterMethods   = new HashMap<String, Method>();	
	private Map<String, Field>      fields           = new HashMap<String, Field>();
	private Map<String, Annotation> classAnnotations = new HashMap<String, Annotation>();
	
	// Cached Annotations used to enforce business logic in the model
	volatile private Map<String, List<MethodInfo>> promises = new HashMap<String, List<MethodInfo>>();
	
	private Set<MethodInfo> postLogicMethods = new HashSet<MethodInfo>(); // includes PostLogic annotated methods of ancestors

	private static final HashSet<Class<?>> WRAPPER_TYPES = getWrapperTypes();
	private static final HashSet<Class<?>> BASIC_TYPES = getBasicTypes();

	private DataAccessService das;

	public AbstractType() {
		classResolver = new ClassResolver(this);
	}
	
	public void init() {
		initMeta();
		initImmutable();
	}
	
	protected void initMeta() {
		initGetterMethods();
		initSetterMethods();
		initFields();
		initClassAnnotations();
		initPostLogic();
		initPromises();
	}

	@Override
	public void setDAS(DataAccessService das) {
		this.das = das;
	}
	
	@Override
	public Set<String> getInitializedProperties() {
		Set<String> result = new HashSet<String>();
		
		if(getIdentifierProperty() != null) {
			result.add(getIdentifierProperty().getName());
		}

		for(Property property: getProperties()) {

			if( ((ExtendedProperty)property).needsInitialization() && property.getType().isDataType()) {
				result.add(property.getName());
			}
		}
		
		return result;
	}
	
	public int getOrder() {
		return this.order;
	}
	
	public void setOrder(int value) {
		this.order = value;
	}

	public EntityType getRootEntityType() {
		return rootEntityType;
	}
	
	public static String getViewName(Type type) {
		return ClassUtil.getBucketName(type.getInstanceClass());
	}
	
	public static String getBaseViewName(Type type) {
		StringBuilder sb = new StringBuilder(ClassUtil.getBucketName(type.getInstanceClass()));
		sb.append(Settings.URI_PATH_DELIMITER);
		sb.append(AggregateView.BASE);
		
		return sb.toString();
	}	
	
	@Override
	public void defineSubtypes(List<Type> types) {
		subTypes = new HashSet<EntityType>();
		
		for(Type type: types) {
			if(type instanceof EntityType) {
				if (this.getInstanceClass().isAssignableFrom(type.getInstanceClass()) &&
					this.getInstanceClass() != type.getInstanceClass()) {
					subTypes.add((EntityType) type);
				}
			}
		}
	}
	
	@Override
	public EntityType getSuperType() {
		return this.superType;
	}
	
	@Override
	public void setSuperType(EntityType value) {
		this.superType = value;
	}	
	
	@Override
	public Set<EntityType> getSubtypes() {
		if(subTypes == null) {
			defineSubtypes(das.getTypes());
		}

		return subTypes;
	}

	protected Method getPolymorphicGetterMethod (String property)
	{
		Class<?> instanceClass = getInstanceClass();
		if (instanceClass.isInterface())
			return null;
		while (instanceClass != Object.class) {
			Method[] ma = instanceClass.getDeclaredMethods();

			for (int i = ma.length - 1; i > -1; i--) {
				Method m = ma[i];

				if (m.getAnnotation(XorPromise.class) != null) {

					if (m.getParameterTypes().length != 0) {
						if (!validateParamAnnotations(m)) {
							continue;
						}
					}

					XorPromise p = m.getAnnotation(XorPromise.class);
					if (p.property().equals(property) &&
							p.capture() &&
							hasReadAction(p.action())) {
						return m;
					}
					else {
						continue;
					}
				}
				else {
					continue;
				}
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}

		return null;
	}
	
	private boolean hasReadAction(AggregateAction[] actions) {
		for(AggregateAction a: actions) {
			if(a == AggregateAction.READ) {
				return true;
			}
		}
		
		return false;
	}

	protected Method getPolymorphicSetterMethod (String property)
	{
		Class<?> instanceClass = getInstanceClass();
		if (instanceClass.isInterface())
			return null;
		while (instanceClass != Object.class) {
			Method[] ma = instanceClass.getDeclaredMethods();

			for (int i = ma.length - 1; i > -1; i--) {
				Method m = ma[i];

				if (m.getAnnotation(XorPromise.class) != null) {

					if (m.getParameterTypes().length != 0) {
						if (!validateParamAnnotations(m)) {
							continue;
						}
					}

					XorPromise dataUpdate = m.getAnnotation(XorPromise.class);
					if (dataUpdate.property().equals(property)) {
						return m;
					}
					else {
						continue;
					}
				}
				else {
					continue;
				}
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}

		return null;
	}

	
	protected void initGetterMethods() {
		// Get all methods declared by the class or interface.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Method> map = new HashMap<String, Method>();

		Class<?> instanceClass = getInstanceClass();
		if(instanceClass.isInterface())
			return;
		while (instanceClass != Object.class) {
			Method[] ma = instanceClass.getDeclaredMethods();

			for (int i=ma.length-1; i > -1; i--) {
				Method m = ma[i];

				if(!isGetterMethod(instanceClass, m))
					continue;
				if (m.getParameterTypes().length != 0)
					continue;
				if (Modifier.isStatic(m.getModifiers()))
					continue;

				final String propertyString = getGetterProperty(m);
				String propertyName = Introspector.decapitalize(propertyString); 
				map.put(propertyName, m);
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}
		readerMethods = Collections.unmodifiableMap(map);
	}	
	
	/**
	 * 	If the field a boolean (primitive type), then only "is<propertyName>" is a valid getter method
	 *  If a "get<propertyName>" method is present for this type then we override it 
	 * @param m
	 * @return
	 */
	private boolean isGetterMethod(Class<?> beanClass, Method m) {
		Class<?> returnType = m.getReturnType();
		if(returnType == boolean.class) {
			return m.getName().startsWith("is");
		} else {
			boolean result = m.getName().startsWith("get");
			if(!result && returnType == Boolean.class) { // fallback to is getter
				result = m.getName().startsWith("is");
			}
			return result;
		}
	}

	public String getGetterProperty(Method m) {
		return (m.getName().startsWith("get")) ? m.getName().substring("get".length()) : m.getName().substring("is".length());
	}	

	@Override
	public Method getGetterMethod(String targetProperty){
		if(readerMethods.isEmpty()) {
			logger.warn("Getter methods cache is not yet initialized.");
		}
		targetProperty = Introspector.decapitalize(targetProperty);
		return readerMethods.get(targetProperty);
	}
	
	protected void initSetterMethods() {

		// Get all methods declared by the class or interface.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Method> map = new HashMap<String, Method>();

		Class<?> instanceClass = getInstanceClass();
		if(instanceClass.isInterface())
			return;
		while (instanceClass != Object.class) {
			Method[] ma = instanceClass.getDeclaredMethods();

			for (int i=ma.length-1; i > -1; i--) {
				Method m = ma[i];

				if (!m.getName().startsWith("set"))
					continue;
				if (m.getParameterTypes().length != 1)
					continue;
				final int mod = m.getModifiers();

				if (Modifier.isStatic(mod))
					continue;

				// Adds the specified element to the set if it is not already present
				final String propertyString = m.getName().substring("set".length());
				String propertyName = Introspector.decapitalize(propertyString);
				map.put(propertyName, m);
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}
		updaterMethods = Collections.unmodifiableMap(map);
	}		

	@Override
	public Method getSetterMethod(String targetProperty){
		return updaterMethods.get(targetProperty);
	}	
	
	public void initFields() {

		// Get all fields declared by the class or interface.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Field> map = new HashMap<String, Field>();

		Class<?> instanceClass = getInstanceClass();
		if(instanceClass.isInterface())
			return;
		while (instanceClass != Object.class) {
			Field[] fa = instanceClass.getDeclaredFields();

			for (int i=fa.length-1; i > -1; i--) {
				Field f = fa[i];

				final int mod = f.getModifiers();
				f.setAccessible( true );

				if (Modifier.isStatic(mod))
					continue;

				map.put(f.getName(), f); 
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}
		fields = Collections.unmodifiableMap(map);
	}	

	@Override
	public Field getField(String targetProperty){
		return fields.get(targetProperty);
	}	

	/**
	 * Initialize both the root entity type and also the parent entity type
	 * @param das
	 */
	public void initRootEntityType(DataAccessService das) {
		Class<?> rootEntityClass = getInstanceClass();

		Class<?> parentClass = rootEntityClass.getSuperclass();		
		while(parentClass != null) {
			Annotation annotation = getClassAnnotation(das, parentClass, Entity.class);

			if(annotation != null && annotation.annotationType() == Entity.class) {
				Entity entity = (Entity) annotation;
				if(entity.name() != null && !"".equals(entity.name()))
					rootEntityClass = parentClass;
			}

			parentClass = parentClass.getSuperclass();
		}

		rootEntityType = (EntityType) das.getType(rootEntityClass);
	}	
	
	protected void initClassAnnotations() {

		// Get all annotations declared by the class.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Annotation> map = new HashMap<String, Annotation>();

		Class<?> instanceClass = getInstanceClass();
		if(instanceClass.isInterface())
			return;

		while (instanceClass != Object.class) {
			Annotation[] annotations = instanceClass.getAnnotations();
			for (int i=annotations.length-1; i > -1; i--) {
				Annotation a = annotations[i];
				map.put(a.annotationType().getName(), a);
			}

			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}
		classAnnotations = Collections.unmodifiableMap(map);
	}
	
	@Override
	public Annotation getClassAnnotation(Class<?> annotationClass) {	
		return classAnnotations.get(annotationClass.getName());
	}	
	
	public Annotation getClassAnnotation(DataAccessService das, Class<?> targetClass, Class<?> annotationClass) {
		Type type = das.getType(targetClass);
		if(type != null && EntityType.class.isAssignableFrom(type.getClass())) {
			EntityType targetType = (EntityType) type;
			return targetType.getClassAnnotation(annotationClass);
		}
		
		return null;
	}
	
	protected void initPostLogic() {	

			Set<MethodInfo> methods = new HashSet<MethodInfo>();			
			Class<?> instanceClass = getInstanceClass();
			for(Method method: instanceClass.getMethods()) {
				if(method.getAnnotation(XorAfter.class) != null) {
					if(method.getParameterTypes().length != 0) {
						if(!validateParamAnnotations(method)) {
							continue;
						}
					}
					
					XorAfter postLogic = method.getAnnotation(XorAfter.class);
					MethodInfo methodInfo = new MethodInfo(
							0,
							postLogic.fromVersion(), 
							postLogic.untilVersion(), 
							method,
							false,
							postLogic.action(),
							postLogic.tag(),
							null,
							null);
					methods.add(methodInfo);
				}
			}
			postLogicMethods = Collections.unmodifiableSet(methods);
	}
	
	private List<MethodInfo> getMethods(Map<String, List<MethodInfo>> allMethods, String property) {
		List<MethodInfo> methods = allMethods.get(property);
		if(methods == null) {
			methods = new LinkedList<MethodInfo>();
			allMethods.put(property, methods);
		}
		return methods;
	}
	
	/**
	 * Check that there is only one method for a particular version, i.e., the version range
	 * for the methods do not overlap
	 *
	 * @param allMethods
	 * @param property
	 * @param methodInfo
	 */
	private boolean addUniqueMethod(Map<String, List<MethodInfo>> allMethods, String property, MethodInfo methodInfo) {
		List<MethodInfo> methods = getMethods(allMethods, property);
		for(MethodInfo mi: methods) {
			if(methodInfo.doOverlap(mi)) {
				logger.error(
					"The two methods overlap either by their tags, actions or version range should be unique for method. Will go only with the most specific one by inheritance hierarchy: "
						+ methodInfo.getMethod().getDeclaringClass().getName() + "#" +
						methodInfo.getMethod().getName());
				return false;
			}
		}
		
		methods.add(methodInfo);
		return true;
	}

	private boolean validateParamAnnotations(Method method) {
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		boolean foundParamAnnotation = false;
		boolean foundParamNoAnnotation = false;

		// check that each of the paramAnnotations array has either an Input/Output annotation but not both
		for(Annotation[] paramA: paramAnnotations) {
			boolean found = false;
			for(Annotation annotation: paramA) {
				if(XorInput.class.isAssignableFrom(annotation.getClass()) ||
						XorOutput.class.isAssignableFrom(annotation.getClass())) {
					if(!found) {
						found = true;
						foundParamAnnotation = true;
					} else {
						throw new RuntimeException("A parameter cannot be annotated with both Input and Output annotations");
					}
				}
			}
			if(!found) {
				foundParamNoAnnotation = true;
			}
		}

		if(foundParamAnnotation && foundParamNoAnnotation) {
			logger.warn("The business logic method " + method.getName() + " should have Input/Output parameter annotations on all its parameters to indicate how the param values need to be populated");
		}
		
		if(!Modifier.isStatic(method.getModifiers())) {
			throw new RuntimeException("The business logic method " + method.getDeclaringClass().getName() + "#" + method.getName() + " should be static");
		}

		return foundParamAnnotation;
	}

	protected void initPromises() {	

		HashMap<String, List<MethodInfo>> allMethods = new HashMap<String, List<MethodInfo>>();				
		Class<?> instanceClass = getInstanceClass();
		Set<String> notUnique = new HashSet<String>();
		for(Method method: instanceClass.getMethods()) {
			if(method.getAnnotation(XorPromise.class) != null) {

				if(method.getParameterTypes().length != 0) {
					if(!validateParamAnnotations(method)) {
						continue;
					}
				}

				XorPromise promise = method.getAnnotation(XorPromise.class);
				if(notUnique.contains(promise.property())) {
					// already recorded
					continue;
				}
				MethodInfo methodInfo = new MethodInfo(
					promise.order(),
					promise.fromVersion(), 
					promise.untilVersion(),
					method,
					promise.capture(),
					promise.action(),
					promise.tag(),
					promise.phase(),
					promise.stage());
				if(!addUniqueMethod(allMethods, promise.property(), methodInfo)) {
					notUnique.add(promise.property());
				}
			}
		}
		// Get the polymorphic method
		for(String property: notUnique) {
			Method method = getPolymorphicGetterMethod(property);
			XorPromise dataUpdate = method.getAnnotation(XorPromise.class);
			MethodInfo methodInfo = new MethodInfo(
				dataUpdate.order(),
				dataUpdate.fromVersion(),
				dataUpdate.untilVersion(),
				method,
				dataUpdate.capture(),
				dataUpdate.action(),
				dataUpdate.tag(),
				dataUpdate.phase(),
				dataUpdate.stage());
			addUniqueMethod(allMethods, dataUpdate.property(), methodInfo);
		}

		promises = Collections.unmodifiableMap(allMethods);
	}

	@Override
	public List<MethodInfo> getPromises(String targetProperty) {
		return promises.get(targetProperty);
	}

	public static boolean isWrapperType(Class<?> clazz)
	{
		return WRAPPER_TYPES.contains(clazz);
	}

	private static HashSet<Class<?>> getWrapperTypes()
	{
		HashSet<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);
		return ret;
	}

	private static HashSet<Class<?>> getBasicTypes()
	{
		HashSet<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(String.class);
		ret.add(java.math.BigDecimal.class);
		ret.add(java.math.BigInteger.class);
		ret.add(java.sql.Date.class);
		ret.add(java.sql.Time.class);
		ret.add(java.sql.Timestamp.class);
		ret.add(java.util.Date.class);
		return ret;
	}    	

	protected void initImmutable() {
		Annotation annotation = getClassAnnotation(XorEntity.class);
		if(annotation != null && annotation.annotationType() == XorEntity.class) {
			boolean value = ((XorEntity)annotation).immutable();
			if(value)
				this.immutable = value;
		}	
	}	

	@Override
	public boolean isImmutable() {
		return immutable;
	}
	
	@Override
	public boolean isAggregate() {
		return aggregate;
	}	

	public static boolean isBasicType(Class<?> clazz) {
		return clazz.isPrimitive() || isWrapperType(clazz) || BASIC_TYPES.contains(clazz);
	}

	@Override
	public boolean isDataType(Object obj) {
		return isDataType();
	}

	@Override
	public boolean isDomainType() {
		return true;
	}

	@Override
	public EntityType getDomainType() {
		return this;
	}

	@Override
	public TypeMapper getTypeMapper() {
		return typeMapper;
	}

	public void setTypeMapper(TypeMapper typeMapper) {
		this.typeMapper = typeMapper;
	}


	@Override
	public boolean isDataType() {
		return properties.size() == 0;
	}

	@Override
	public ClassResolver getClassResolver() {
		return classResolver;
	}

	public void setClassResolver(ClassResolver classResolver) {
		this.classResolver = classResolver;
	}	

	@Override
	public List<Property> getProperties() {
		return getProperties(ALL);
	}
	
	/**
	 * Filter the properties by the API version
	 *
	 * @param input
	 * @param apiVersion
	 * @return
	 */
	List<Property> getProperties(Collection<Property> input, int apiVersion) {
		List<Property> result = new ArrayList<Property>(input.size());
		for(Property p: input) {
			ExtendedProperty property = (ExtendedProperty) p;
			if(property.isApplicable(apiVersion)) {
				Property resolvedProperty = getProperty(property.getName());
				if(resolvedProperty == null) { 
					logger.warn("Could not find property: " + p.getName() + " in type " + getName());
					continue;
				}
				result.add(resolvedProperty);
			}
		}
		
		return result;
	}
	
	@Override
	/**
	 * We will not automatically sort as this involves additional resources that the 
	 * user should not pay if not needed.
	 * So the user will take of invoking the business logic in the correct order.
	 */
	public List<Property> getProperties(int apiVersion) {
		if(propertiesByVersion.containsKey(apiVersion) ) {
			return propertiesByVersion.get(apiVersion);
		}

		List<Property> result = null;
		if(apiVersion != ALL) {
			result = getProperties(properties.values(), apiVersion);
			propertiesByVersion.put(apiVersion, result);
		} else {
			result = new ArrayList<Property>(properties.values());
			propertiesByVersion.put(ALL, result);
		}

		return result;
	}

	/**
	 * This method is not synchronized as we do not remove elements from a Map
	 */
	@Override
	public void addProperty(Property property) {
		properties.put(property.getName(), property);

		// Clear the property version map so it gets rebuilt
		propertiesByVersion.clear();
	}

	@Override
	public Property getProperty(String path) {
		int delim = path.indexOf(Settings.PATH_DELIMITER);

		Property result = null;
		if(delim == -1) {
			if(properties == null) {
				throw new IllegalStateException("Properties not set for type: " + getName() + " with class: " + getInstanceClass().getName());
			}
			result = properties.get(path);
		} else {
			Property property = getProperty(path.substring(0, delim));
			if(property == null && path.contains(DFAtoRE.RECURSE_SYMBOL)) {
				throw new RuntimeException("Recursive references currently not supported");
			}
			Type propertyType = property.getType();
			if(property.isMany())
				propertyType = ((ExtendedProperty)property).getElementType();
			result = propertyType.getProperty(path.substring(delim+1));
		}

		if (result != null) {
			return result;
		}

		return null;
	}	
	
	@Override
	public Property getPropertyByAlias(String name) {
		for(Property property: properties.values()) {
			if(property.getAliasNames().contains(name))
				return property;
		}
		
		return null;
	}
	
	@Override
	public void initPositionProperty() {
		for(Property property: properties.values())
			((ExtendedProperty)property).initPositionProperty();
	}	

	@Override
	public List<Type> getEmbeddableTypes() {
		return new ArrayList<Type>();
	}

	@Override
	public void invokePostLogic(Settings settings, Object instance) {
		for(MethodInfo methodInfo: postLogicMethods) {
			try {
				if(methodInfo.isRelevant(settings)) {
					Object[] args = {};
					ClassUtil.invokeMethodAsPrivileged(instance, methodInfo.getMethod(), args);
				}
			} catch (Exception e) {
				throw ClassUtil.wrapRun(e);
			}
		}
	}
	
	@Override
	public Property getUserKey() {

		String userKeyString = null;
		Annotation annotation = getClassAnnotation(XorEntity.class);
		if(annotation != null && annotation.annotationType() == XorEntity.class) {
			userKeyString = ((XorEntity)annotation).userKeyProperty();	
		}
		return (userKeyString != null) ? getProperty(userKeyString) : null;	
	}

	@Override
	public Property getCollectionUserKey() {
		String collUserKeyString = null;
		
		Annotation annotation = getClassAnnotation(XorEntity.class);
		if(annotation != null && annotation.annotationType() == XorEntity.class) {
			if( ((XorEntity)annotation).collectionUserKey() == true)
				collUserKeyString = ((XorEntity)annotation).userKeyProperty();	
		}
		return (collUserKeyString != null) ? getProperty(collUserKeyString) : null;
	}	
	
	@Override
	public int compareTo(EntityType o) {
		return this.getOrder() - o.getOrder();
	}
	
	@Override
	public Object newInstance(Object instance) {
		 return ClassUtil.newInstance(getInstanceClass());
	}	
}
