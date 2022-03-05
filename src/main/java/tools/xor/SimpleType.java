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
import org.json.JSONObject;

import tools.xor.generator.DefaultGenerator;
import tools.xor.generator.Generator;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

/**
 * SimpleType have no properties
 * 
 * @author Dilip Dalton
 * 
 */
public class SimpleType implements BasicType {

	private Class<?> instanceClass;
	private ClassResolver classResolver;
	private Shape shape;

	public SimpleType(Class<?> clazz) {
		this(clazz, null);
	}

	public SimpleType(Class<?> clazz, Shape shape) {
		this.instanceClass = clazz;
		classResolver = new ClassResolver(this);
		this.shape = shape;
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
		if (object != null && shape != null) {
			object = ClassUtil.getInstance(object);
			instanceType = shape.getType(object.getClass());
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
	public List<Type> getParentTypes() {
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

	@Override public boolean isLOB ()
	{
		return false;
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

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		// generate method not supported for unknown type
		return null;
	}

	protected JSONArray generateArray(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
									  StateGraph.ObjectGenerationVisitor visitor)
	{
		JSONArray result = new JSONArray();

		String path = (rootedAt == null) ? null : (rootedAt.has(Constants.XOR.GEN_PATH) ?
			rootedAt.getString(Constants.XOR.GEN_PATH) : null);
		path = Constants.XOR.walkDown(path, property);

		BasicType elementType = (BasicType)((ExtendedProperty)property).getElementType();

		Generator gen = ((ExtendedProperty)property).getGenerator(visitor.getRelationshipName());
		if(gen == null) {
			gen = new DefaultGenerator(null);
		}
		int fanOut = gen.getFanout(property, settings, path, visitor);

		// If this is not a containment property, then link to an existing

		if(elementType instanceof EntityType && gen.isApplicableToCollectionElement()) {
			((ExtendedProperty)property).setGenerator(gen);
		}

		BasicType collectionElementType = elementType;
		for (int i = 0; i < fanOut; i++) {

			// Keep track of the index, as this is needed by some Generator implementations
			visitor.setSequenceNo(i);

			// Handle inheritance (dynamic subType selection)
			if (elementType instanceof EntityType) {
				collectionElementType = gen.getSubType((EntityType)elementType, visitor.getStateGraph());
			}
			if(collectionElementType == null) {
				throw new RuntimeException("No valid subtypes were found for " + elementType.getName() + " in the given StateGraph.");
			}
			Object collectionElement = collectionElementType.generate(settings, property, rootedAt, entitiesToChooseFrom, visitor);
			result.put(collectionElement);

			// check limits
			if(visitor != null && visitor.hasReachedLimit()) {
				break;
			}
		}

		return result;
	}

    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_STRING_TYPE;
    }


    @Override
	public TypeKind getKind() { return TypeKind.SCALAR; }
}
