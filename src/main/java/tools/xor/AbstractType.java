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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.annotation.XorAfter;
import tools.xor.annotation.XorDataService;
import tools.xor.annotation.XorDomain;
import tools.xor.annotation.XorEntity;
import tools.xor.annotation.XorExternal;
import tools.xor.annotation.XorExternalData;
import tools.xor.annotation.XorLambda;
import tools.xor.generator.Generator;
import tools.xor.generator.LinkedChoices;
import tools.xor.service.Shape;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoRE;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.ViewType;

public abstract class AbstractType implements EntityType, Cloneable, Comparable<EntityType> {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final int ALL = -1;
	
	private ClassResolver       classResolver;
	private boolean             immutable;

	// Need to only contain type name, as the actual type should be resolved dynamically
	protected String            rootEntityType;
	protected Set<String>       subTypes;
	protected Set<String>       childTypes; // immediate sub types

	private   String            superTypeName;
	private   EntityType        superType;
	private   int               order; //represents the topological sort order of the entity type
	protected List<String>      naturalKey;
	protected List<String>      expandedNaturalKey;
	private   String            domainTypeName;
	
	private Map<String, Method>     readerMethods    = new HashMap<String, Method>();
	private Map<String, Method>     updaterMethods   = new HashMap<String, Method>();	
	private Map<String, Field>      fields           = new HashMap<String, Field>();
	private Map<String, Annotation> classAnnotations = new HashMap<String, Annotation>();
	
	// Cached Annotations used to enforce business logic in the model
	volatile private Map<String, List<MethodInfo>> lambdas = new HashMap<String, List<MethodInfo>>();
	
	private Set<MethodInfo> postLogicMethods = new HashSet<MethodInfo>(); // includes PostLogic annotated methods of ancestors

	private static final HashSet<Class<?>> WRAPPER_TYPES = getWrapperTypes();
	private static final HashSet<Class<?>> BASIC_TYPES = getBasicTypes();

	private Shape shape;

	private List<GeneratorDriver> entityGenerators = new LinkedList<>();

	public AbstractType() {
		classResolver = new ClassResolver(this);
	}
	
	public void init() {
		initMeta();
		initXorEntity();
	}
	
	protected void initMeta() {
		if(isOpen()) {
			return;
		}

		initGetterMethods();
		initSetterMethods();
		initFields();
		initClassAnnotations();
		initPostLogic();
		this.lambdas = initLambdas(getInstanceClass());
	}

	@Override
	public Shape getShape() {
		return this.shape;
	}

	@Override
	public void setShape(Shape shape) {
		assert(this.shape == null || this.shape == shape);

		this.shape = shape;
	}
	
