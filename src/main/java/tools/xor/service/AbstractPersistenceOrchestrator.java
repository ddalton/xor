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

package tools.xor.service;

import java.io.Serializable;
import java.sql.Blob;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.hibernate.LockOptions;
import tools.xor.AbstractBO;
import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.RelationshipType;
import tools.xor.Settings;
import tools.xor.TypeMapper;
import tools.xor.util.ClassUtil;
import tools.xor.util.ObjectCreator;
import tools.xor.view.AggregateView;
import tools.xor.view.StoredProcedure;

public abstract class AbstractPersistenceOrchestrator implements PersistenceOrchestrator {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	protected abstract void createStatement (StoredProcedure sp);
	
	@Override
	public boolean supportsVersionTracking() {
		return true;
	}	
	
	@Override
	public boolean canProcessAggregate() {
		return false;
	}
	
	@Override 
	public void clear() {
		// Overridden by subclasses
	}

	@Override
	public void clear(Set<Object> ids) {
		// Overridden by subclasses
	}
	
	@Override 
	public void refresh(Object object) {
		// Overridden by subclasses
	}	

	private Object getByUserKey(CallInfo callInfo, EntityType type) {
		BusinessObject from = (BusinessObject) callInfo.getInput();

		if(type.getNaturalKey() != null) {
			Map<String, Object> param = new HashMap<String, Object>();
			for(String key: type.getExpandedNaturalKey()) {
				if(from.get(key) == null)
					continue;
				
				param.put(key, from.get(key) );
			}
			if(param.size() == 0) {
				return null;
			}

			return findByProperty(from.getDomainType(), param);
		} else
			return null;
	}

	/**
	 * Give a chance to shortcircuit the persistence loading if we know for sure that the
	 * given object is a transient object.
	 * For example, we know that the identifier was generated when the object was created etc.
	 *
	 * @param from the user given object
	 * @return true if the given object is a transient object
	 */
	protected boolean isTransient(BusinessObject from) {
		return false;
	}
	
	@Override
	public Object getPersistentObject(CallInfo callInfo, TypeMapper typeMapper) {
		if(callInfo.isBulkInput()) {
			return null;
		}

		BusinessObject from = (BusinessObject) callInfo.getInput();
		Object persistentObject = null;

		if(!EntityType.class.isAssignableFrom(from.getType().getClass()))
			return null;

		EntityType type = (EntityType) from.getType();

		if(type.isEmbedded()) // We don't separately load embedded values from the database
			return null;

		if(isTransient(from)) {
			return null;
		}

		if(!(callInfo.getSettings().getAction() == AggregateAction.CLONE) )
			persistentObject = getByUserKey(callInfo, type);

		if(persistentObject == null) {
			ExtendedProperty identifierProperty = (ExtendedProperty) type.getIdentifierProperty();
			if(identifierProperty == null) {
				logger.error("Type without identifier: " + type.getName());
			}

			Serializable id = (Serializable) identifierProperty.getValue(from);
			if(id != null && !"".equals(id)) {
				Class<?> desiredClass = typeMapper.toDomain(
					type.isDomainType() ?
						type.getInstanceClass() :
						type.getDomainType().getInstanceClass(), from);
				persistentObject = findById(desiredClass, id);
			} 
		}

		return persistentObject;
	}
	
	@Override
	public Object getTargetObject(BusinessObject source, String openPropertyName) {
		ExtendedProperty property = (ExtendedProperty) source.getType().getProperty(openPropertyName);
		if(property.getRelationshipType() == RelationshipType.TO_ONE) {
			Map<String, String> keyFields = property.getKeyFields();
			
			if(keyFields.size() == 1) {
				Map.Entry<String, String> entry = keyFields.entrySet().iterator().next();
				if( ((EntityType)property.getType()).getIdentifierProperty().getName().equals(entry.getValue()) ) {
					// The value in the source object referenced by the key property 
					// is the id value of the target object
					return findById(property.getType().getInstanceClass(), source.get(entry.getKey()));
				}
			} else {
				// For all other cases we query the DB
				Map<String, Object> params = new HashMap<String, Object>();
				for(Map.Entry<String, String> entry: keyFields.entrySet()) {
					params.put(entry.getValue(), source.get(entry.getKey()));
				}
				return findByProperty(property.getType(), params);
			}
		}		
		
		// TODO: Handle TO_MANY
		
		return null;
	}

	@Override
	public Object getCached(Class<?> persistentClass, Object id) {
		return null;
	}	
	

	protected void performAttach(BusinessObject input, Object instance) {
		throw new UnsupportedOperationException("The reattach operation is either not supported or not yet implemented");
	}

	@Override
	public Object attach (BusinessObject input, BusinessObject snapshot, Settings settings)
	{
		Object instance = null;

		AggregateView view = settings.getView();
		EntityType type = (EntityType)settings.getEntityType();
		if (view.getStateGraph(type).supportsDynamicUpdate()) {

			EntityType entityType = (EntityType)settings.getEntityType();
			ObjectCreator oc = input.getObjectCreator();
			try {
				instance = AbstractBO.createInstance(
					input.getObjectCreator(),
					input.getIdentifierValue(),
					null,
					entityType,
					true);
				if(entityType.getVersionProperty() != null ) {
					((ExtendedProperty)entityType.getVersionProperty()).setValue(oc.getSettings(), instance, input.getVersionValue());
				} else {
					ClassUtil.initSingleLevel(input, snapshot, settings);
				}

				// At this point it is implementation specific, and it overridden by subclass implementations
				performAttach(input, instance);
			}
			catch (Exception e) {
				throw ClassUtil.wrapRun(e);
			}

		}
		else {
			throw new UnsupportedOperationException(
				"The entity type " + settings.getEntityType().getName()
					+ " does not support dynamic update for the view " + view.getName());
		}

		return instance;
	}
	
	@Override
	public boolean supportsStoredProcedure() {
		return false;
	}
}
