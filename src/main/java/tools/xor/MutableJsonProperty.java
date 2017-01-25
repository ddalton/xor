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
	
	private static Map<Class, Converter> convertersByClass = new ConcurrentHashMap<Class, Converter>();
	private static Map<String, Converter> convertersByProperty = new ConcurrentHashMap<String, Converter>();	
	public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	private volatile Converter converter;
	
	public interface Converter {
		public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException;
		public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException;

		/**
		 * We have to use an array builder since there is no "name" property
		 *
		 * @param settings under which this operation is performed
		 * @param jsonArray object
		 * @param object to add to the array
		 */
		public void   add(Settings settings, JSONArray jsonArray, Object object);
	}

	public static void registerConverter(Class<?> clazz, Converter converter) {
		if(!convertersByClass.containsKey(clazz)) {
			convertersByClass.put(clazz, converter);
		}
	}
	
	public static void registerConverter(String propertyName, Converter converter) {
		if(!convertersByProperty.containsKey(propertyName)) {
			convertersByProperty.put(propertyName, converter);
		}
	}	

	public static Converter findConverter(Class<?> clazz) {
		if(convertersByClass.containsKey(clazz)) {
			return convertersByClass.get(clazz);
		}

		return null;
	}
	
	abstract public static class AbstractConverter implements Converter {
		@Override
		public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException {
			jsonObject.put(name, object);
		}
	}
	
	static {
		convertersByClass.put(Boolean.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {

						return jsonObject.getBoolean(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((Boolean) object);
					}
				}
			);
		convertersByClass.put(boolean.class, convertersByClass.get(Boolean.class)); // primitive
		
		convertersByClass.put(BigDecimal.class,
			new AbstractConverter() {
				
				@Override
				public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
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
				public void add(Settings settings, JSONArray jsonArray, Object object) {
					jsonArray.put((BigDecimal) object);
				}
			}
		);
		
		convertersByClass.put(BigInteger.class,
				new AbstractConverter() {

					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
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
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((BigInteger) object);
					}
				}
			);	
		
		convertersByClass.put(Double.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getDouble(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((Double) object);
					}
				}
			);		
		convertersByClass.put(double.class, convertersByClass.get(Double.class)); // primitive
		
		convertersByClass.put(Float.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						return (float) jsonObject.getDouble(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((Float) object);
					}
				}
			);	
		convertersByClass.put(float.class, convertersByClass.get(Float.class)); // primitive		
		
		convertersByClass.put(Integer.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getInt(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((Integer) object);
					}
				}
			);
		convertersByClass.put(int.class, convertersByClass.get(Integer.class)); // primitive				
		
		convertersByClass.put(Long.class,
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getLong(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((Long) object);
					}
				}
			);
		convertersByClass.put(long.class, convertersByClass.get(Long.class)); // primitive		
		
		convertersByClass.put(String.class,	
				new AbstractConverter() {	
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						return jsonObject.getString(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put((String) object);
					}
				}
			);

		convertersByClass.put(Date.class,
				new AbstractConverter() {

					@Override
					public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException {
						if(settings.getDateForm() == Settings.DateForm.FORMATTED) {
							DateFormat df = new SimpleDateFormat(settings.getDateFormat());
							jsonObject.put(name, object == null ? null : df.format(object));
						} else {
							jsonObject.put(name, object == null ? null : ((Date)object).getTime());
						}
					}
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						Object date = jsonObject.get(key);
						if(date instanceof Date) {
							return date;
						}

						if(settings.getDateForm() == Settings.DateForm.FORMATTED) {
							DateFormat df = new SimpleDateFormat(settings.getDateFormat());
							String dateString = jsonObject.getString(key);
							try {
								return dateString == null ?
									null :
									("".equals(dateString) ? null : df.parse(dateString));
							}
							catch (ParseException e) {
								logger.warn(
									"DynamicProperty#getObject problem parsing date string: "
										+ dateString + ", message: " + e.getMessage());
								return null;
							}
						} else {
							String dateString = jsonObject.getString(key);
							return dateString == null ?
								null :
								("".equals(dateString) ? null : Long.parseLong(dateString));
						}
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						if(settings.getDateForm() == Settings.DateForm.FORMATTED) {
							DateFormat df = new SimpleDateFormat(settings.getDateFormat());
							jsonArray.put(object == null ? null : df.format(object));
						} else {
							jsonArray.put(object == null ? null : ((Date)object).getTime());
						}
					}
				}
			);	
	
		/**
		 * Invoking build() on a JsonObjectBuilder, retains only this property and clears out
		 * all the other properties. So build() should be invoked only once at the end of 
		 * populating all the fields.
		 */
		convertersByClass.put(JSONObject.class,	
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						// We cannot handle it here since we do not know the type
						return jsonObject.get(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
						jsonArray.put(object == null ? null : (JSONObject)object);
					}
				}
			);	
		
		convertersByClass.put(JSONArray.class,	
				new AbstractConverter() {
					
					@Override
					public Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
						// We cannot handle it here since we do not know the type
						return jsonObject.get(key);
					}

					@Override
					public void add(Settings settings, JSONArray jsonArray, Object object) {
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
			ExternalType parentType, Type elementType) {
		super(domainProperty, type, parentType, elementType);
	}

	public MutableJsonProperty(String name, ExtendedProperty domainProperty, Type type,
							   ExternalType parentType, Type elementType) {
		super(name, domainProperty, type, parentType, elementType);
	}

	@Override
	public String getStringValue(BusinessObject dataObject)
	{
		Object instance = ClassUtil.getInstance(dataObject);
		if (JSONObject.class.isAssignableFrom(instance.getClass())) {
			JSONObject json = (JSONObject)instance;

			try {
				Object value = json.get(getName());
				if(value instanceof String) {
					return json.getString(getName());
				} else {
					return (String)value;
				}
			} catch (Exception e) {
				return null;
			}
		}

		return getValue(dataObject).toString();
	}
	
	@Override
	public Object getValue(BusinessObject dataObject)
	{	
		Object instance = ClassUtil.getInstance(dataObject);
		if(JSONObject.class.isAssignableFrom(instance.getClass())) {
			JSONObject json = (JSONObject) instance;
			try {
				Object value = toDomain(dataObject.getSettings(), json, getName());
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
	public void setValue(Settings settings, Object dataObject, Object propertyValue)
	{
		Object instance = ClassUtil.getInstance(dataObject);
		if(JSONObject.class.isAssignableFrom(instance.getClass())) {
			JSONObject jsonObject = (JSONObject) instance;
			try {
				setExternal(settings, jsonObject, getName(), propertyValue);
			} catch (JSONException e) {
				throw ClassUtil.wrapRun(e);
			}
		} else {
			logger.error("DynamicProperty#setValue dataObject instance is not a JsonObject");
		}		
	}	
	
	private Converter getConverter() {
		if(this.converter == null) {
			if(convertersByProperty.containsKey(getName())) {
				converter = convertersByProperty.get(getName());
			}
			if(convertersByClass.containsKey(getDomainProperty().getType().getInstanceClass())) {
				converter = convertersByClass.get(getDomainProperty().getType().getInstanceClass());
			}
		}
		
		return converter;
	}
	
	private void setExternal(Settings settings, JSONObject jsonObject, String name, Object propertyValue) throws JSONException {
		if(getConverter() != null) {
			getConverter().setExternal(settings, jsonObject, name, propertyValue);
		} else {
			Object instanceObj = propertyValue;
			if(BusinessObject.class.isAssignableFrom(propertyValue.getClass())) {
				instanceObj = ((BusinessObject)propertyValue).getInstance();
			}
			if(JSONObject.class.isAssignableFrom(instanceObj.getClass())) {
				convertersByClass.get(JSONObject.class).setExternal( settings, jsonObject, name, instanceObj);
			} else if (JSONArray.class.isAssignableFrom(instanceObj.getClass())) {
				convertersByClass.get(JSONArray.class).setExternal( settings, jsonObject, name, instanceObj);
			}
		}		
	}
	
	private Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
		if(getConverter() != null) {
			return getConverter().toDomain(settings, jsonObject, key);
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
	public void addElement(BusinessObject dataObject, Object element) {

		if(!JSONArray.class.isAssignableFrom(((BusinessObject) dataObject).getInstance().getClass())) {
			throw new IllegalArgumentException("DynamicProperty#addElement dataObject instance " 
					+ ((BusinessObject) dataObject).getInstance().getClass() + " is not of type JSONArray");
		}

		JSONArray jsonArray = (JSONArray) ((BusinessObject) dataObject).getInstance();
		if(convertersByClass.containsKey(element.getClass())) {
			convertersByClass.get(element.getClass()).add(dataObject.getSettings(), jsonArray, element);
		} else {

			if(JSONObject.class.isAssignableFrom(element.getClass())) {
				convertersByClass.get(JSONObject.class).add(dataObject.getSettings(), jsonArray, element);
			} else if (JSONArray.class.isAssignableFrom(element.getClass())) {
				convertersByClass.get(JSONArray.class).add(dataObject.getSettings(), jsonArray, element);
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
