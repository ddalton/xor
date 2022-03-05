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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;

/**
 * SimpleType have no properties
 * 
 * @author Dilip Dalton
 * 
 */
public class MutableJsonType extends ExternalType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	public static final String SCHEMA_ALLOF = "allOf";
	public static final String SCHEMA_TYPE  = "type";
	public static final String SCHEMA_FORMAT  = "format";
	public static final String SCHEMA_ITEMS  = "items";
	public static final String SCHEMA_PROPERTIES = "properties";
	public static final String SCHEMA_REQUIRED = "required";
	public static final String SCHEMA_REF   = "$ref";
	public static final String SCHEMA_REF_SEPARATOR = "/";
	public static final String SCHEMA_ID_PROPERTY = "surrogateKey";
	
	public static final String JSONSCHEMA_STRING_TYPE  = "string";
	public static final String JSONSCHEMA_NUMBER_TYPE  = "number";
	public static final String JSONSCHEMA_INTEGER_TYPE  = "integer";
	public static final String JSONSCHEMA_BOOLEAN_TYPE  = "boolean";
	public static final String JSONSCHEMA_ARRAY_TYPE  = "array";
	public static final String JSONSCHEMA_OBJECT_TYPE  = "object";
	private static final Map<String, Class<?>> JSONSCHEMA_TYPES = new HashMap<>();
	
	public static final String JSONSCHEMA_FORMAT_DATE_TIME = "date-time";
    public static final String JSONSCHEMA_FORMAT_BIGINTEGER = "biginteger";
    public static final String JSONSCHEMA_FORMAT_BIGDECIMAL = "bigdecimal";    
	
	static {
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_STRING_TYPE, String.class);
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_NUMBER_TYPE, BigDecimal.class);
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_INTEGER_TYPE, Integer.class);
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_BOOLEAN_TYPE, boolean.class);
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_ARRAY_TYPE, List.class);
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_OBJECT_TYPE, Object.class);
	    
	    // some types are decided by the format specifier
	    JSONSCHEMA_TYPES.put(JSONSCHEMA_FORMAT_DATE_TIME, Date.class);
        JSONSCHEMA_TYPES.put(JSONSCHEMA_FORMAT_BIGINTEGER, BigInteger.class);
        JSONSCHEMA_TYPES.put(JSONSCHEMA_FORMAT_BIGDECIMAL, BigDecimal.class);        
	}
	
	private List<String> parentTypeNames;
	private JSONObject   jsonSchema;

	public MutableJsonType(EntityType domainType, Class<?> javaClass) {
		super(domainType, javaClass);
	}
	
	/**
	 * Used to support swagger schema
	 * @param entityName for the type
	 * @param json schema for the type
	 * @param idPropertyName identifier property name
	 */
	public MutableJsonType(String entityName, JSONObject json, String idPropertyName) {
	    super(entityName, JSONObject.class);
	    
	    this.jsonSchema = json;
        this.isDataType = true;
        this.idPropertyName = idPropertyName;
        this.versionPropertyName = null; // currently not supported as this schema is not used for updates
        this.isEmbedded = idPropertyName == null;
        this.isEntity = true;
        this.parentTypeNames = new ArrayList<>();
        
        // extract parentTypes
        if(json.has(SCHEMA_ALLOF)) {
            JSONArray array = json.getJSONArray(SCHEMA_ALLOF);
            
            // Process each element in the array
            for(int i = 0 ; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if(obj.has(SCHEMA_REF)) {
                    // get the parent entity name from the ref path
                    this.parentTypeNames.add(getEntityNameFromRef(obj));
                }
            }
        }
	}
	
	public void setIdPropertyName(String name) {
	    this.idPropertyName = name;
	    this.isEmbedded = false;
	}
	
	private String getEntityNameFromRef(JSONObject obj) {
        String refPath = obj.getString(SCHEMA_REF);
        
        // get the last component in the parent path
        String refEntityName = refPath.substring(refPath.lastIndexOf(SCHEMA_REF_SEPARATOR) + SCHEMA_REF_SEPARATOR.length());
        
        return refEntityName;
	}
	
	@Override
    public void initParentTypes(EntityType domainType, TypeMapper typeMapper) {
	    // Are we initializing for a Swagger schema
	    if(this.parentTypeNames != null) {
            for (String parentEntityName : this.parentTypeNames) {
                Type externalType = getShape().getType(parentEntityName);
                this.parentTypes.add((ExternalType) externalType);
            }	        
	    } else {
	        super.initParentTypes(domainType, typeMapper);
	    }
    }	

	@Override
	public Method getGetterMethod(String targetProperty){
		// Dynamic type does not have getters
		return null;
	}	
	
	@Override
	public String getName() {
		return getEntityName();
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public Property defineProperty(Property domainProperty, Shape dynamicShape, TypeMapper typeMapper) {
	    
		Class<?> externalClass = typeMapper.toExternal(domainProperty.getType());
		if(externalClass == null) {
			throw new RuntimeException("The dynamic type is missing for the following domain class: " + domainProperty.getType().getInstanceClass().getName());
		}

        String typeName = domainProperty.getType().getName();
        if(domainProperty.getType() instanceof EntityType) {
            typeName = ((EntityType)domainProperty.getType()).getEntityName();
        }
        Type propertyType = dynamicShape.getType(typeName);
		Type elementType = null;
		if(((ExtendedProperty)domainProperty).getElementType() != null) {
            String elementTypeName = ((ExtendedProperty)domainProperty).getElementType().getName();
            if(((ExtendedProperty)domainProperty).getElementType() instanceof EntityType) {
                elementTypeName = ((EntityType)((ExtendedProperty)domainProperty).getElementType()).getEntityName();
            }                    
            elementType = dynamicShape.getType(elementTypeName);		    
		}		
		if(propertyType == null) {
			Class<?> propertyClass = typeMapper.toExternal(domainProperty.getType());
			logger.debug("Name: " + domainProperty.getName() + ", Domain class: " + domainProperty.getType().getInstanceClass().getName() + ", property class: " + propertyClass.getName());
			propertyType = dynamicShape.getType(propertyClass);
		}
		MutableJsonProperty dynamicProperty = null;
		if(domainProperty.isOpenContent()) {
			dynamicProperty = new MutableJsonProperty(domainProperty.getName(), (ExtendedProperty) domainProperty, propertyType, this, elementType);
		} else {
			dynamicProperty = new MutableJsonProperty((ExtendedProperty) domainProperty, propertyType, this, elementType);
		}
        dynamicProperty.setDomainTypeName(domainProperty.getType().getInstanceClass().getName());
        dynamicProperty.setConverter(((ExtendedProperty)domainProperty).getConverter());
        
		return dynamicProperty;
	}

	@Override
	public void setProperty (Shape domainShape, Shape dynamicShape, TypeMapper typeMapper)
	{
		// populate the properties for this type
	    EntityType domainType = (EntityType) domainShape.getType(getEntityName());
		for (Property domainProperty : domainShape.getProperties(domainType).values()) {
			MutableJsonProperty dynamicProperty = (MutableJsonProperty)defineProperty(
				domainProperty,
				dynamicShape,
				typeMapper);

			dynamicProperty.init((ExtendedProperty) domainProperty, dynamicShape);
			logger.debug(
				"[" + getName() + "] Domain property name: " + domainProperty.getName()
					+ ", type name: " + dynamicProperty.getJavaType());
			dynamicShape.addProperty(dynamicProperty);
		}
	}
	
    public void defineRequired() {
        JSONArray required = jsonSchema.has(SCHEMA_REQUIRED) ? jsonSchema.getJSONArray(SCHEMA_REQUIRED)
                : null;
        
        if (required == null && jsonSchema.has(SCHEMA_ALLOF)) {
            JSONArray array = jsonSchema.getJSONArray(SCHEMA_ALLOF);
            
            // Process each element in the array
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has(SCHEMA_REQUIRED)) {
                    required = obj.getJSONArray(SCHEMA_REQUIRED);
                }
                if(required != null) {
                    break;
                }
            }
        }
        
        if(required != null) {
            for(int i = 0; i < required.length(); i++) {
                String propertyName = required.getString(i);
                MutableJsonProperty property = (MutableJsonProperty) ClassUtil.getDelegate(getProperty(propertyName));
                property.setNullable(true);    
            }
        }
    }
	
	/**
	 * Used to define the swagger type properties
	 * @param shape for swagger schema
	 */
    public void defineProperties(Shape shape) {
        JSONObject properties = jsonSchema.has(SCHEMA_PROPERTIES) ? jsonSchema.getJSONObject(SCHEMA_PROPERTIES)
                : null;

        if (properties == null && jsonSchema.has(SCHEMA_ALLOF)) {
            JSONArray array = jsonSchema.getJSONArray(SCHEMA_ALLOF);

            // Process each element in the array
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has(SCHEMA_PROPERTIES)) {
                    properties = obj.getJSONObject(SCHEMA_PROPERTIES);
                    
                    if(obj.has(SCHEMA_ID_PROPERTY)) {
                        this.idPropertyName = obj.getString(SCHEMA_ID_PROPERTY);
                    }
                }
                if(properties != null) {
                    break;
                }
            }
        } else if(jsonSchema.has(SCHEMA_ID_PROPERTY)) {
            this.idPropertyName = jsonSchema.getString(SCHEMA_ID_PROPERTY);            
        }

        // process each property
        if (this.parentTypeNames.size() == 0 && properties == null) {
            throw new RuntimeException("Unable to find the properties object or the type has no properties");
        } else {
            // this is an entity type and mark it as such
            this.isDataType = false;
        }

        if (properties != null) {
            String[] propertyNames = JSONObject.getNames(properties);
            for (int i = 0; i < propertyNames.length; i++) {
                JSONObject obj = properties.getJSONObject(propertyNames[i]);
                Type propertyType = null;
                Type elementType = null;

                // TO_ONE relationship
                if (obj.has(SCHEMA_REF)) {
                    String toOneEntityName = getEntityNameFromRef(obj);
                    propertyType = getShape().getType(toOneEntityName);
                } else if (obj.has(SCHEMA_TYPE)) {
                    propertyType = getType(obj);
                    if (JSONSCHEMA_ARRAY_TYPE.equals(obj.get(SCHEMA_TYPE))) {
                        // look for the items object
                        JSONObject items = obj.getJSONObject(SCHEMA_ITEMS);
                        // to many entity relationship
                        if (items.has(SCHEMA_REF)) {
                            elementType = getShape().getType(getEntityNameFromRef(items));
                        } else {
                            elementType = getType(items);
                        }
                    }
                }

                // The property might just be a placeholder, so we check for this scenario.
                if(propertyType != null) {
                    shape.addProperty(new MutableJsonProperty(propertyNames[i], propertyType, this, elementType));
                }
            }
        }
    }
    
    private Type getType(JSONObject obj) {
        if (obj.has(SCHEMA_FORMAT)) {
            return getShape().getType(JSONSCHEMA_TYPES.get(obj.get(SCHEMA_FORMAT)));
        }
        if (obj.has(SCHEMA_TYPE)) {
            return getShape().getType(JSONSCHEMA_TYPES.get(obj.get(SCHEMA_TYPE)));
        }
        return null;
    }

	@Override
	public void setOpenProperty(Object obj, String propertyName, Object value ) {
		if(obj instanceof JSONObject) {
			JSONObject json = (JSONObject) obj;
			json.put(propertyName, value);
		} else {
			super.setOpenProperty(obj, propertyName, value);
		}
	}

    protected MutableJsonType (String typeName,
                            Class<JSONObject> javaClass)
    {
        super(typeName, javaClass);
    }
}
