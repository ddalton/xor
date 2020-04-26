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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.Access;
import javax.sql.DataSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import tools.xor.BusinessObject;
import tools.xor.ExtendedProperty;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.view.View;

@Component
public class ClassUtil {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private static final String JAVASSIST_STARTWITH = "org.javassist.tmp.";
	private static final String JAVASSIST_INDEXOF = "_$$_javassist_";

	private static AtomicBoolean parallelDispatch = new AtomicBoolean(true);

	private static boolean isJavassistEnhanced(Class<?> c) {
		String className = c.getName();

		// pattern found in javassist ProxyFactory
		return className.startsWith(JAVASSIST_STARTWITH)
				|| className.indexOf(JAVASSIST_INDEXOF) != -1;
	}

	public static void setParallelDispatch(boolean value) {
		parallelDispatch.set(value);
	}

	public static boolean doParallelDispatch() {
		return parallelDispatch.get();
	}

	public static Class<?> getUnEnhanced(Class<?> clazz) {
		if(isEnhanced(clazz))
			return clazz.getSuperclass();

		return clazz;
	}

	public static String getCSVFilename(String entityName) {
		return String.format("%s.csv", entityName);
	}

	public static boolean isEnhanced(Class<?> c) {
		boolean result = false;
		if (isJavassistEnhanced(c)) {
			result = true;
		}
		/*
		 * CGLIB support is deprecated so we won't support it. 
		if (Enhancer.isEnhanced(c) || isJavassistEnhanced(c)) {
			result = true;
		}		
		*/

		return result;
	}

	public static boolean isEnhanced(Object proxy) {
		boolean result = false;
		
		/*
		 * CGLIB support is deprecated so we won't support it.
		if (Enhancer.isEnhanced(proxy.getClass()) || isJavassistEnhanced(proxy.getClass()) ||
				AopUtils.isJdkDynamicProxy(proxy)) {
			result = true;
		}
		*/
		
		if ( isJavassistEnhanced(proxy.getClass()) || AopUtils.isJdkDynamicProxy(proxy)) {
			result = true;
		}		

		return result;
	}	

	public static Object getTargetObject(Object object) {
		Object result = object;

		// Hibernate based implementation, safe to initialize the proxy since we need its data
		// Allow user to override this behavior
		if (object instanceof HibernateProxy) {
			while(object instanceof HibernateProxy) {
				HibernateProxy proxy = (HibernateProxy) object; 
				LazyInitializer li = proxy.getHibernateLazyInitializer();
				object = li.getImplementation();
			}		
			return object;
		} else if (AopUtils.isJdkDynamicProxy(object)) {
			try {
				return ((Advised)object).getTargetSource().getTarget();
			} catch(Exception e) {
				throw wrapRun(e);
			}
		}

		return result;
	}

	/**
	 * Invoke the given method as a privileged action, if necessary.
	 * @param target the object on which the method needs to be invoked
	 * @param method to invoke
	 * @param args to the method
	 * @return result of the invocation
	 * @throws InvocationTargetException while invoking the method
	 * @throws IllegalAccessException when accessing the meta data
	 */
	//public static Object invokeMethodAsPrivileged(final Object target, final Method method, final Object[] args) 
	public static Object invokeMethodAsPrivileged(final Object target, final Method method, final Object... args)
			throws InvocationTargetException, IllegalAccessException 
			{
		if (Modifier.isPublic(method.getModifiers()))
			if(args == null) {
				return method.invoke(target);
			} else {
				try {
					return method.invoke(target, args);
				}
				catch (IllegalArgumentException e) {
					System.out.println("@@@@ Method: " + method.getName() + ", args: " + args.toString());

					e.printStackTrace();
				}
			}
		return AccessController.doPrivileged(
				new PrivilegedAction<Object>() {
					public Object run() {
						method.setAccessible(true);
						Object result = null;
						try {
							if(args == null)
								result = method.invoke(target);
							else
								result = method.invoke(target, args);
						} catch (Exception e) {
							throw wrapRun(e);
						}
						return result;
					}
				});
			}
	
