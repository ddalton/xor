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
import java.math.BigInteger;
import java.sql.Blob;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.service.Shape;

/**
 * @author Dilip Dalton
 * 
 */
public class SimpleTypeFactory {
	private static Map<Class<?>, SimpleType> simpleTypeMap = new HashMap<>();

	static {
		simpleTypeMap.put(String.class, new StringType(String.class));
		simpleTypeMap.put(BigInteger.class, new BigIntegerType(BigInteger.class));
		simpleTypeMap.put(BigDecimal.class, new BigDecimalType(BigDecimal.class));
		simpleTypeMap.put(boolean.class, new BooleanType(boolean.class));
		simpleTypeMap.put(Boolean.class, new BooleanType(Boolean.class));
		simpleTypeMap.put(byte.class, new ByteType(byte.class));
		simpleTypeMap.put(Byte.class, new ByteType(Byte.class));
		simpleTypeMap.put(short.class, new ShortType(short.class));
		simpleTypeMap.put(Short.class, new ShortType(Short.class));
		simpleTypeMap.put(char.class, new CharType(char.class));
		simpleTypeMap.put(Character.class, new CharType(Character.class));
		simpleTypeMap.put(int.class, new IntType(int.class));
		simpleTypeMap.put(Integer.class, new IntType(Integer.class));
		simpleTypeMap.put(long.class, new LongType(long.class));
		simpleTypeMap.put(Long.class, new LongType(Long.class));
		simpleTypeMap.put(float.class, new FloatType(float.class));
		simpleTypeMap.put(Float.class, new FloatType(Float.class));
		simpleTypeMap.put(double.class, new DoubleType(double.class));
		simpleTypeMap.put(Double.class, new DoubleType(Double.class));
	}

	/**
	 * Creates the Simple Java Data type.
	 * The number of if-then-else should not impact performance significantly because
	 * this is invoked only during startup.
	 * 
	 * @param clazz the java class
	 * @param shape of all the types
	 * @return the Type instance
	 */
	public static SimpleType getType(Class<?> clazz, Shape shape) {

		if(simpleTypeMap.containsKey(clazz)) {
			return simpleTypeMap.get(clazz);
		}

		 if (UnsignedByteType.class.isAssignableFrom(clazz)) {
			return new UnsignedByteType(clazz);
		}  else if (clazz.isArray()) {
			return new ArrayType(clazz);
		} else if(Date.class.isAssignableFrom(clazz)) {
			return new DateType(clazz);
		} else if(Set.class.isAssignableFrom(clazz)) {
			return new SetType(clazz);
		} else if(List.class.isAssignableFrom(clazz) || JSONArray.class.isAssignableFrom(clazz)) {
			return new ListType(clazz);
		} else if(Map.class.isAssignableFrom(clazz)) {
			return new MapType(clazz);
		} else if(Collection.class.isAssignableFrom(clazz)) {
			return new CollectionType(clazz);
		} else if(Blob.class.isAssignableFrom(clazz)) {
			return new BlobType(clazz);
		} else if(Enum.class.isAssignableFrom(clazz)) {
			return new EnumType(clazz);
		} else {
			return new SimpleType(clazz, shape);
		}
	}

	/**
	 * Scalar as defined by GraphQL
	 * @param clazz to check its "scalarness"
	 * @return true if scalar false otherwise
	 */
	public static boolean isScalar (Class<?> clazz)
	{
		if (List.class.isAssignableFrom(clazz) || JSONArray.class.isAssignableFrom(clazz)
			|| Map.class.isAssignableFrom(clazz) || Collection.class.isAssignableFrom(clazz)
			|| Set.class.isAssignableFrom(clazz) || JSONObject.class.isAssignableFrom(clazz)) {
			return false;
		}

		return true;
	}
}
