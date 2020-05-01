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

import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.util.ClassUtil;

/**
 * This is designed to work with javax.json.JsonObject
 * 
 * @author Dilip Dalton
 *
 */
public class ImmutableJsonProperty extends ExternalProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private static Map<String, Converter> converters = new HashMap<String, Converter>();
	public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	public interface Converter {
		public Object toExternal(JsonObjectBuilder builder, String name, Object object);
		public Object toDomain(JsonValue value);
		
		/**
		 * We have to use an array builder since there is no "name" property
		 * @param builder json array builder
		 * @param object to add to the builder
		 */
		public void add(JsonArrayBuilder builder, Object object);
	}
	
	abstract public static class AbstractConverter implements Converter {
		@Override
		public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
			return builder.add(name, (JsonValue) object);
		}
	}
	
	static {
		converters.put(Boolean.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (Boolean)object);
					}	
						
					@Override
					public Object toDomain(JsonValue jsonValue) {

						if(jsonValue == JsonValue.FALSE)
							return Boolean.FALSE;
						if(jsonValue == JsonValue.TRUE)
							return Boolean.TRUE;
									
						return null;
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((Boolean) object);
					}
				}
			);
		converters.put(boolean.class.getName(), converters.get(Boolean.class.getName())); // primitive
		
		converters.put(BigDecimal.class.getName(),
			new AbstractConverter() {
				@Override
				public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
					return builder.add(name, object == null ? null : (BigDecimal)object);
				}	
				
				@Override
				public Object toDomain(JsonValue jsonValue) {
					return jsonValue == null ? null : ((JsonNumber)jsonValue).bigDecimalValue();
				}

				@Override
				public void add(JsonArrayBuilder builder, Object object) {
					builder.add((BigDecimal) object);
				}
			}
		);
		
		converters.put(BigInteger.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (BigInteger)object);
					}	
			
					@Override
					public Object toDomain(JsonValue jsonValue) {
						return jsonValue == null ? null : ((JsonNumber)jsonValue).bigIntegerValue();
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((BigInteger) object);
					}
				}
			);	
		
		converters.put(Double.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (Double)object);
					}	
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						return jsonValue == null ? null : ((JsonNumber)jsonValue).doubleValue();
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((Double) object);
					}
				}
			);		
		converters.put(double.class.getName(), converters.get(Double.class.getName())); // primitive
		
		converters.put(Float.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (Float)object);
					}	
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						return jsonValue == null ? null : (float) ((JsonNumber)jsonValue).doubleValue();
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((Float) object);
					}
				}
			);	
		converters.put(float.class.getName(), converters.get(Float.class.getName())); // primitive		
		
		converters.put(Integer.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (Integer)object);
					}	
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						return jsonValue == null ? null : ((JsonNumber)jsonValue).intValue();
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((Integer) object);
					}
				}
			);
		converters.put(int.class.getName(), converters.get(Integer.class.getName())); // primitive				
		
		converters.put(Long.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (Long)object);
					}		
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						return jsonValue == null ? null : ((JsonNumber)jsonValue).longValue();
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((Long) object);
					}
				}
			);
		converters.put(long.class.getName(), converters.get(Long.class.getName())); // primitive		
		
		converters.put(String.class.getName(),	
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : object.toString());
					}		
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						return jsonValue == null ? null : ((JsonString)jsonValue).getString();
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add((String) object);
					}
				}
			);		
		
		converters.put(Date.class.getName(),
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						DateFormat df = new SimpleDateFormat(ISO8601_FORMAT);
						return builder.add(name, object == null ? null : df.format(object));
					}
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						DateFormat df = new SimpleDateFormat(ISO8601_FORMAT);
						try {
							return jsonValue == null ? null : df.parse(((JsonString)jsonValue).getString());
						} catch (ParseException e) {
							logger.warn("DynamicProperty#getObject problem parsing date string: " 
									+ jsonValue.toString() + ", message: " + e.getMessage());
							return null;
						}						
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						DateFormat df = new SimpleDateFormat(ISO8601_FORMAT);
						builder.add(object == null ? null : df.format(object));
					}
				}
			);	
	
		/**
		 * Invoking build() on a JsonObjectBuilder, retains only this property and clears out
		 * all the other properties. So build() should be invoked only once at the end of 
		 * populating all the fields.
		 */
		converters.put(JsonObjectBuilder.class.getName(),	
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						return builder.add(name, object == null ? null : (JsonObjectBuilder)object);
					}		
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						// We cannot handle it here since we do not know the type
						return jsonValue;
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add(object == null ? null : (JsonObjectBuilder)object);
					}
				}
			);	
		
		converters.put(JsonArrayBuilder.class.getName(),	
				new AbstractConverter() {
					@Override
					public Object toExternal(JsonObjectBuilder builder, String name, Object object) {
						Object result = builder.add(name, object == null ? null : (JsonArrayBuilder)object);
						return result;
					}		
					
					@Override
					public Object toDomain(JsonValue jsonValue) {
						// We cannot handle it here since we do not know the type
						return jsonValue;
					}

					@Override
					public void add(JsonArrayBuilder builder, Object object) {
						builder.add(object == null ? null : (JsonArrayBuilder)object);
					}
				}
			);			
	}	
	
	@Override
	public Class<?> getJavaType() {
		return getType().getInstanceClass();
	}	
	
	public ImmutableJsonProperty(ExtendedProperty domainProperty, Type type,
			ExternalType parentType, Type externalType) {
		super(domainProperty, type, parentType, externalType);
	}

	@Override
	public String getStringValue(BusinessObject dataObject)
	{
		Object instance = ClassUtil.getInstance(dataObject);
		if(JsonObject.class.isAssignableFrom(instance.getClass())) {
			JsonObject json = (JsonObject)instance;
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
		if(JsonObject.class.isAssignableFrom(instance.getClass())) {
			JsonObject json = (JsonObject) instance;
			JsonValue jsonValue = json.get(getName());
			Object value = toDomain(jsonValue);
			if(logger.isDebugEnabled()) {
				logger.debug("DynamicProperty#getValue Property: " + getName() + ", value: " + (value == null? "null" : value.toString()) 
						+ ", input: " + (jsonValue == null ? "null": jsonValue.toString()) );
			}
			return value;
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
		if(JsonObjectBuilder.class.isAssignableFrom(instance.getClass())) {
			JsonObjectBuilder jsonBuilder = (JsonObjectBuilder) instance;
			toExternal(jsonBuilder, getName(), propertyValue);
		} else {
			logger.error("DynamicProperty#setValue dataObject instance is not a JsonObject");
		}		
	}	
	
    /**
     * Get the property converter based on the domain class name
     * @return converter
     */
    private Converter getDomainConverter() {
        Converter result = null;
        
        // we have a potential converter
        if(this.getDomainTypeName() != null) {
            result = converters.get(this.getDomainTypeName());
        }
        
        return result;
    }	
	
	private Object toExternal(JsonObjectBuilder builder, String name, Object propertyValue) {
        Converter domainConverter = getDomainConverter();
        if(domainConverter != null) {
            return domainConverter.toExternal(builder, name, propertyValue);	    
		} else {
			if(BusinessObject.class.isAssignableFrom(propertyValue.getClass())) {
				Object childbuilder = ((BusinessObject)propertyValue).getInstance();
				if(JsonObjectBuilder.class.isAssignableFrom(childbuilder.getClass())) {
					return converters.get(JsonObjectBuilder.class.getName()).toExternal( builder, name, childbuilder);	
				} else if (JsonArrayBuilder.class.isAssignableFrom(childbuilder.getClass())) {
					return converters.get(JsonArrayBuilder.class.getName()).toExternal( builder, name, childbuilder);	
				}
			}

			return propertyValue;
		}		
	}
	
	private Object toDomain(JsonValue jsonValue) {
	    Converter domainConverter = getDomainConverter();
		if(domainConverter != null) {
			return domainConverter.toDomain(jsonValue);
		} else {
			logger.info("DynamicProperty#toDomain: Unknown converter for " + getType().getInstanceClass() 
					+ ", jsonValue: " + jsonValue 
					+ ", type name: " + getType().getName());
			return jsonValue;
		}

	}

	@Override
	public void addElement(BusinessObject dataObject, Object element) {

		if(!JsonArrayBuilder.class.isAssignableFrom(((BusinessObject) dataObject).getInstance().getClass())) {
			throw new IllegalArgumentException("DynamicProperty#addElement dataObject instance " 
					+ ((BusinessObject) dataObject).getInstance().getClass() + " is not of type JsonArrayBuilder");
		}

		JsonArrayBuilder jsonArrayBuilder = (JsonArrayBuilder) ((BusinessObject) dataObject).getInstance();
		if(converters.containsKey(element.getClass().getName())) {
			converters.get(element.getClass().getName()).add(jsonArrayBuilder, element);
		} else {

			if(JsonObjectBuilder.class.isAssignableFrom(element.getClass())) {
				converters.get(JsonObjectBuilder.class.getName()).add( jsonArrayBuilder, element);	
			} else if (JsonArrayBuilder.class.isAssignableFrom(element.getClass())) {
				converters.get(JsonArrayBuilder.class.getName()).add(jsonArrayBuilder, element);	
			} else {
				logger.error("DynamicProperty#addElement element " + element.getClass() + " is not of type JsonValue/JsonObjectBuilder/JsonArrayBuilder");
			}
		}
	}		

	
	@Override
	public void addMapEntry(Object dataObject, Object key, Object value) {
		if(!JsonObject.class.isAssignableFrom(((BusinessObject) dataObject).getInstance().getClass())) {
			throw new IllegalArgumentException("DynamicProperty#addMapEntry dataObject is not of type JsonObject");
		}
		
		if(!JsonValue.class.isAssignableFrom(value.getClass()))  {
			throw new IllegalArgumentException("DynamicProperty#addMapEntry element is not of type JsonValue");
		}
		
		JsonObject jsonObject = (JsonObject) ((BusinessObject) dataObject).getInstance();
		jsonObject.put(key.toString(), (JsonValue) value);
	}
}
