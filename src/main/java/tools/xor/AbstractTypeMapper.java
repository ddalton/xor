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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.xor.service.DataModel;
import tools.xor.service.DynamicShape;
import tools.xor.service.Shape;
import tools.xor.util.CreationStrategy;
import tools.xor.util.NaturalKeyStrategy;
import tools.xor.util.ObjectCreator;
import tools.xor.util.POJOCreationStrategy;

public abstract class AbstractTypeMapper implements TypeMapper {
    protected static final String DYNAMIC_SUFFIX = "_DYNAMIC"; // Added to domain shape name, if the dynamic shape is based on it 
    
	private MapperSide side;
    private DataModel model;
    private String shapeName;
    protected Shape domainShape;
    protected Shape dynamicShape;
    protected boolean persistenceManaged;
    
    protected static final Map<String, Class<?>> primitives = new HashMap<>();
    
    static {
        // primitives
            primitives.put(boolean.class.getName(), boolean.class);
            primitives.put(char.class.getName(), char.class);
            primitives.put(byte.class.getName(), byte.class);
            primitives.put(short.class.getName(), short.class);
            primitives.put(int.class.getName(), int.class);
            primitives.put(long.class.getName(), long.class);
            primitives.put(float.class.getName(), float.class);
            primitives.put(double.class.getName(), double.class);
    }    

    public AbstractTypeMapper()
    {
        this.side = MapperSide.DOMAIN;
        this.persistenceManaged = true;
    }
    
    public AbstractTypeMapper(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged)
    {
        this();
        
        this.model = das;
        this.side = side;
        this.shapeName = shapeName;
        this.persistenceManaged = persistenceManaged;
    }
    
    protected Class<?> getJavaClass(String name) {
        try {
            Class<?> clazz = primitives.get(name);
            if (clazz == null) {
                clazz = Class.forName(name);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        
        return null;
    }   
    
    @Override
    public boolean isPersistenceManaged() {
        return persistenceManaged;
    }

    public void setPersistenceManaged(boolean isPersistenceManaged) {
        this.persistenceManaged = isPersistenceManaged;
    }    

    @Override
	public DataModel getModel() {
        return model;
    }

    @Override
    public void setModel(DataModel das) {
        assert this.model == null : "TypeMapper instance already associated with a DAS instance";
        
        this.model = das;
    }

    @Override
    public String getShapeName() {
        return shapeName;
    }

    public void setShapeName(String shapeName) {
        this.shapeName = shapeName;
    }

    /**
	 * Should be overridden by subclasses to return the correct type
	 * 
     * @param das DataAccessService responsible for powering this TypeMapper instance
     * @param side TypeMapper targeted against EXTERNAL or DOMAIN side
     * @param shapeName name of the shape instance
     * @param persistenceManaged indicates if the model it represents is managed by a persistence store
     * @return TypeMapper instance
     */
	abstract protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged);
	
    @Override
    public TypeMapper newInstance(MapperSide side, String shapeName) {
        return newInstance(this.getModel(), side, shapeName, isPersistenceManaged());
    } 	
	
	@Override
    public String toDomain(String externalTypeName, BusinessObject bo) {
	    return toDomain(externalTypeName);
	}
	
    public String toDomain(String typeName) {
        return typeName;
    }
    
    public String toExternal(String typeName) {
        return typeName;
    }

	@Override
	public MapperSide getSide() {
		return side;
	}
	
	@Override
	public void setSide(MapperSide direction) {
		this.side = direction;
	}	

	@Override
	public boolean isExternal(String typeName) {
	    return true;
	}
	
	@Override
	public boolean isDomain(String typeName) {
	    return true;
	}
	
    @Override
    public String getMappedType(String typeName, CallInfo callInfo) {
        String result = null;

        switch(getSide()) {
        case EXTERNAL:          
            result = toExternal(typeName);
            break;
        case DOMAIN:            
            result = toDomain(typeName);
            break;
        default:
            result = typeName;
            break;
        }

        return result;
    }   	
	
	public boolean isExternalSide() {
		return getSide() == MapperSide.EXTERNAL;
	}

	@Override
	public CreationStrategy getCreationStrategy(ObjectCreator oc) {
		return getDomainCreationStrategy(oc);
	}	
	
	protected CreationStrategy getDomainCreationStrategy(ObjectCreator oc) {
	    return new POJOCreationStrategy(oc);
	}
	
	@Override
	public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
		return new ExternalType(domainType, derivedClass);
	}
	
