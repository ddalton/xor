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

import tools.xor.service.DataAccessService;
import tools.xor.service.DynamicShape;
import tools.xor.service.Shape;
import tools.xor.util.CreationStrategy;
import tools.xor.util.ImmutableJsonCreationStrategy;
import tools.xor.util.ObjectCreator;

public class ImmutableJsonTypeMapper extends AbstractTypeMapper {
	
	private String domainPackagePath;
    private Shape domainShape;
    private Shape dynamicShape; 	
	
	private static final Map<Class<?>, Class<?>> javaToJson = new HashMap<Class<?>, Class<?>>();
	
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
	}
	
    public ImmutableJsonTypeMapper() {
        super();
    }	
	
    public ImmutableJsonTypeMapper(DataAccessService das, MapperSide side, String shapeName) 
    {
        super(das, side, shapeName);
    }	

	public String getDomainPackagePath() {
		return domainPackagePath;
	}

	public void setDomainPackagePath(String domainPackagePath) {
		this.domainPackagePath = domainPackagePath;
	}

	@Override
	public Class<?> toDomain(Class<?> externalClass) {
		if(!isDomain(externalClass)) {
			throw new UnsupportedOperationException("Cannot resolve the domain class from a JSON object");
		}
		return externalClass;
	}
	
	@Override
	public Class<?> getMappedClass(Class<?> clazz, CallInfo callInfo) {
		Class<?> result = null;

		switch(getSide()) {
		case EXTERNAL:			
			result = toExternal(clazz);
			break;
		case DOMAIN:		
			try {
				result = toDomain(clazz);
			} catch (UnsupportedOperationException e) {
				if(callInfo.getInputProperty() != null) {
                    Property domainProperty = getDomainProperty(callInfo.getInputProperty().getContainingType().getEntityName(), callInfo.getInputProperty().getName());				    
					result = domainProperty.getType().getInstanceClass();
				} else {
					// Collection, so go to the owner and get the element type
					ExtendedProperty property = callInfo.getParent().getInputProperty();
                    Property domainProperty = getDomainProperty(property.getContainingType().getEntityName(), property.getName());
					result = ((ExtendedProperty)domainProperty).getElementType().getInstanceClass();
				}
			}
			break;
		default:
			result = clazz;
			break;
		}

		return result;
	}		
	
	@Override
	/**
	 * Return the domain class from the external type
	 */
	public Class<?> toDomain(Type type) {
		throw new UnsupportedOperationException("Cannot resolve the domain class from a JSON object");
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
	public Class<?> toExternal(Class<?> domainClass) {
		
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
	public boolean isExternal(Class<?> clazz) {
		return clazz.isAssignableFrom(JsonValue.class);
	}
	
	@Override
	public boolean isDomain(Class<?> clazz) {
		return (clazz.getCanonicalName().startsWith(domainPackagePath));
	}

	@Override
	protected TypeMapper createInstance(DataAccessService das, MapperSide side, String shapeName) {
		return new ImmutableJsonTypeMapper(das, side, shapeName);
	}
	
    @Override 
    public TypeMapper newInstance(MapperSide side) {
        return newInstance(side, null);
    }
    
    @Override
    public TypeMapper newInstance(DataAccessService das, MapperSide side, String shapeName) {
        ImmutableJsonTypeMapper mapper = (ImmutableJsonTypeMapper)createInstance(das, side, shapeName);
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
	public String getExternalTypeName(Class<?> inputClass, EntityType domainType) {
		return domainType.getEntityName();
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
