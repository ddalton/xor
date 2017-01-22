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

import tools.xor.service.DataAccessService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dilip Dalton
 * 
 */
public class SimpleTypeFactory {

	/**
	 * Creates the Simple Java Data type.
	 * The number of if-then-else should not impact performance significantly because
	 * this is invoked only during startup.
	 * 
	 * @param clazz the java class
	 * @param das the DataAccessService instance
	 * @return the Type instance
	 */
	public static SimpleType getType(Class<?> clazz, DataAccessService das) {
		if(String.class == clazz) {
			return new StringType(clazz);	  
		} else if (BigInteger.class == clazz) {
			return new BigIntegerType(clazz);
		} else if (BigDecimal.class == clazz) {
			return new BigDecimalType(clazz);
		} else if (boolean.class == clazz || Boolean.class == clazz) {
			return new BooleanType(clazz);
		} else if (byte.class == clazz || Byte.class == clazz) {
			return new ByteType(clazz);
		} else if (short.class == clazz || Short.class == clazz) {
			return new ShortType(clazz);			
		} else if (char.class == clazz || Character.class == clazz) {
			return new CharType(clazz);			
		} else if (int.class == clazz || Integer.class == clazz) {
			return new IntType(clazz);
		} else if (long.class == clazz || Long.class == clazz) {
			return new LongType(clazz);			
		} else if (float.class == clazz || Float.class == clazz) {
			return new FloatType(clazz);
		} else if (double.class == clazz || Double.class == clazz) {
			return new DoubleType(clazz);			
		} else if (clazz.isArray()) {
			return new ArrayType(clazz);
		} else if(Date.class.isAssignableFrom(clazz)) {
			return new DateType(clazz);
		} else if(Set.class.isAssignableFrom(clazz)) {
			return new SetType(clazz);
		} else if(List.class.isAssignableFrom(clazz)) {
			return new ListType(clazz);
		} else if(Map.class.isAssignableFrom(clazz)) {
			return new MapType(clazz);
		} else if(Collection.class.isAssignableFrom(clazz)) {
			return new CollectionType(clazz);
		} else {
			return new SimpleType(clazz, das);
		}
	}

}