	@Override
	public boolean immutable() {
		return false;
	}
	
	@Override
	public boolean isOpen(Class<?> clazz) {
		return false;
	}
	
	@Override
	public EntityKey getEntityKey(Object id, Type type) {
		return getEntityKey(id, type, null, null);
	}
	
	@Override
	public EntityKey getEntityKey(Object id, BusinessObject bo) {
		return getEntityKey(id, bo.getType(), bo, null);
	}	
	
	/**
	 * Return an entity key that is preferable NaturalEntityKey or a SurrogateEntityKey.
	 * EntityKeys are problematic for objects that are being created.
	 * For e.g., the id might not be present or the natural key fields might not have
	 * yet been set.
	 * 
	 * @param id optional identifier
	 * @param type can be domain or external type
	 * @param bo Must be an BusinessObject based on a domain EntityType if present
	 * @param anchor state tree path that determines the shape that we are interested in fetching
	 * @return EntityKey
	 */
	public EntityKey getEntityKey(Object id, Type type, BusinessObject bo, String anchor) {


		if (bo == null) {
			if (id == null) {
				return null;
			}
		} else {
			if(!(bo.getType() instanceof EntityType)) {
				return null;
			}
		}

		if(bo == null && !EntityType.class.isAssignableFrom(type.getClass()))
			throw new IllegalArgumentException("type has to refer to a EntityType");

		EntityType rootEntityType = null;
		
		if(bo == null) {
			rootEntityType = ((EntityType)type).getRootEntityType();
		} else {
			rootEntityType = (EntityType) bo.getType();
		}
		String domainTypeName = rootEntityType.getEntityName();

		if(id == null) {
			return null;
		}

		// Try to obtain by NaturalKey first and then the Surrogate Key
		// Helps with entity import from a different system
		if(rootEntityType.getNaturalKey() != null && bo != null) {
			try {
				return NaturalKeyStrategy.getInstance().execute(bo, getNaturalKeyTypeName(type), anchor);
			} catch (IllegalStateException ise) {
				//Fall through to surrogate key, the natural key values are not populated;
			}
		}

		if(!(id instanceof String) || !"".equals(id.toString().trim())) {
			return new SurrogateEntityKey(id, domainTypeName, anchor);
		}
		
		return null;
	}

	public EntityKey getSurrogateKey(Object id, Type type) {
		return getSurrogateKey(id, type, null);
	}

	public EntityKey getSurrogateKey(Object id, Type type, String anchor) {
		if(id == null) {
			return null;
		}

		if(!(type instanceof EntityType)) {
			return null;
		}

		EntityType entityType = (EntityType) type;

		if(anchor != null || (id != null && !"".equals(id.toString().trim()))) {
			return new SurrogateEntityKey(id, getSurrogateKeyTypeName(entityType), anchor);
		}

		return null;
	}

	public static String getSurrogateKeyTypeName (Type type) {
	    // Not all DataModels might share the surrogate key by the root entity type.
	    // This might have to be made configurable for each DataModel
		EntityType rootEntityType = ((EntityType)type).getRootEntityType();
		return rootEntityType.getEntityName();
	}

	public static String getNaturalKeyTypeName (Type type) {
		return ((EntityType)type).getEntityName();
	}

	public List<EntityKey> getNaturalKey(BusinessObject bo) {
		return getNaturalKey(bo, null);
	}

	/**
	 * Return a list of natural keys. There could be more than one if the supertype has
	 * natural keys.
	 * The keys being returned has to be kept in sync with the code that populates these
	 * fields.
	 * @see tools.xor.operation.GraphTraversal#process
	 *
	 * @param bo business object
	 * @param anchor path that determines the shape
	 * @return list of all keys including the natural keys of its super types
	 */
	public List<EntityKey> getNaturalKey(BusinessObject bo, String anchor) {
		List<EntityKey> result = new ArrayList<>();

		if (bo == null || bo.getInstance() == null) {
			return result;
		}

		if(!(bo.getType() instanceof EntityType)) {
			return result;
		}

		EntityType entityType = (EntityType) bo.getType();
		/*
		while(entityType != null && entityType.getNaturalKey() != null && bo != null) {
			try {
				EntityKey naturalEntityKey = NaturalKeyStrategy.getInstance().execute(
					bo, getNaturalKeyTypeName(
						entityType
					));
				if(naturalEntityKey != null) {
					result.add(naturalEntityKey);
				}
			} catch (IllegalStateException ise) {
				//the natural key values are not populated.
			}
			entityType = entityType.getSuperType();
		}
		*/

		if(entityType != null && entityType.getNaturalKey() != null && bo != null) {
			try {
				EntityKey naturalEntityKey = NaturalKeyStrategy.getInstance().execute(
					bo, getNaturalKeyTypeName(entityType), anchor);
				if(naturalEntityKey != null) {
					result.add(naturalEntityKey);
				}
			} catch (IllegalStateException ise) {
				//the natural key values are not populated.
			}
		}

		return result;
	}
	