	/** 
	 * Invoke the given method as a privileged action, if necessary. 
	 * @param target the object on which the method needs to be invoked
	 * @param field we are reading or writing
	 * @param value to set in the field
	 * @param read true if this a read operation
	 * @return result of the invocation
	 * @throws InvocationTargetException while invoking the method
	 * @throws IllegalAccessException when accessing the meta data
	 */
	public static Object invokeFieldAsPrivileged(final Object target, final Field field, final Object value, final boolean read) 
			throws InvocationTargetException, IllegalAccessException 
			{

		if (Modifier.isPublic(field.getModifiers())) {
			Object readValue = null;
			if(read)
				readValue = field.get(target);
			else
				field.set(target, value);
			return readValue;
		} else {
			return AccessController.doPrivileged(
					new PrivilegedAction<Object>() {
						public Object run() {
							field.setAccessible(true);
							Object readValue = null;
							try {
								if(read)
									readValue = field.get(target);
								else
									field.set(target, value);
							} catch (Exception e) {
								throw wrapRun(e);
							} 
							return readValue;
						}
					});
		}
			}		

	public static RuntimeException wrapRun(Exception e) {
		if(InvocationTargetException.class.isAssignableFrom(e.getClass())) {
			InvocationTargetException ite = (InvocationTargetException) e;
			Throwable cause = ite.getCause();
			if(cause != null && Exception.class.isAssignableFrom(cause.getClass()))
				e = (Exception) cause;
		}

		if(RuntimeException.class.isAssignableFrom(e.getClass()))
			return (RuntimeException) e;
		else
			return new RuntimeException(e);
	}

	public static tools.xor.AccessType getAccessStrategy(org.hibernate.cfg.AccessType type) {
		if ( org.hibernate.cfg.AccessType.PROPERTY.equals( type ) ) {
			return tools.xor.AccessType.PROPERTY;
		}
		else if ( org.hibernate.cfg.AccessType.FIELD.equals( type ) ) {
			return tools.xor.AccessType.FIELD;
		}
		else {
			return tools.xor.AccessType.PROPERTY;
		}
	}	

	public static PersistenceType getAccessStrategy(String accessType) {
		return PersistenceType.valueOf(accessType);
	}	

	public static tools.xor.AccessType getHibernateAccessType(Class<?> instanceClass) {
		org.hibernate.cfg.AccessType classDefinedAccessType;

		org.hibernate.cfg.AccessType hibernateDefinedAccessType = org.hibernate.cfg.AccessType.DEFAULT;
		org.hibernate.cfg.AccessType jpaDefinedAccessType = org.hibernate.cfg.AccessType.DEFAULT;

		org.hibernate.annotations.AccessType accessTypeAnno = instanceClass.getAnnotation( org.hibernate.annotations.AccessType.class );
		if ( accessTypeAnno != null ) {
			hibernateDefinedAccessType = org.hibernate.cfg.AccessType.getAccessStrategy( accessTypeAnno.value() );
		}

		Access accessAnno = instanceClass.getAnnotation( Access.class );
		if ( accessAnno != null ) {
			jpaDefinedAccessType = org.hibernate.cfg.AccessType.getAccessStrategy( accessAnno.value() );
		}

		if ( hibernateDefinedAccessType != org.hibernate.cfg.AccessType.DEFAULT
				&& jpaDefinedAccessType != org.hibernate.cfg.AccessType.DEFAULT
				&& hibernateDefinedAccessType != jpaDefinedAccessType ) {
			throw new RuntimeException(
					"@PersistenceType and @Access specified with contradicting values. Use of @Access only is recommended. "
					);
		}

		if ( hibernateDefinedAccessType != org.hibernate.cfg.AccessType.DEFAULT ) {
			classDefinedAccessType = hibernateDefinedAccessType;
		}
		else {
			classDefinedAccessType = jpaDefinedAccessType;
		}
		return ClassUtil.getAccessStrategy(classDefinedAccessType);		
	}	

	public static int getDimensionCount(Object array) {
		int count = 0;
		Class<?> arrayClass = array.getClass();
		while ( arrayClass.isArray() ) {
			count++;
			arrayClass = arrayClass.getComponentType();
		}
		return count;
	}	

	public static Object getInstance(Object dataObject) {
		if(dataObject == null)
			return null;

		Object instance = dataObject;
		// Using instanceof for performance reasons
		//if(BusinessObject.class.isAssignableFrom(dataObject.getClass()))
		if(dataObject instanceof BusinessObject)
			instance = ((BusinessObject)dataObject).getInstance();

		return instance;
	}		
	
