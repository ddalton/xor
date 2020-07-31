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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.service.DataModel;
import tools.xor.util.Constants;
import tools.xor.util.CreationStrategy;
import tools.xor.util.ExcelJsonCreationStrategy;
import tools.xor.util.MutableJsonCreationStrategy;
import tools.xor.util.ObjectCreator;

public class MutableJsonTypeMapper extends AbstractTypeMapper {
	
	private String domainPackagePath;
	
	private static final Set<Class<?>> unchanged = new HashSet<Class<?>>();
	private static final Set<String> unchangedNames = new HashSet<>();
	
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
		unchanged.add(BigDecimal.class);
		unchanged.add(BigInteger.class);
		
		// primitives
		unchanged.add(boolean.class);
		unchanged.add(char.class);
		unchanged.add(byte.class);
		unchanged.add(short.class);
		unchanged.add(int.class);
		unchanged.add(long.class);
		unchanged.add(float.class);
		unchanged.add(double.class);	
		
		for(Class<?> clazz: unchanged) {
		    unchangedNames.add(clazz.getName());
		}
	}

	public static synchronized void addUnchanged(Class clazz) {
		unchanged.add(clazz);
	}

	public static Set<Class<?>> getUnchanged() {
		return unchanged;
	}
	
    public MutableJsonTypeMapper() {
        super();
    }	
	
    public MutableJsonTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) 
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
        if (domainShape != null) {
            Type domainType = this.domainShape.getType(typeName);
            if (domainType != null) {
                return domainType.getName();
            }
        } else {
            if (isDomain(typeName)) {
                return typeName;
            }
        }

        return null;
    }	

	public static Class getEntityClass(final JSONObject jsonObject) {
		if(jsonObject.has(Constants.XOR.TYPE)) {
			String className = jsonObject.getString(Constants.XOR.TYPE);
			try {
				return Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unable to find domain entity class: " + className);
			}
		}

		throw new UnsupportedOperationException("Cannot resolve the domain class from a JSON object");
	}
	
    public static String getEntityName(final JSONObject jsonObject) {
        if(jsonObject.has(Constants.XOR.TYPE)) {
            return jsonObject.getString(Constants.XOR.TYPE);
        }

        throw new UnsupportedOperationException("Cannot resolve the domain class from a JSON object");
    }	
	
    @Override
    public String toDomain(String externalTypeName, BusinessObject bo) {
        if(!isDomain(externalTypeName)) {
            if(bo != null && bo.getInstance() != null) {
                if(bo.getInstance() instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) bo.getInstance();
                    return getEntityName(jsonObject);
                } else if(bo.getInstance() instanceof JSONArray) {
                    JSONObject container = (JSONObject) ((BusinessObject)bo.getContainer()).getInstance();
                    Property containmentProperty = bo.getContainmentProperty();
                    if(container != null && containmentProperty != null) {
                        String collectionTypeKey = ExcelJsonCreationStrategy.getCollectionTypeKey(containmentProperty);
                        if(container.has(collectionTypeKey)) {
                            return container.getString(collectionTypeKey);
                        }
                    }
                }
            } 
            throw new UnsupportedOperationException("Cannot resolve the domain class from a JSON object");
        }
        return externalTypeName;        
    }	
	
    public String getMappedType(String typeName, CallInfo callInfo) {
        String result = null;

        switch(getSide()) {
        case EXTERNAL:      
            result = toExternal(typeName);
            break;
        case DOMAIN:
            try {
                result = toDomain(typeName, (BusinessObject) callInfo.getInput());
            } catch (UnsupportedOperationException e) {
                if(callInfo.getInputProperty() != null) {
                    Property domainProperty = getDomainProperty(callInfo.getInputProperty().getContainingType().getEntityName(), callInfo.getInputProperty().getName());                    
                    result = domainProperty.getType().getName();
                } else {
                    if(callInfo.getParent() == null) {
                        throw new RuntimeException("Unable to infer the entity type, provide this information using XOR:type field");
                    }
                    // Collection, so go to the owner and get the element type
                    ExtendedProperty property = callInfo.getParent().getInputProperty();
                    Type type = null;
                    if(property == null && callInfo.getParent().isBulkInput()) {
                        type = callInfo.getSettings().getEntityType();
                    } else {
                        Property domainProperty = getDomainProperty(property.getContainingType().getEntityName(), property.getName());                      
                        type = ((ExtendedProperty)domainProperty).getElementType();
                    }
                    result = type.getName();
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

	    Class<?> domainClass = type == null ? null : type.getInstanceClass();
	    
		// domainClass can be null for an open type
		if(domainClass != null) {
			if (getUnchanged().contains(domainClass)) {
				return domainClass;
			}

			if (Set.class.isAssignableFrom(domainClass) ||
				List.class.isAssignableFrom(domainClass) ||
				domainClass.isArray()) {
				return JSONArray.class;
			}
		}
		
		return JSONObject.class;		
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

        if(typeName != null) {
            if (unchangedNames.contains(typeName)) {
                return typeName;
            }

            Class<?> domainClass;
            try {
                domainClass = Class.forName(typeName);
                if(Collection.class.isAssignableFrom(domainClass)) {
                    return JSONArray.class.getName();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        return JSONObject.class.getName();        
    }	
	
	@Override
    public boolean isExternal(String typeName) {
	    return unchangedNames.contains(typeName) || JSONObject.class.getName().equals(typeName) || JSONArray.class.getName().equals(typeName);
	}	
	
    @Override
    public boolean isDomain(String typeName) {
        if(domainPackagePath == null) {
            return super.isDomain(typeName);
        }
        return (typeName.startsWith(domainPackagePath));
    }   	
	
    @Override 
    public TypeMapper newInstance(MapperSide side) {
        return newInstance(side, null);
    }
    
    @Override
    public TypeMapper newInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        MutableJsonTypeMapper mapper = (MutableJsonTypeMapper)createInstance(das, side, shapeName, persistenceManaged);
        mapper.setDomainPackagePath(getDomainPackagePath());

        return mapper;        
    }       

    @Override
	protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
		return new MutableJsonTypeMapper(das, side, shapeName, persistenceManaged);
	}
	
	@Override
	public CreationStrategy getCreationStrategy(ObjectCreator oc) {
        if(getSide() == MapperSide.EXTERNAL) {
            return new MutableJsonCreationStrategy(oc);
        }
        
        return getDomainCreationStrategy(oc);	    
	}		
	
	@Override
	public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
		return new MutableJsonType(domainType, derivedClass);
	}		
	
	@Override
	public boolean immutable() {
		return false;
	}
	
	@Override
	public boolean isOpen(Class<?> clazz) {
		if(JSONObject.class.isAssignableFrom(clazz)
				|| JSONArray.class.isAssignableFrom(clazz) ) {
			return true;
		}
		
		return super.isOpen(clazz);
	}
}
