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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.BasicType;
import tools.xor.BusinessObject;
import tools.xor.Property;
import tools.xor.Settings;


public class ImmutableJsonCreationStrategy extends AbstractCreationStrategy {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	
	
	private POJOCreationStrategy pojoCS;
	
	private static final Set<Class<?>> unchanged = new HashSet<Class<?>>();
	
	static {
		unchanged.add(String.class);
		unchanged.add(java.util.Date.class);
		unchanged.add(Boolean.class);
		unchanged.add(Void.class);
		unchanged.add(Character.class);
		unchanged.add(Byte.class);
		unchanged.add(Short.class);
		unchanged.add(Integer.class);
		unchanged.add(Long.class);
		unchanged.add(Float.class);
		unchanged.add(Double.class);
		unchanged.add(boolean.class);
		unchanged.add(char.class);
		unchanged.add(byte.class);
		unchanged.add(short.class);
		unchanged.add(int.class);
		unchanged.add(long.class);
		unchanged.add(float.class);
		unchanged.add(double.class);	
		unchanged.add(BigDecimal.class);
		unchanged.add(BigInteger.class);
		unchanged.add(JsonNumber.class);
		unchanged.add(JsonString.class);
	}	
	
	public ImmutableJsonCreationStrategy(ObjectCreator objectCreator) {
		super(objectCreator);
		
		pojoCS = new POJOCreationStrategy(objectCreator);
	}
	
	@Override
	/**
	 * Handle the creation of the following classes
	 * JsonObject
	 * JsonArray
	 * JsonNumber
	 * JsonString
	 * JsonValue.TRUE
	 * JsonValue.FALSE
	 * JsonValue.NULL
	 */
	public Object newInstance(Object from, BasicType type, Class<?> toClass) throws Exception {
		return this.newInstance(from, type, toClass, null, null);
	}

	@Override
	public Object newInstance(Object from, BasicType type, Class<?> toClass, BusinessObject container,
			Property containmentProperty) throws Exception {

		Object result = null;
		if(unchanged.contains(toClass)) {
			result = from;
		} else if(toClass == JsonObject.class) {
			result = Json.createObjectBuilder();
		} else if(toClass == JsonArray.class) {
			result = Json.createArrayBuilder();
		} else if(toClass == JsonValue.class) {
			if(Boolean.class.isAssignableFrom(from.getClass())) {
				result = from;
			} else {
				result = null;
			}
		} else {
			result = pojoCS.newInstance(from, type, toClass);
		}
		logger.debug("JSONCreationStrategy#newInstance from: " + (from==null ? "null": from) 
				+ ", toClass: " + (toClass == null ? "null" : toClass.getName()) 
				+ ", result: " + (result == null ? "null" : result.getClass().getName()));
		
		return result;
	}
	
	@Override
	public Object getNormalizedInstance(BusinessObject bo, Settings settings) {
		Object instance = bo.getInstance();
		if(JsonObjectBuilder.class.isAssignableFrom(instance.getClass())) {
			return ((JsonObjectBuilder)instance).build();
		}
		return super.getNormalizedInstance(bo, settings);
	}
}