	/**
	 * Find the domain property using the entityName.
	 * The pre-requisite here for this call to be successful, is for the related domain
	 * and the dynamic types to have the same entity name.
	 * 
	 * @param entityName of the type
	 * @param propertyName of the property
	 * @return property that is found in the type with entity name - entityName
	 */
	protected Property getDomainProperty(String entityName, String propertyName) {
	    EntityType type = (EntityType) getDomainShape().getType(entityName);
	    
	    // If an alias is used, then the mapping should be used here
	    return type.getProperty(propertyName);
	}
	
    @Override
    public void setDomainShape(Shape domainShape) {
        this.domainShape = domainShape;
    }

    @Override
    public void setDynamicShape(Shape dynamicShape) {
        this.dynamicShape = dynamicShape;
    }	
    
    @Override
    public Shape getShape() {
        if(getSide() == MapperSide.DOMAIN) {
            return getDomainShape();
        } else {
            return getDynamicShape();
        }
    }
    
    @Override
    public Shape getDomainShape() {
        if(this.domainShape == null) {
            this.domainShape = getModel().getShape(getShapeName());
            
            if(this.domainShape == null) {
                // create this shape
                this.domainShape = getModel().createShape(getShapeName());
            }
        }

        return this.domainShape;
    }

    @Override
    public Shape getDynamicShape() {
        if(this.dynamicShape == null) {
            this.dynamicShape = getModel().getShape(getShapeName()+DYNAMIC_SUFFIX);
            
            if(this.dynamicShape == null) {
                // create the dynamic shape
                this.dynamicShape = createDynamicShape(getDomainShape());
            }
        }

        return this.dynamicShape;
    }
    
    private Shape createDynamicShape(Shape domain) {
        if(domain == null) {
            return null;
        }
        
        Shape dynamicParent = createDynamicShape(domain.getParent());
        String name = domain.getName() + DYNAMIC_SUFFIX;
        Shape dynamic = getModel().getShape(name);
        if(dynamic == null) {
            dynamic = new DynamicShape(domain.getName()+DYNAMIC_SUFFIX, dynamicParent, domain, this);
            getModel().addShape(dynamic);
        }
        
        return dynamic;
    }

    @Override
    public void addProperty(ExtendedProperty property) {
        EntityType type = (EntityType) property.getContainingType();

        Property domainProperty = type.isDomainType() ? property : null;
        Property externalProperty = !type.isDomainType() ? property : null;
        if (externalProperty == null && type.isDomainType()) {
            ExternalType externalType = (ExternalType) getDynamicShape().getType(type.getEntityName());
            if (externalType != null) {
                externalProperty = externalType.defineProperty(domainProperty, getDynamicShape(), this);
            }
        }

        if(domainProperty != null) {
            getDomainShape().addProperty(domainProperty);
        }
        if(externalProperty != null) {
            getDynamicShape().addProperty(externalProperty);
        }
    }
    
    @Override
    public void addType(EntityType type) {
        if(getDomainShape().getType(type.getName()) != null) {
            throw new RuntimeException("A type with the same name exists, please choose a different name for the open type: " + type.getName());
        }

        type.setShape(getDomainShape());
        ((OpenType)type).setProperty();
        getDomainShape().addType(type.getName(), type);

        String externalClassName = getModel().getTypeMapper().toExternal(type.getInstanceClass() == null ? null : type.getInstanceClass().getName());
        try {
            Class<?> externalClass = Class.forName(externalClassName);
            if (externalClass != null) {
                ExternalType externalType = getModel().getTypeMapper().createExternalType(type, externalClass);
                getDynamicShape().addType(externalType.getName(), externalType);
                externalType.setProperty(getDomainShape(), getDynamicShape(), this);
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
}
