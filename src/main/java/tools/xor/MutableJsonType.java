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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.service.Shape;

/**
 * SimpleType have no properties
 * 
 * @author Dilip Dalton
 * 
 */
public class MutableJsonType extends ExternalType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private static final String SWAGGER_ALLOF = "allOf";
	private static final String SWAGGER_TYPE  = "type";
	private static final String SWAGGER_PROPERTIES = "properties";
	private static final String SWAGGER_REF   = "$ref";
	public static final String  SWAGGER_REF_SEPARATOR = "/";
	
	private List<String> parentTypeNames;
	private JSONObject   swaggerSchema;

	public MutableJsonType(EntityType domainType, Class<?> javaClass) {
		super(domainType, javaClass);
	}
	
	/**
	 * Used to support swagger schema
	 * @param json schema for the type
	 */
	public MutableJsonType(String entityName, JSONObject json, String idPropertyName) {
	    super(entityName, JSONObject.class);
	    
	    this.swaggerSchema = json;
        this.isDataType = true;
        this.idPropertyName = idPropertyName;
        this.versionPropertyName = null; // currently not supported as this schema is not used for updates
        this.isEmbedded = idPropertyName == null;
        this.isEntity = true;
        this.parentTypeNames = new ArrayList<>();
        
        // extract parentTypes
        if(json.has(SWAGGER_ALLOF)) {
            JSONArray array = json.getJSONArray(SWAGGER_ALLOF);
            
            // Process each element in the array
            for(int i = 0 ; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if(obj.has(SWAGGER_REF)) {
                    String parentPath = obj.getString(SWAGGER_REF);
                    // get the last component in the parent path
                    String parentEntityName = parentPath.substring(parentPath.lastIndexOf(SWAGGER_REF_SEPARATOR) + SWAGGER_REF_SEPARATOR.length());
                    this.parentTypeNames.add(parentEntityName);
                }
            }
        }
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
	    
		Class<?> externalClass = typeMapper.toExternal(domainProperty.getType().getInstanceClass());
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
			Class<?> propertyClass = typeMapper.toExternal(domainProperty.getType().getInstanceClass());
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
	
	/**
	 * Used to define the swagger type properties
	 * @param shape for swagger schema
	 */
	public void defineProperties(Shape shape) {
	    JSONObject properties = swaggerSchema.has(SWAGGER_PROPERTIES) ? swaggerSchema.getJSONObject(SWAGGER_PROPERTIES) : null;
	    
        if(properties == null && swaggerSchema.has(SWAGGER_ALLOF)) {
            JSONArray array = swaggerSchema.getJSONArray(SWAGGER_ALLOF);
            
            // Process each element in the array
            for(int i = 0 ; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if(obj.has(SWAGGER_TYPE)) {
                    properties = obj.has(SWAGGER_PROPERTIES) ? obj.getJSONObject(SWAGGER_PROPERTIES) : null;
                    break;
                }
            }
        }
        
        // process each property
        if(this.parentTypeNames.size() == 0 && properties == null) {
            throw new RuntimeException("Unable to find the properties object or the type has no properties");
        }
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
}