    @Override
    public EntityType copy(Shape shape) {
        try {
            AbstractType copy = (AbstractType) super.clone();
            
            // Fix any mutable objects
            copy.entityGenerators = new LinkedList<>();
            copy.shape = shape;
            shape.addType(copy);
            
            // Also make a copy of the properties
            for(Property p: getProperties()) {
                Property pCopy = ((ExtendedProperty)p).copy(copy);
                shape.addProperty(pCopy);
            }
            
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }	
	
	@Override
	public Set<String> getInitializedProperties() {
		Set<String> result = new HashSet<String>();
		
		if(getIdentifierProperty() != null) {
			result.add(getIdentifierProperty().getName());
		}

		for(Property property: getProperties()) {

			if( ((ExtendedProperty)property).isAlwaysInitialized() && property.getType().isDataType()) {
				result.add(property.getName());
			}
		}
		
		return result;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setOrder(int value) {
		this.order = value;
	}

	@Override
	public EntityType getRootEntityType() {
		if(rootEntityType == null) {
			rootEntityType = getEntityName();
		}
		return (EntityType)getShape().getType(this.rootEntityType);
	}
	
	@Override
	public void setRootEntityType(String value) {
	    this.rootEntityType = value;
	}

	private static boolean isInValid(Type type) {
		return type == null || type.getInstanceClass() == null;
	}

	private static boolean isInValid(Type type, Property property) {
		return type == null || type.getInstanceClass() == null || property == null;
	}
	
	public static String getViewName(Type type) {
		if(isInValid(type)) {
			return null;
		}

		StringBuilder sb = new StringBuilder(ClassUtil.getBucketName(type));
		sb.append(Settings.URI_PATH_DELIMITER);
		sb.append(ViewType.AGGREGATE);

		return sb.toString();
	}
	
	public static String getBaseViewName(Type type) {
		if(isInValid(type)) {
			return null;
		}

		StringBuilder sb = new StringBuilder(ClassUtil.getBucketName(type));
		sb.append(Settings.URI_PATH_DELIMITER);
		sb.append(ViewType.BASE);
		
		return sb.toString();
	}

	public static String getMigrateViewName(Type type) {
		if(isInValid(type)) {
			return null;
		}

		StringBuilder sb = new StringBuilder(ClassUtil.getBucketName(type));
		sb.append(Settings.URI_PATH_DELIMITER);
		sb.append(ViewType.MIGRATE);

		return sb.toString();
	}

	public static String getRelationshipViewName(Type type, Property property) {
		if(isInValid(type, property)) {
			return null;
		}

		StringBuilder sb = new StringBuilder(ClassUtil.getBucketName(type));
		sb.append("#");
		sb.append(property.getName());
		sb.append(Settings.URI_PATH_DELIMITER);
		sb.append(ViewType.RELATIONSHIP);

		return sb.toString();
	}

	public static String getRefViewName(Type type) {
		if(isInValid(type)) {
			return null;
		}

		StringBuilder sb = new StringBuilder(ClassUtil.getBucketName(type));
		sb.append(Settings.URI_PATH_DELIMITER);
		sb.append(ViewType.REF);

		return sb.toString();
	}
	
	@Override
	public void defineSubtypes(List<Type> types) {
		subTypes = new HashSet<String>();

		if(this.getInstanceClass() == null) {
			return;
		}

		for(Type type: types) {
			if(type instanceof EntityType) {
				if(type.isOpen() || type.getInstanceClass() == null) {
					continue;
				}
				if (this.getInstanceClass().isAssignableFrom(type.getInstanceClass()) &&
					this.getInstanceClass() != type.getInstanceClass()) {
					subTypes.add(((EntityType)type).getEntityName());
				}
			}
		}
	}

	@Override
	public void defineChildTypes() {
		Map<Class, EntityType> subTypeMap = new HashMap<>();
		for(EntityType entityType: getSubtypes()) {
			subTypeMap.put(entityType.getInstanceClass(), entityType);
		}

		// Check child/immediate subTypes
		childTypes = new HashSet<String>();
		next: for(EntityType subType: getSubtypes()) {
			Class subTypeInstanceClass = subType.getInstanceClass().getSuperclass();
			while(getInstanceClass() != subTypeInstanceClass) {
				// If this is not an immediate subType then skip
				if(subTypeMap.containsKey(subTypeInstanceClass)) {
					continue next;
				}
				// Go up the inheritance hierarchy
				subTypeInstanceClass = subTypeInstanceClass.getSuperclass();
			}
			// We should be at the root of the inheritance hierarchy. Just confirm to be safe.
			if(getInstanceClass() == subTypeInstanceClass) {
				childTypes.add(subType.getEntityName());
			}
		}
	}
	
	@Override
	public EntityType getParentType() {

		if(this.superTypeName == null) {
			return null;
		}

		if(superType == null) {
			superType = (EntityType)getShape().getType(superTypeName);
		}

		return superType;
	}
	
	@Override
	public void setParentType(EntityType value) {
		this.superTypeName = value.getEntityName();
	}
	
    @Override
    public List<? extends Type> getParentTypes() {
        List<Type> parentTypes = new ArrayList<>();
        Type parentType = getParentType();
        if(parentType != null) {
            parentTypes.add(parentType);
        }
        
        return parentTypes;
    }
	
	@Override
	public Set<EntityType> getSubtypes() {
		if(subTypes == null) {
			List allTypes = new ArrayList<>(getShape().getUniqueTypes());
			if(getShape().getShapeInheritance() == Shape.Inheritance.REFERENCE && getShape().getParent() != null) {
				allTypes.addAll(getShape().getParent().getUniqueTypes());
			}
			defineSubtypes(allTypes);
		}

		Set<EntityType> result = new HashSet<>();
		for(String entityName: subTypes) {
			result.add((EntityType)getShape().getType(entityName));
		}

		return result;
	}

	@Override
	public Set<EntityType> getChildTypes() {
		if(childTypes == null) {
			// first ensure that the subTypes are initialized
			getSubtypes();
			defineChildTypes();
		}

		Set<EntityType> result = new HashSet<>();
		for(String entityName: childTypes) {
			result.add((EntityType)getShape().getType(entityName));
		}
		return result;
	}

	protected static Method getPolymorphicGetterMethod (String property, Class<?> instanceClass)
	{
		if (instanceClass.isInterface())
			return null;
		while (instanceClass != Object.class) {
			Method[] ma = instanceClass.getDeclaredMethods();

			for (int i = ma.length - 1; i > -1; i--) {
				Method m = ma[i];

				if (m.getAnnotation(XorLambda.class) != null) {

					if (m.getParameterTypes().length != 0) {
						if (!validateParamAnnotations(m)) {
							continue;
						}
					}

					XorLambda p = m.getAnnotation(XorLambda.class);
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
	
	public static boolean hasReadAction(AggregateAction[] actions) {
		for(AggregateAction a: actions) {
			if(a == AggregateAction.READ) {
				return true;
			}
		}
		
		return false;
	}

	@Override public List<GeneratorDriver> getGenerators ()
	{
		return this.entityGenerators;
	}

	@Override public void addGenerator (GeneratorDriver generator)
	{
		this.entityGenerators.add(generator);
	}

	@Override public void clearGenerators() {
		this.entityGenerators.clear();
		
		// We also unset all the generators for the properties of this type
		for(Property p: getProperties()) {
		    ((ExtendedProperty)p).setGenerator(null);
		}
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

				if (m.getAnnotation(XorLambda.class) != null) {

					if (m.getParameterTypes().length != 0) {
						if (!validateParamAnnotations(m)) {
							continue;
						}
					}

					XorLambda dataUpdate = m.getAnnotation(XorLambda.class);
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
		if(!isValidInstanceClass(instanceClass))
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

				final String propertyString = getGetterPropertyName(m);
				String propertyName = decapitalize(propertyString);
				map.put(propertyName, m);
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}
		readerMethods = Collections.unmodifiableMap(map);
	}	
	
	/**
	 * 	If the field a boolean (primitive type), then only "is...()" is a valid getter method
	 *  If a "get...()" method is present for this type then we override it
	 *
	 *  Providers can override the way a getter method is identified.
	 *
	 * @param beanClass the java class
	 * @param m the getter method
	 * @return true if it is a getter method
	 */
	protected boolean isGetterMethod(Class<?> beanClass, Method m) {
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

	public String getGetterPropertyName(Method m) {
		return (m.getName().startsWith("get")) ? m.getName().substring("get".length()) : m.getName().substring("is".length());
	}

	public String getSetterPropertyName(Method m) {
		return m.getName().substring("set".length());
	}

	@Override
	public Method getGetterMethod(String targetProperty){
		if(readerMethods.isEmpty()) {
			logger.warn("Getter methods cache is not yet initialized.");
		}
		targetProperty = decapitalize(targetProperty);
		return readerMethods.get(targetProperty);
	}

	protected String decapitalize (String propertyName) {
		return Introspector.decapitalize(propertyName);
	}

	@Override
	public Method getSetterMethod(String targetProperty){
		targetProperty = Introspector.decapitalize(targetProperty);
		return updaterMethods.get(targetProperty);
	}

	protected boolean isSetterMethod(Class<?> beanClass, Method m) {

		return m.getName().startsWith("set");
	}

	/**
	 * This requires the initGetterMethods to be invoked first
	 */
	protected void initSetterMethods() {

		assert(readerMethods != null) : "initGetterMethods need to be invoked first";

		// Get all methods declared by the class or interface.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Method> map = new HashMap<String, Method>();

		Class<?> instanceClass = getInstanceClass();
		if(!isValidInstanceClass(instanceClass))
			return;
		while (instanceClass != Object.class) {
			Method[] ma = instanceClass.getDeclaredMethods();

			for (int i=ma.length-1; i > -1; i--) {
				Method m = ma[i];

				if (!isSetterMethod(instanceClass, m))
					continue;
				if (m.getParameterTypes().length != 1)
					continue;
				final int mod = m.getModifiers();

				if (Modifier.isStatic(mod))
					continue;

				final String propertyString = getSetterPropertyName(m);
				String propertyName = decapitalize(propertyString);

				// Find the return type of the corresponding getter method
				// Since there can be multiple "setters" with the same name
				// We need the return type from the getter to resolve this situation
				Method getterMethod = readerMethods.get(propertyName);
				if (getterMethod != null) {
					Method setterMethod;
					try {
						setterMethod = instanceClass.getDeclaredMethod(m.getName(), getterMethod.getReturnType());
					}
					catch (NoSuchMethodException e) {
						// Cannot find the corresponding setter so skip this method
						continue;
					}

					if (!setterMethod.equals(m)) {
						// This is not the correct setter for the getter
						continue;
					}
				}

				// Adds the specified element to the set if it is not already present
				map.put(propertyName, m);
			}
			// climb to the super class and repeat
			instanceClass = instanceClass.getSuperclass();
		}
		updaterMethods = Collections.unmodifiableMap(map);
	}
	
	public void initFields() {

		// Get all fields declared by the class or interface.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Field> map = new HashMap<String, Field>();

		Class<?> instanceClass = getInstanceClass();
		if(!isValidInstanceClass(instanceClass))
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
	 * Initialize the root superType
	 */
	public void initRootEntityType() {

		EntityType potentialRootEntityType = this;

		while(potentialRootEntityType.getParentType() != null) {
			potentialRootEntityType = potentialRootEntityType.getParentType();
		}

		this.rootEntityType = potentialRootEntityType.getEntityName();
	}

	public void unfoldProperties (Shape shape)
	{
		initRequiredProperties(shape);
	}

	/**
	 * Overridden by providers that needs required properties such as id and version fields
	 * to be initialized after the unfolding process.
	 *
	 * @param shape of the type
	 */
	protected void initRequiredProperties(Shape shape) {}
	
	@Override
	public List<Set<String>> getCandidateKeys() {
		
		Class<?> instanceClass = getInstanceClass();	
		Table table = null;
		do {
			table = (Table) getClassAnnotation(getShape(), instanceClass, Table.class);
			if(table != null && table.annotationType() == Table.class) {
				break;
			}

			instanceClass = instanceClass.getSuperclass();
		} while (table == null && instanceClass != null);
		
		UniqueConstraint[] ucs = table.uniqueConstraints();
		List<Set<String>> result = new ArrayList<>(ucs.length);
		for(UniqueConstraint uc: ucs) {
			result.add(new HashSet<String>(Arrays.asList(uc.columnNames())));
		}
		
		return result;
	}

	private static boolean isValidInstanceClass(Class<?> instanceClass) {
		return !instanceClass.isInterface();
	}
	
	protected void initClassAnnotations() {

		// Get all annotations declared by the class.
		// This includes public, protected, default (package) access, 
		// and private methods, but excludes inherited methods.
		Map<String, Annotation> map = new HashMap<String, Annotation>();

		Class<?> instanceClass = getInstanceClass();
		if(!isValidInstanceClass(instanceClass))
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
	
	public Annotation getClassAnnotation(Shape shape, Class<?> targetClass, Class<?> annotationClass) {
		Type type = shape.getType(targetClass);
		if(type != null && EntityType.class.isAssignableFrom(type.getClass())) {
			EntityType targetType = (EntityType) type;
			return targetType.getClassAnnotation(annotationClass);
		}
		
		return null;
	}
	
	protected void initPostLogic() {	

			Set<MethodInfo> methods = new HashSet<MethodInfo>();			
			Class<?> instanceClass = getInstanceClass();
			if(!isValidInstanceClass(instanceClass))
				return;
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
	
	private static List<MethodInfo> getMethods(Map<String, List<MethodInfo>> allMethods, String property) {
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
	private static boolean addUniqueMethod(Map<String, List<MethodInfo>> allMethods, String property, MethodInfo methodInfo) {
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

	public static boolean validateParamAnnotations(Method method) {
		Annotation[][] paramAnnotations = method.getParameterAnnotations();
		boolean foundParamAnnotation = false;
		boolean foundParamNoAnnotation = false;

		for(Annotation[] paramA: paramAnnotations) {
			boolean found = false;
			for(Annotation annotation: paramA) {
				if(XorDomain.class.isAssignableFrom(annotation.getClass()) ||
						XorExternal.class.isAssignableFrom(annotation.getClass()) ||
						XorDataService.class.isAssignableFrom(annotation.getClass()) ||
						XorExternalData.class.isAssignableFrom(annotation.getClass())) {
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
			logger.warn(
				"The business logic method " + method.getName()
					+ " should have Input/Output parameter annotations on all its parameters to indicate how the param values need to be populated");
		}
		
		//if(!Modifier.isStatic(method.getModifiers())) {
		//	throw new RuntimeException("The business logic method " + method.getDeclaringClass().getName() + "#" + method.getName() + " should be static");
		//}

		return foundParamAnnotation;
	}

	public static Map<String, List<MethodInfo>> initLambdas(Class<?> instanceClass) {

		HashMap<String, List<MethodInfo>> allMethods = new HashMap<String, List<MethodInfo>>();

		if(!isValidInstanceClass(instanceClass))
			return Collections.unmodifiableMap(allMethods);

		Set<String> notUnique = new HashSet<String>();
		for(Method method: instanceClass.getMethods()) {
			if(method.getAnnotation(XorLambda.class) != null) {

				if(method.getParameterTypes().length != 0) {
					if(!validateParamAnnotations(method)) {
						continue;
					}
				}

				XorLambda lambda = method.getAnnotation(XorLambda.class);
				if(notUnique.contains(lambda.property())) {
					// already recorded
					continue;
				}
				MethodInfo methodInfo = new MethodInfo(
					lambda.order(),
					lambda.fromVersion(), 
					lambda.untilVersion(),
					method,
					lambda.capture(),
					lambda.action(),
					lambda.tag(),
					lambda.phase(),
					lambda.stage());
				if(!addUniqueMethod(allMethods, lambda.property(), methodInfo)) {
					notUnique.add(lambda.property());
				}
			}
		}
		// Get the polymorphic method
		for(String property: notUnique) {
			Method method = getPolymorphicGetterMethod(property, instanceClass);
			XorLambda dataUpdate = method.getAnnotation(XorLambda.class);
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

		return Collections.unmodifiableMap(allMethods);
	}

	@Override
	public List<MethodInfo> getLambdas(String targetProperty) {
		return lambdas.get(targetProperty);
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

	protected void initXorEntity() {
		Annotation annotation = getClassAnnotation(XorEntity.class);
		if(annotation != null && annotation.annotationType() == XorEntity.class) {
			boolean value = ((XorEntity)annotation).immutable();
			if(value) {
				this.immutable = value;
			}
			
			String[] userKeyArr = ((XorEntity)annotation).naturalKey();
			if(userKeyArr != null && userKeyArr.length > 0) {
				setNaturalKey(userKeyArr);
			}
		}	
	}	

	@Override
	public boolean isImmutable() {
		return immutable;
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
	public String getDomainTypeName() {
		return this.domainTypeName;
	}
	
	public void setDomainTypeName(String value) {
	    this.domainTypeName = value;
	}

	@Override
	public boolean isDataType ()
	{
		return false;
	}

	@Override
	public ClassResolver getClassResolver() {
		return classResolver;
	}

	public void setClassResolver(ClassResolver classResolver) {
		this.classResolver = classResolver;
	}

	protected boolean isSuperTypeProperty(String name) {
		// Property is present in the supertype, so we do not add it here
		if(superType != null && superType.getProperty(name) != null) {
			return true;
		}

		return false;
	}

	@Override
	public List <Property> getDeclaredProperties() {
		Map<String, Property> propertyMap = getShape().getProperties(this);
		if(propertyMap == null) {
			return null;
		}

		return new ArrayList<>(propertyMap.values());
	}

	List<Property> getProperties (Collection<Property> input)
	{
		List<Property> result = new ArrayList<>(input.size());
		for (Property p : input) {
			ExtendedProperty property = (ExtendedProperty)p;

			//Get the property for this type, helps to resolve to the correct model whether
			// external or domain
			Property resolvedProperty = getProperty(property.getName());
			if (resolvedProperty == null) {
				logger.warn(
					"Could not find property: " + p.getName() + " in type " + getName());
				continue;
			}
			result.add(resolvedProperty);
		}

		return result;
	}

	/**
	 * This method is not synchronized as we do not remove elements from a Map
	 */
	@Override
	public void addProperty(Property property) {
		getShape().addProperty(property);
	}

	@Override
	public void removeProperty(Property property) {
		getShape().removeProperty(property);
	}

	@Override
	public boolean isNullable(String path) {
		int delim = path.indexOf(Settings.PATH_DELIMITER);

		if(delim == -1) {
			if(getShape().getProperties(this) == null) {
				throw new IllegalStateException("Properties not found for type: " + getName() + " with class: " + getInstanceClass().getName());
			}
			Property p = getShape().getProperty(this, path);
			return p == null || p.isNullable();
		} else {
			Property property = getProperty(path.substring(0, delim));
			if(property == null) {
				logger.info("Property " + path + " not found. If this is an open property, ensure it is added to the type");
				return true;
			}

			Type propertyType = property.getType();
			if(property.isMany())
				propertyType = ((ExtendedProperty)property).getElementType();
			return property.isNullable() || ((EntityType)propertyType).isNullable(path.substring(delim+1));
		}
	}
	
	@Override
	public Property getPropertyByAlias(String name) {
		for(Property property: getShape().getProperties(this).values()) {
			if(property.getAliasNames().contains(name))
				return property;
		}
		
		return null;
	}
	
	@Override
	public void initPositionProperty(Shape shape) {
		if(isOpen()) {
			return;
		}

		for(Property property: shape.getProperties(this).values()) {
			((ExtendedProperty)property).initPositionProperty();
		}
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
	public List<String> getNaturalKey() {
		return this.naturalKey;
	}

	@Override
	public List<String> getExpandedNaturalKey() {
		if(this.expandedNaturalKey == null) {
			this.expandedNaturalKey = new ArrayList<>();

			if( this.naturalKey != null) {
				for (String key : this.naturalKey) {
					Property p = getProperty(key);
					if (!p.getType().isDataType() && p.getType() instanceof EntityType) {
						// expand
						EntityType entityType = (EntityType)p.getType();
						if(entityType.getExpandedNaturalKey().size() > 0) {
							for (String subKey : entityType.getExpandedNaturalKey()) {
								this.expandedNaturalKey.add(key + Settings.PATH_DELIMITER + subKey);
							}
						} else {
							String idName = entityType.getIdentifierProperty().getName();
							this.expandedNaturalKey.add(key + Settings.PATH_DELIMITER + idName);
						}
					}
					else {
						this.expandedNaturalKey.add(key);
					}
				}
			}
		}
		return this.expandedNaturalKey;
	}
	
	@Override
	public void setNaturalKey(String[] keys) {
		if(keys != null) {
			this.naturalKey = Arrays.asList(keys);
		} else {
			this.naturalKey = null;
		}

		// validation
		if(this.naturalKey != null && new HashSet<String>(this.naturalKey).size() != this.naturalKey.size()) {
			throw new RuntimeException(String.format("Duplicate natural key components not allowed [%s]: %s", getName(), String.join(", ", keys)));
		}

		// reset expandedNaturalKey
		this.expandedNaturalKey = null;
	}
	
	@Override
	public int compareTo(EntityType o) {
		return this.getOrder() - o.getOrder();
	}
	
	@Override
	public Object newInstance(Object instance) {
		 return ClassUtil.newInstance(getInstanceClass());
	}	
	
	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		assert(visitor != null);

		JSONObject result = new JSONObject();

		// For containment or entities with natural keys use the generator to populate the values
		// otherwise we link with an existing object
		if (ApplicationConfiguration.config().containsKey(Constants.Config.GENERATOR_LINK_EXISTING)
			&& ApplicationConfiguration.config().getBoolean(Constants.Config.GENERATOR_LINK_EXISTING)) {
			if (property != null && !property.isContainment() && getNaturalKey() == null
				&& entitiesToChooseFrom != null && entitiesToChooseFrom.size() > 0) {
				result = entitiesToChooseFrom.get(
					(int)(ClassUtil.nextDouble() * (entitiesToChooseFrom.size() - 1)));
			}
		}

		boolean castLot = false;
		for(Property p: getProperties()) {
			if( ((ExtendedProperty) p).isDataType()) {

				// Get a new lot value and link all the LinkedChoices fields together
				// for this entity
				Generator gen = ((ExtendedProperty)p).getGenerator(visitor.getRelationshipName());
				if(p != null && gen instanceof LinkedChoices && !castLot) {
					((LinkedChoices)gen).castLot();
					castLot = true;
				}

				// For now we skip data type associations, we might change this in the
				// future
				if(p.isMany()) {
					continue;
				}

				result.put(p.getName(), ((BasicType)p.getType()).generate(
						settings,
						p,
						rootedAt,
						entitiesToChooseFrom,
						visitor));
			}
		}
		result.put(Constants.XOR.TYPE, getInstanceClass().getName());
		result.put(Constants.XOR.GEN_PARENT, rootedAt);
		
		return result;
	}

	public static String getBaseName(String className)
	{
		if (className.indexOf(Settings.PATH_DELIMITER) != -1) {
			return className.substring(
				className.lastIndexOf(Settings.PATH_DELIMITER) + 1);
		} else {
			return className;
		}
	}

	/**
	 * Give a chance for providers to do final initialization
	 * @param shape type system
	 */
	public void initEnd (Shape shape) {

	}

	@Override public boolean isLOB ()
	{
		return false;
	}

	@Override
	public boolean isExplorable () {
		return true;
	}

	@Override
	public boolean isRootConcreteType() {
		boolean result = true;

		EntityType superType = getParentType();
		while (superType != null) {
			if(!superType.isAbstract()) {
				result = false;
				break;
			}

			superType = superType.getParentType();
		}

		return result;
	}

	@Override
	public void setOpenProperty(Object obj, String propertyName, Object value ) {
		throw new RuntimeException("Unable to set property: " + propertyName);
	}

	@Override
	public boolean isSameOrSupertypeOf (EntityType entityType) {
		EntityType parent = entityType;
		while(parent != null) {
			if(parent == this) {
				return true;
			}
			parent = parent.getParentType();
		}

		return false;
	}

	@Override
	public boolean isSubtypeOf (EntityType entityType) {
		EntityType superType = getParentType();
		if(superType == null) {
			return false;
		}

		return entityType.isSameOrSupertypeOf(superType);
	}

	@Override
	public List<EntityType> getDescendantsTo(EntityType entityType) {
		List<EntityType> result = new LinkedList<>();

		Stack<EntityType> reverse = new Stack<>();
		while(entityType.getParentType() != this) {
			reverse.push(entityType);
			entityType = entityType.getParentType();
		}

		while(!reverse.isEmpty()) {
			result.add(reverse.pop());
		}

		return result;
	}

	@Override
	public List<EntityType> findInSubtypes (String property) {
		List<EntityType> result = new LinkedList<>();

		// Ensure property is not present in the current type
		assert(getShape().getProperty(this, property) == null);

		// We will do a BFS to get the result
		Queue<EntityType> queue = new LinkedList<>();
		queue.addAll(getChildTypes());
		while(!queue.isEmpty()) {
			// remove the head of the queue
			EntityType childType = queue.remove();

			// check if this type has the property. If it has it then
			// add it to the result
			// if not check its children by added them to the back of the queue
			if(getShape().getDeclaredProperty(childType, property) != null) {
				result.add(childType);
			} else {
				queue.addAll(childType.getChildTypes());
			}
		}

		return result;
	}
	
	/**
	 * Populate the properties of the super type if needed
	 * @return true if the supertype was created false otherwise
	 */
	protected boolean createdSuperTypeProperties() {
	       // If the properties are already defined then return
        if (shape.getDeclaredProperties(this) != null) {
            return false;
        }

        EntityType parentType = getParentType();
        // Ensure that the properties has been populated for the supertype
        if(parentType != null) {
            parentType.defineProperties(shape);
        }
        
        return true;
	}

	@Override
	public String getGraphQLName ()
	{
		if(getName() == null) {
			throw new RuntimeException("Name cannot be null");
		}

		if(getInstanceClass() != null && getName().equals(getInstanceClass().getName())) {
			return getInstanceClass().getSimpleName();
		}

		return getName();
	}
}
