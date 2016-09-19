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


public abstract class AbstractTypeMapper implements TypeMapper {
	private MapperDirection direction;
	
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
	 * Return an entity key that is preferable NaturalEntityKey or a SurrogateEntityKey
	 * 
	 * @param id optional identifier
	 * @param type can be domain or external type
	 * @param bo Must be an BusinessObject based on a domain EntityType if present
	 * @return EntityKey
	 */
	public EntityKey getEntityKey(Object id, Type type, BusinessObject bo) {
				
		if(id == null)
			return null;

		if(bo == null && !EntityType.class.isAssignableFrom(type.getClass()))
			throw new IllegalArgumentException("type has to refer to a Data Object");

		EntityType rootEntityType = null;
		
		if(bo == null) {
			rootEntityType = ((EntityType)type).getRootEntityType();
		} else {
			rootEntityType = (EntityType) bo.getType();
		}

		String domainTypeName = rootEntityType.getDomainType().getName();
		//if(ExternalType.class.isAssignableFrom(rootEntityType.getClass())) {
		//	domainTypeName = toDomain(rootEntityType.getInstanceClass(), bo).getName();
		//}

		// Try to obtain by NaturalKey
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
}
