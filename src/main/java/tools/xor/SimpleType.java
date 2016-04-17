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

import tools.xor.service.AbstractDASFactory;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * SimpleType have no properties
 * 
 * @author Dilip Dalton
 * 
 */
public class SimpleType implements BasicType {

	private Class<?> instanceClass;
	private ClassResolver classResolver;
	private DataAccessService das;

	public SimpleType(Class<?> clazz) {
		this(clazz, null);
	}

	public SimpleType(Class<?> clazz, DataAccessService das) {
		this.instanceClass = clazz;
		classResolver = new ClassResolver(this);
		this.das = das;
	}

	@Override
	public String getName() {
		// The name is unique in the namespace because if is qualified by the
		// package name
		return instanceClass.getName();
	}

	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Class<?> getInstanceClass() {
		return instanceClass;
	}

	@Override
	public boolean isInstance(Object object) {
		return getClass().isAssignableFrom(object.getClass());
	}

	/**
	 * The property names within the type are unique
	 */
	@Override
	public List<Property> getProperties() {
		return new ArrayList<Property>();
	}

	@Override
	public Property getProperty(String propertyName) {
		return null;
	}

	@Override
	public boolean isDataType() {
		return true;
	}

	/**
	 * We use the DataAccessService to see for the type from the object.
	 * If the DataAccessService object is not set, then this indicates that we should
	 * not infer the type from the object.
	 *
	 * @param object The object whose type we are trying to check. If it is null, then
	 *               we fallback to checking the meta information. The meta information may not
	 *               always be correct since it could refer to an abstract class that might be
	 *               marked as a DataType.
	 * @return
	 */
	@Override public boolean isDataType (Object object)
	{
		Type instanceType = null;
		if (object != null && das != null) {
			object = ClassUtil.getInstance(object);
			instanceType = das.getType(object.getClass());
		}
		if(instanceType != null) {
			return instanceType.isDataType();
		} else {
			return isDataType();
		}
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isSequenced() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public List<Type> getBaseTypes() {
		return new ArrayList<Type>();
	}

	@Override
	public List<Property> getDeclaredProperties() {
		return new ArrayList<Property>();
	}

	@Override
	public List<?> getAliasNames() {
		return new ArrayList<String>();
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}

	@Override
	public ClassResolver getClassResolver() {
		return classResolver;
	}

	/**
	 * By default we treat instance as an
	 * immutable object
	 */
	@Override
	public Object newInstance(Object instance) {
		return instance;
	}
}