	/**
	 * Creates a non cglib/javassist enhanced instance of the given class, which
	 * could itself be the class of a cglib/javassist enhanced object.
	 * @param fromClass basis of the java class for the new instance
	 * @return new object instance
	 */
	public static Object newInstance(Class<?> fromClass) {
		Class<?> toClass = ClassUtil.getUnEnhanced(fromClass);

		try {
			return newInstanceAsPrivileged(toClass);
		} catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}	
	}
	
	/**
	 * Creates a new instance of the given class via the no-arg constructor,
	 * invoking the constructor as a privileged action if it is protected or
	 * private.
	 * 
	 * @param c given class
	 * @return a new instance of the given class via the no-arg constructor
	 * @throws Exception when creating the instance
	 */
	public static Object newInstanceAsPrivileged(final Class<?> c) throws Exception {

		try {
			return c.newInstance();

		} catch (Exception e) {
			return AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					try {
						final Constructor<?> constructor = c.getDeclaredConstructor();
						constructor.setAccessible(true);
						return constructor.newInstance();
					} catch (Exception e)  {
						throw ClassUtil.wrapRun(e);
					}
				}
			});
		}
	}

	public static String getBucketName(Type type) {

		String name = type.getName();
		return Settings.encodeParam(name);
	}
	
	public static List jsonArrayToList (JSONArray jsonArray) {
		List list = new ArrayList<>( jsonArray.length() );
		try {
			for(int i = 0; i < jsonArray.length(); i++) {
				list.add(jsonArray.get(i));
			}
		} catch (JSONException e) {
			throw ClassUtil.wrapRun(e);
		}
		
		return list;
	}
	
	public static boolean intersectsTags(String[] tags, String[] otherTags) {
		Set<String> commonTags = new HashSet<String>(Arrays.asList(tags));
		commonTags.retainAll(new HashSet<String>(Arrays.asList(otherTags)) );
		if(commonTags.isEmpty()) {
			// applies to different tags so they do not overlap
			return false;
		}
		
		return true;
	}

	public static void initSingleLevel (Object instance, BusinessObject bo, Settings settings)
	{
		if(bo == null) {
			return;
		}

		View view = settings.getView();
		for(String propertyName: view.getAttributeList()) {
			ExtendedProperty property = (ExtendedProperty) bo.getType().getProperty(propertyName);
			if(property.isIdentifier()) {
				continue;
			}

			property.setValue(
				settings,
				instance,
				property.getValue(bo));
		}
	}

	public static void executeScript(DataSource datasource, String path) throws SQLException
	{
		executeScript(datasource, path, false, ";");
	}

	public static void executeScript(DataSource datasource, String path, String separator) throws SQLException
	{
		executeScript(datasource, path, false, separator);
	}

	public static void executeScript(DataSource datasource, String path, boolean continueOnError, String separator) throws SQLException
	{
		EncodedResource er = new EncodedResource(new ClassPathResource(path));
		try (Connection connection = datasource.getConnection();) {
			ScriptUtils.executeSqlScript(
				connection,
				er,
				continueOnError,
				false,
				"--",
				separator,
				"/*",
				"*/");
		}
	}

	public static JSONObject copyJson(JSONObject original) {
		return (JSONObject) copyValue(original, new HashMap<>());
	}

	private static Object copyValue(Object originalValue, Map<JSONObject, JSONObject> sharedObjects) {
		Object copyValue = originalValue;

		if(originalValue instanceof JSONObject) {
			if(sharedObjects.containsKey(originalValue)) {
				copyValue = sharedObjects.get(originalValue);
			} else {
				copyValue = new JSONObject();
				sharedObjects.put((JSONObject) originalValue, (JSONObject) copyValue);

				Iterator keys = ((JSONObject)originalValue).keys();
				while(keys.hasNext()) {
					String key = (String)keys.next();
					Object value = ((JSONObject)originalValue).get(key);

					((JSONObject)copyValue).put(key, copyValue(value, sharedObjects));
				}
			}
		} else if(originalValue instanceof JSONArray) {
			JSONArray copyArray = new JSONArray();
			for(int i = 0; i < ((JSONArray)originalValue).length(); i++) {
				copyArray.put(i, copyValue(((JSONArray)originalValue).get(i), sharedObjects));
			}
		} else if(originalValue instanceof Date) {
			copyValue =  new Date(((Date)originalValue).getTime());
		}

		return copyValue;
	}
}
