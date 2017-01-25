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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;

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
	 * @return true if the type is a data type
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

	/**
	 * Usually takes a toString() value and reconstitutes the object from it
	 * @param value This is an object's value represented as a string
	 * @return reconstitued java object
	 */
	public Object unmarshall(String value) {

		if(instanceClass != String.class) {
			if(value == null || "".equals(value)) {
				return null;
			}
		}

		if( Boolean.class == instanceClass || Boolean.TYPE == instanceClass ) return Boolean.parseBoolean( value );
		if( Byte.class == instanceClass || Byte.TYPE == instanceClass ) return Byte.parseByte( value );
		if( Short.class == instanceClass || Short.TYPE == instanceClass) return Short.parseShort( value );
		if( Integer.class == instanceClass || Integer.TYPE == instanceClass ) return Integer.parseInt( value );
		if( Long.class == instanceClass || Long.TYPE == instanceClass) return Long.parseLong( value );
		if( Float.class == instanceClass || Float.TYPE == instanceClass) return Float.parseFloat( value );
		if( Double.class == instanceClass || Double.TYPE == instanceClass) return Double.parseDouble( value );
		if (BigDecimal.class == instanceClass) return new BigDecimal(value);

		return value;
	}
	
	public Object generate(Settings settings, Property property) {
		// generate method not supported for unknown type
		return null;
	}

	protected JSONArray generateArray(Settings settings, Property property) {
		JSONArray result = new JSONArray();

		int fanOut = (int) (Math.random() * settings.getEntitySize().size() * settings.getSparseness());
		EntityType elementType = (EntityType) ((ExtendedProperty)property).getElementType();
		for(int i = 0; i < fanOut; i++) {
			result.put(elementType.generate(settings, property));
		}

		return result;
	}
}
