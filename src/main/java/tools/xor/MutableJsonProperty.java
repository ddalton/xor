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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tools.xor.service.DataAccessService;
import tools.xor.util.ClassUtil;

/**
 * This is designed to work with javax.json.JsonObject
 * 
 * @author Dilip Dalton
 *
 */
public class MutableJsonProperty extends ExternalProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private static Map<Class, Converter> converters = new ConcurrentHashMap<Class, Converter>();
	public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	public interface Converter {
		public void setExternal(JSONObject jsonObject, String name, Object object) throws JSONException;
		public Object toDomain(JSONObject jsonObject, String key) throws JSONException;

		/**
		 * We have to use an array builder since there is no "name" property
		 *
		 * @param jsonArray
		 * @param object
		 */
		public void   add(JSONArray jsonArray, Object object);
	}

	public static void registerConverter(Class<?> clazz, Converter converter) {
		if(!converters.containsKey(clazz)) {
			converters.put(clazz, converter);
		}
	}

	public static Converter findConverter(Class<?> clazz) {
		if(converters.containsKey(clazz)) {
			return converters.get(clazz);
		}

		return null;
	}
	
	abstract public static class AbstractConverter implements Converter {
		@Override
		public void setExternal(JSONObject jsonObject, String name, Object object) throws JSONException {
			jsonObject.put(name, object);
		}
	}
	
	static {
		converters.put(Boolean.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {

						return jsonObject.getBoolean(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((Boolean) object);
					}
				}
			);
		converters.put(boolean.class, converters.get(Boolean.class)); // primitive
		
		converters.put(BigDecimal.class,
			new AbstractConverter() {
				
				@Override
				public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
					if(jsonObject.has(key)) {
						Object value = jsonObject.get(key);
						if(value instanceof BigDecimal) 
							return value;
						else
							return new BigDecimal(jsonObject.getString(key));
					}
					return null;
				}

				@Override
				public void add(JSONArray jsonArray, Object object) {
					jsonArray.put((BigDecimal) object);
				}
			}
		);
		
		converters.put(BigInteger.class,
				new AbstractConverter() {

					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						if(jsonObject.has(key)) {
							Object value = jsonObject.get(key);
							if(value instanceof BigInteger) 
								return value;
							else
								return new BigInteger(jsonObject.getString(key));
						}
						return null;						
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((BigInteger) object);
					}
				}
			);	
		
		converters.put(Double.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getDouble(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((Double) object);
					}
				}
			);		
		converters.put(double.class, converters.get(Double.class)); // primitive
		
		converters.put(Float.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						return (float) jsonObject.getDouble(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((Float) object);
					}
				}
			);	
		converters.put(float.class, converters.get(Float.class)); // primitive		
		
		converters.put(Integer.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getInt(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((Integer) object);
					}
				}
			);
		converters.put(int.class, converters.get(Integer.class)); // primitive				
		
		converters.put(Long.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getLong(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((Long) object);
					}
				}
			);
		converters.put(long.class, converters.get(Long.class)); // primitive		
		
		converters.put(String.class,	
				new AbstractConverter() {	
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getString(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put((String) object);
					}
				}
			);		
		
		converters.put(Date.class,
				new AbstractConverter() {
					@Override
					public void setExternal(JSONObject jsonObject, String name, Object object) throws JSONException {
						DateFormat df = new SimpleDateFormat(ISO8601_FORMAT);
						jsonObject.put(name, object == null ? null : df.format(object));
					}
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						DateFormat df = new SimpleDateFormat(ISO8601_FORMAT);
						String dataString = jsonObject.getString(key);
						try {
							return dataString == null ? null : df.parse(dataString);
						} catch (ParseException e) {
							logger.warn("DynamicProperty#getObject problem parsing date string: " 
									+ dataString + ", message: " + e.getMessage());
							return null;
						}						
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						DateFormat df = new SimpleDateFormat(ISO8601_FORMAT);
						jsonArray.put(object == null ? null : df.format(object));
					}
				}
			);	
	
		/**
		 * Invoking build() on a JsonObjectBuilder, retains only this property and clears out
		 * all the other properties. So build() should be invoked only once at the end of 
		 * populating all the fields.
		 */
		converters.put(JSONObject.class,	
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						// We cannot handle it here since we do not know the type
						return jsonObject.get(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put(object == null ? null : (JSONObject)object);
					}
				}
			);	
		
		converters.put(JSONArray.class,	
				new AbstractConverter() {
					
					@Override
					public Object toDomain(JSONObject jsonObject, String key) throws JSONException {
						// We cannot handle it here since we do not know the type
						return jsonObject.get(key);
					}

					@Override
					public void add(JSONArray jsonArray, Object object) {
						jsonArray.put(object == null ? null : (JSONArray)object);
					}
				}
			);			
	}	
	
	@Override
	public Class<?> getJavaType() {
		return getType().getInstanceClass();
	}	
	
	public MutableJsonProperty(ExtendedProperty domainProperty, Type type,
			ExternalType parentType) {
		super(domainProperty, type, parentType);
	}
	
	@Override
	public Object getValue(Object dataObject) 
	{	
		Object instance = ClassUtil.getInstance(dataObject);
		if(JSONObject.class.isAssignableFrom(instance.getClass())) {
			JSONObject json = (JSONObject) instance;
			try {
				Object value = toDomain(json, getName());
				if(logger.isDebugEnabled()) {
					logger.debug("DynamicProperty#getValue Property: " + getName() + ", value: " + (value == null? "null" : value.toString()) 
							+ ", input: " + (value == null ? "null": value.toString()) );
				}
				return value;
			} catch (JSONException e) {
				// This property was not found
				return null;
			}
		} else {
			// This is at INFO level, because on read we try to read from the JsonObjectBuilder which is not allowed
			logger.info("DynamicProperty#getValue dataObject instance is not a JsonObject " + instance.getClass().getName());
			return null;
		}
	
	}

	@Override
	public void setValue(Object dataObject, Object propertyValue) 
	{
		Object instance = ClassUtil.getInstance(dataObject);
		if(JSONObject.class.isAssignableFrom(instance.getClass())) {
			JSONObject jsonObject = (JSONObject) instance;
			try {
				setExternal(jsonObject, getName(), propertyValue);
			} catch (JSONException e) {
				throw ClassUtil.wrapRun(e);
			}
		} else {
			logger.error("DynamicProperty#setValue dataObject instance is not a JsonObject");
		}		
	}	
	
	private void setExternal(JSONObject jsonObject, String name, Object propertyValue) throws JSONException {
		if(converters.containsKey(getDomainProperty().getType().getInstanceClass())) {
			converters.get(getDomainProperty().getType().getInstanceClass()).setExternal(jsonObject, name, propertyValue);
		} else {
			Object instanceObj = propertyValue;
			if(BusinessObject.class.isAssignableFrom(propertyValue.getClass())) {
				instanceObj = ((BusinessObject)propertyValue).getInstance();
			}
			if(JSONObject.class.isAssignableFrom(instanceObj.getClass())) {
				converters.get(JSONObject.class).setExternal( jsonObject, name, instanceObj);	
			} else if (JSONArray.class.isAssignableFrom(instanceObj.getClass())) {
				converters.get(JSONArray.class).setExternal( jsonObject, name, instanceObj);	
			}
		}		
	}
	
	private Object toDomain(JSONObject jsonObject, String key) throws JSONException {
		if(converters.containsKey(getDomainProperty().getType().getInstanceClass())) {
			return converters.get(getDomainProperty().getType().getInstanceClass()).toDomain(jsonObject, key);
		} else {
			if(logger.isDebugEnabled()) {
				logger.debug("DynamicProperty#toDomain: Unknown converter for " + getType().getInstanceClass() 
						+ ", jsonValue: " + jsonObject.get(key) 
						+ ", type name: " + getType().getName() 
						+ ", domain type: " + getDomainProperty().getType().getInstanceClass().getName());
			}
			return jsonObject.get(key);
		}

	}

	@Override
	public void addElement(Object dataObject, Object element) {

		if(!JSONArray.class.isAssignableFrom(((BusinessObject) dataObject).getInstance().getClass())) {
			throw new IllegalArgumentException("DynamicProperty#addElement dataObject instance " 
					+ ((BusinessObject) dataObject).getInstance().getClass() + " is not of type JSONArray");
		}

		JSONArray jsonArray = (JSONArray) ((BusinessObject) dataObject).getInstance();
		if(converters.containsKey(element.getClass())) {
			converters.get(element.getClass()).add(jsonArray, element);
		} else {

			if(JSONObject.class.isAssignableFrom(element.getClass())) {
				converters.get(JSONObject.class).add( jsonArray, element);	
			} else if (JSONArray.class.isAssignableFrom(element.getClass())) {
				converters.get(JSONArray.class).add(jsonArray, element);	
			} else {
				logger.error("DynamicProperty#addElement element " + element.getClass() + " is not of type JsonValue/JsonObjectBuilder/JsonArrayBuilder");
			}
		}
	}		

	
	@Override
	public void addMapEntry(Object dataObject, Object key, Object value) {
		if(!JSONObject.class.isAssignableFrom(((BusinessObject) dataObject).getInstance().getClass())) {
			throw new IllegalArgumentException("DynamicProperty#addMapEntry dataObject is not of type JSONObject");
		}
		
		JSONObject jsonObject = (JSONObject) ((BusinessObject) dataObject).getInstance();
		try {
			jsonObject.put(key.toString(),  value);
		} catch (JSONException e) {
			throw ClassUtil.wrapRun(e);
		}
	}	
	
	@Override
	protected Type getExternalKeyType(DataAccessService das) {
		return das.getExternalType(((ExtendedProperty)getDomainProperty()).getKeyType().getName());
	}	
	
	@Override
	protected Type getExternalElementType(DataAccessService das) {
		return das.getExternalType(((ExtendedProperty)getDomainProperty()).getElementType().getName());
	}	
}
