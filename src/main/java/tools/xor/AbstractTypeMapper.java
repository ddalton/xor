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

import tools.xor.util.CreationStrategy;
import tools.xor.util.NaturalKeyStrategy;
import tools.xor.util.ObjectCreator;
import tools.xor.util.POJOCreationStrategy;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTypeMapper implements TypeMapper {
	private MapperDirection direction;

	/**
	 * Should be overridden by subclasses to return the correct type
	 * @return TypeMapper instance
	 */
	abstract protected TypeMapper createInstance();

	@Override
	public Class<?> toDomain(Type type) {
		return toDomain(type.getInstanceClass());
	}
	
	@Override
	public Class<?> toDomain(Class<?> externalClass, BusinessObject bo) {
		return toDomain(externalClass);
	}	

	@Override
	public MapperDirection getDirection() {
		return direction;
	}
	
	@Override
	public void setDirection(MapperDirection direction) {
		this.direction = direction;
	}	
	
	@Override
	public boolean isExternal(Class<?> clazz) {
		return true;
	}
	
	@Override
	public boolean isDomain(Class<?> clazz) {
		return true;
	}	
	
	@Override
	public Class<?> getSourceClass(Class<?> clazz, CallInfo callInfo) {
		Class<?> result = null;

		switch(getDirection()) {
		case EXTERNALTODOMAIN:
		case EXTERNALTOEXTERNAL:			
			result = toExternal(clazz);
			break;
		case DOMAINTOEXTERNAL:
		case DOMAINTODOMAIN:			
			result = toDomain(clazz);
			break;
		default:
			result = clazz;
			break;
		}

		return result;
	}
	
	
	@Override
	public Class<?> getTargetClass(Class<?> clazz, CallInfo callInfo) {
		Class<?> result = null;

		switch(getDirection()) {
		case DOMAINTOEXTERNAL:
		case EXTERNALTOEXTERNAL:			
			result = toExternal(clazz);
			break;
		case EXTERNALTODOMAIN:
		case DOMAINTODOMAIN:			
			result = toDomain(clazz);
			break;
		default:
			result = clazz;
			break;
		}

		return result;
	}	
	
	public boolean isToExternal() {
		return getDirection() == MapperDirection.DOMAINTOEXTERNAL ||
				getDirection() == MapperDirection.EXTERNALTOEXTERNAL;
	}

	@Override
	public CreationStrategy getCreationStrategy(ObjectCreator oc) {
		return new POJOCreationStrategy(oc);
	}	
	
	@Override
	public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
		return new ExternalType(domainType, derivedClass);
	}
	
	@Override
	public String getExternalTypeName(Class<?> inputClass, Type domainType) {
		return inputClass.getName();
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
		return getEntityKey(id, type, null);
	}
	
	@Override
	public EntityKey getEntityKey(Object id, BusinessObject bo) {
		return getEntityKey(id, bo.getType(), bo);
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
	 * @return EntityKey
	 */
	public EntityKey getEntityKey(Object id, Type type, BusinessObject bo) {


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

		String domainTypeName = rootEntityType.getDomainType().getName();

		if(id == null) {
			return null;
		}

		// Try to obtain by NaturalKey first and then the Surrogate Key
		// Helps with entity import from a different system
		if(rootEntityType.getNaturalKey() != null && bo != null) {
			try {
				return NaturalKeyStrategy.getInstance().execute(bo, domainTypeName);
			} catch (IllegalStateException ise) {
				//Fall through to surrogate key, the natural key values are not populated;
			}
		}

		if(!(id instanceof String) || !"".equals(id.toString().trim())) {
			return new SurrogateEntityKey(id, domainTypeName);
		}
		
		return null;
	}

	public EntityKey getSurrogateKey(Object id, Type type) {
		if(id == null) {
			return null;
		}

		if(!(type instanceof EntityType)) {
			return null;
		}

		EntityType entityType = (EntityType) type;

		if(!(id instanceof String) || !"".equals(id.toString().trim())) {
			return new SurrogateEntityKey(id, getSurrogateKeyTypeName(entityType));
		}

		return null;
	}

	public static String getSurrogateKeyTypeName (Type type) {
		EntityType rootEntityType = ((EntityType)type).getRootEntityType();
		return rootEntityType.getDomainType().getName();
	}

	public static String getNaturalKeyTypeName (Type type) {
		return ((EntityType)type).getDomainType().getName();
	}

	/**
	 * Return a list of natural keys. There could be more than one if the supertype has
	 * natural keys.
	 * The keys being returned has to be kept in sync with the code that populates these
	 * fields.
	 * @see tools.xor.operation.GraphTraversal#process
	 *
	 * @param bo business object
	 * @return list of all keys including the natural keys of its super types
	 */
	public List<EntityKey> getNaturalKey(BusinessObject bo) {
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
					bo, getNaturalKeyTypeName(
						entityType
					));
				if(naturalEntityKey != null) {
					result.add(naturalEntityKey);
				}
			} catch (IllegalStateException ise) {
				//the natural key values are not populated.
			}
		}

		return result;
	}
}
