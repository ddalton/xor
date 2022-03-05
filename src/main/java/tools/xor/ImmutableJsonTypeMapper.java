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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import tools.xor.service.DataModel;
import tools.xor.util.CreationStrategy;
import tools.xor.util.ImmutableJsonCreationStrategy;
import tools.xor.util.ObjectCreator;

public class ImmutableJsonTypeMapper extends AbstractTypeMapper {
	
	private String domainPackagePath;	
	
	private static final Map<Class<?>, Class<?>> javaToJson = new HashMap<Class<?>, Class<?>>();
	private static final Map<String, String> javaClassNameToJson = new HashMap<String, String>();
	
	static {
		javaToJson.put(String.class, JsonString.class);
		javaToJson.put(java.util.Date.class, JsonString.class);
		javaToJson.put(Boolean.class, JsonValue.class);
		javaToJson.put(Void.class, JsonValue.class);
		javaToJson.put(Character.class, JsonNumber.class);
		javaToJson.put(Byte.class, JsonNumber.class);
		javaToJson.put(Short.class, JsonNumber.class);
		javaToJson.put(Integer.class, JsonNumber.class);
		javaToJson.put(Long.class, JsonNumber.class);
		javaToJson.put(Float.class, JsonNumber.class);
		javaToJson.put(Double.class, JsonNumber.class);
		javaToJson.put(BigDecimal.class, JsonNumber.class);
		javaToJson.put(BigInteger.class, JsonNumber.class);
		
		// primitives
		javaToJson.put(boolean.class, JsonValue.class);
		javaToJson.put(char.class, JsonNumber.class);
		javaToJson.put(byte.class, JsonNumber.class);
		javaToJson.put(short.class, JsonNumber.class);
		javaToJson.put(int.class, JsonNumber.class);
		javaToJson.put(long.class, JsonNumber.class);
		javaToJson.put(float.class, JsonNumber.class);
		javaToJson.put(double.class, JsonNumber.class);	
		
		for(Class<?> javaClass: javaToJson.keySet()) {
		    javaClassNameToJson.put(javaClass.getName(), javaToJson.get(javaClass).getName());
		}
	}
	
    public ImmutableJsonTypeMapper() {
        super();
    }	
	
    public ImmutableJsonTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) 
    {
        super(das, side, shapeName, persistenceManaged);
    }	

	public String getDomainPackagePath() {
		return domainPackagePath;
	}

	public void setDomainPackagePath(String domainPackagePath) {
		this.domainPackagePath = domainPackagePath;
	}
	
	@Override
	public String toDomain(String typeName) {
        if(!isDomain(typeName)) {
            throw new UnsupportedOperationException("Cannot resolve the domain class from a JSON object");
        }
        return typeName;	    
	}
	
    public String getMappedType(String typeName, CallInfo callInfo) {
        String result = null;

        switch(getSide()) {
        case EXTERNAL:          
            result = toExternal(typeName);
            break;
        case DOMAIN:        
            try {
                result = toDomain(typeName);
            } catch (UnsupportedOperationException e) {
                if(callInfo.getInputProperty() != null) {
                    Property domainProperty = getDomainProperty(callInfo.getInputProperty().getContainingType().getEntityName(), callInfo.getInputProperty().getName());                    
                    result = domainProperty.getType().getName();
                } else {
                    // Collection, so go to the owner and get the element type
                    ExtendedProperty property = callInfo.getParent().getInputProperty();
                    Property domainProperty = getDomainProperty(property.getContainingType().getEntityName(), property.getName());
                    result = ((ExtendedProperty)domainProperty).getElementType().getName();
                }
            }
            break;
        default:
            result = typeName;
            break;
        }

        return result;
    }		

	@Override
	/**
	 * Handle the interpretation of returning the following classes:
	 * 
	 * JsonObject
	 * JsonArray
	 * JsonNumber
	 * JsonString
	 * JsonValue.TRUE
	 * JsonValue.FALSE
	 * JsonValue.NULL
	 */
	public Class<?> toExternal(Type type) {
		
	    Class<?> domainClass = type.getInstanceClass();
	    
		if(javaToJson.containsKey(domainClass)) {
			return javaToJson.get(domainClass);
		}
		
		if(Set.class.isAssignableFrom(domainClass) ||
				List.class.isAssignableFrom(domainClass) ||
				domainClass.isArray()) {
			return JsonArray.class;
		}
		
		return JsonObject.class;		
	}
	
    @Override
    /**
     * Handle the interpretation of returning the following classes:
     * 
     * JsonObject
     * JsonArray
     * JsonNumber
     * JsonString
     * JsonValue.TRUE
     * JsonValue.FALSE
     * JsonValue.NULL
     */
    public String toExternal(String typeName) {
        
        if(javaClassNameToJson.containsKey(typeName)) {
            return javaClassNameToJson.get(typeName);
        }
        
        Class<?> domainClass;
        try {
            domainClass = Class.forName(typeName);
            if(Collection.class.isAssignableFrom(domainClass)) {
                return JsonArray.class.getName();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }        
        
        return JsonObject.class.getName();        
    }	
	
    @Override
    public boolean isExternal(String typeName) {
        if(typeName != null) {
            Class<?> externalClass;
            try {
                externalClass = Class.forName(typeName);
                if(externalClass != null && externalClass.isAssignableFrom(JsonValue.class)) {
                    return true;
                }                
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        throw new RuntimeException("Type name is null: " + typeName);
    }   	
	
    @Override
    public boolean isDomain(String typeName) {
        return (typeName.startsWith(domainPackagePath));
    }	

	@Override
	protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
		return new ImmutableJsonTypeMapper(das, side, shapeName, persistenceManaged);
	}
	
    @Override 
    public TypeMapper newInstance(MapperSide side) {
        return newInstance(side, null);
    }
    
    @Override
    public TypeMapper newInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        ImmutableJsonTypeMapper mapper = (ImmutableJsonTypeMapper)createInstance(das, side, shapeName, persistenceManaged);
        mapper.setDomainPackagePath(getDomainPackagePath());

        return mapper;           
    }     
	
	@Override
	public CreationStrategy getCreationStrategy(ObjectCreator oc) {
	    if(getSide() == MapperSide.EXTERNAL) {
	        return new ImmutableJsonCreationStrategy(oc);
	    }
	    
	    return getDomainCreationStrategy(oc);
	}		
	
	@Override
	public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
		return new ImmutableJsonType(domainType, derivedClass);
	}		
	
	@Override
	public boolean immutable() {
		return true;
	}
	
	@Override
	public boolean isOpen(Class<?> clazz) {
		if(JsonObject.class.isAssignableFrom(clazz)
				|| JsonArray.class.isAssignableFrom(clazz) 
				|| JsonArrayBuilder.class.isAssignableFrom(clazz)) {
			return true;
		}
		
		return super.isOpen(clazz);
	}
}
