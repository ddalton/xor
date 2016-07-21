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
	
	public EntityKey getEntityKey(Object id, Type type, BusinessObject bo) {
		if(id == null)
			return null;

		if(!EntityType.class.isAssignableFrom(type.getClass()))
			throw new IllegalArgumentException("type has to refer to a Data Object");

		Type rootEntityType = ((EntityType)type).getRootEntityType();

		String domainTypeName = rootEntityType.getName();
		String externalTypeName = rootEntityType.getName();
		if(ExternalType.class.isAssignableFrom(rootEntityType.getClass()))
			domainTypeName = toDomain(rootEntityType.getInstanceClass(), bo).getName();
		else
			externalTypeName = toExternal(rootEntityType.getInstanceClass()).getName();

		EntityKey key =  new EntityKey(id, domainTypeName, externalTypeName);

		return key;
	}
}
