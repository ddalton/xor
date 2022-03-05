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

package tools.xor.operation;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ImmutableJsonProperty;
import tools.xor.ProcessingStage;
import tools.xor.Type;
import tools.xor.action.AddElementAction;
import tools.xor.action.CollectionUpdateAction;
import tools.xor.action.Executable;
import tools.xor.action.PropertyKey;
import tools.xor.action.RemoveElementAction;
import tools.xor.action.SetterAction;


public class ModifyOperation extends GraphTraversal {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private Object result;
	
	@Override
	protected void persist(CallInfo callInfo) {
		// We do not update the database for just a model conversion
		if(callInfo.getSettings().getAction() == AggregateAction.TO_DOMAIN) {
			return;
		}
		callInfo.getOutputObjectCreator().persistGraph(callInfo.getSettings());
	}		

	public void removeObsoleteEntities(CallInfo callInfo, CollectionUpdateAction migratorAction) {

		// If we are merging then obsolete elements are not removed 		
		if(callInfo.getSettings().doMerge(callInfo))
			return;		

		migratorAction.unlinkElements(callInfo);
	}	

	@Override
	public void processCollection(CallInfo callInfo) throws Exception {

		//if(callInfo.getStage() != ProcessingStage.UPDATE)
		//	return;		

		// Update the collection/map. This includes adding/removing/repositioning elements
		PropertyKey propertyKey = new PropertyKey((BusinessObject) callInfo.getParent().getOutput(), callInfo.getOutputProperty());
		CollectionUpdateAction migratorAction = callInfo.getOutputRoot().getObjectPersister().getOrCreateMigratorAction(propertyKey);

		if(callInfo.getStage() == ProcessingStage.UPDATE) {
			removeObsoleteEntities(callInfo, migratorAction);
		}
		migratorAction.linkElements(callInfo);
		//migratorAction.addOrReposition(callInfo);
	}

	@Override
	protected boolean shouldUnlink(CallInfo ci) {

		if(ci.getInput() == null || ci.getInputProperty() == null)
			return true;

		return ci.getInputProperty().isGenerated();
	}	

	@Override
	protected void processNullValue(CallInfo ci) throws Exception {

		if(ci.getSettings().doUpdate(ci) && ci.getStage() == ProcessingStage.UPDATE) {

			if(ci.getInputProperty() != null && ci.getInputProperty().isMany()) {
				ci.setOutput(createTarget(ci, ci.getOutputFromParent(ci.getSettings()), null)); // Get the persistent data object into target
				processCollection(ci);
			} else
				unlinkToOne(ci);
		}
	}	

	protected void unlinkToOne(CallInfo ci) {
		if(ci.getOutputProperty().getAssociationType() != PersistentAttributeType.ONE_TO_ONE &&
				ci.getOutputProperty().getAssociationType() != PersistentAttributeType.MANY_TO_ONE)
			return;
		
		// Unlinking for dynamic properties is handled by remove obsolete method
		if(ImmutableJsonProperty.class.isAssignableFrom(ci.getOutputProperty().getClass()))
			return;

		if(ci.getOutputFromParent(ci.getSettings()) == null)
			return;		

		// Set the target to null
		PropertyKey key = new PropertyKey((BusinessObject)ci.getParentOutputEntity(), ci.getOutputProperty());		
		Executable originalAction = new SetterAction(ci.getSettings(), null, key, null);
		ci.getOutputRoot().getObjectPersister().addAction(originalAction);		

		// bi-directional link
		if(ci.getOutputProperty().isBiDirectional()) {

			ci.setOutput(createTarget(ci, ci.getOutputFromParent(ci.getSettings()), null)); // Get the persistent data object into target
			PropertyKey oppositeKey = new PropertyKey((BusinessObject)ci.getOutput(), ci.getOutputProperty().getOpposite());

			// set the backRef from the opposite object to null	
			Executable oppositeAction = new SetterAction(ci.getSettings(), null, oppositeKey, originalAction);
			if(ci.getOutputProperty().getAssociationType() == PersistentAttributeType.MANY_TO_ONE) {
				Object position = (oppositeKey.getProperty() == null) ? null : ((ExtendedProperty)oppositeKey.getProperty()).getValue(key.getDataObject());
				oppositeAction = new RemoveElementAction(oppositeKey, key.getDataObject(), (BusinessObject) ci.getOutput(), originalAction, position, ci.getInputObjectCreator());				
			}
			ci.getOutputRoot().getObjectPersister().addAction(oppositeAction);			
		}
	}

	private boolean isSameTarget(CallInfo ci, Object value) {
		return ci.getOutputFromParent(ci.getSettings()) == ((BusinessObject)value).getInstance();
	}

	protected void linkToOne(CallInfo ci, Object value) {

		boolean isEmbeddedType = false;
		Type type = ci.getOutputProperty().getType();
		if(type instanceof EntityType && ((EntityType)type).isEmbedded()) {
			isEmbeddedType = true;
		}

		// We like all TO_ONE and embedded relationships
		if (ci.getOutputProperty().getAssociationType() != PersistentAttributeType.ONE_TO_ONE &&
			ci.getOutputProperty().getAssociationType() != PersistentAttributeType.MANY_TO_ONE &&
			!isEmbeddedType) {
			return;
		}

		// If this is a uni-directional OneToOne, we just have to set the target to value
		PropertyKey key            = new PropertyKey((BusinessObject)ci.getParentOutputEntity(), ci.getOutputProperty());
		Executable originalAction  = new SetterAction(ci.getSettings(), value, key, null);

		// Set the target to value
		if(!isSameTarget(ci, value))		
			ci.getOutputRoot().getObjectPersister().addAction(originalAction);			

		if(!isEmbeddedType && ci.getOutputProperty().isBiDirectional()) {
			PropertyKey newOppositeKey = new PropertyKey((BusinessObject)value, ci.getOutputProperty().getOpposite());	

			// If the target has an existing opposite object, its backRef needs to be set to null 
			if(ci.getOutputFromParent(ci.getSettings()) != null && !isSameTarget(ci, value) && !ImmutableJsonProperty.class.isAssignableFrom(ci.getOutputProperty().getClass())) {
				
				CallInfo newCall = new CallInfo(null, null, ci, ci.getOutputProperty());
				BusinessObject oldObject = (BusinessObject) createTarget(newCall, ci.getOutputFromParent(ci.getSettings()), null); // Get the persistent data object into target

				// Set the backRef of the opposite object to null
				PropertyKey oldOppositeKey = new PropertyKey(oldObject, ci.getOutputProperty().getOpposite());

				// Set the backRef from the old value to null
				Executable oppositeAction = new SetterAction(ci.getSettings(), null, oldOppositeKey, originalAction);				
				if(ci.getOutputProperty().getAssociationType() == PersistentAttributeType.MANY_TO_ONE) {
					Object position = getPosition((ExtendedProperty) oldOppositeKey.getProperty(), key.getDataObject());
					oppositeAction = new RemoveElementAction(oldOppositeKey, key.getDataObject(), oldObject, originalAction, position, ci.getInputObjectCreator());				
				}
				ci.getOutputRoot().getObjectPersister().addAction(oppositeAction);	
			}			 

			// Set the backRef of value to point to the parent target (or to the ancester in case of embedded property)
			Executable oppositeAction = new SetterAction(ci.getSettings(), ci.getParentOutputEntity(), newOppositeKey, originalAction);			
			if(ci.getOutputProperty().getAssociationType() == PersistentAttributeType.MANY_TO_ONE) {
				Object position = getPosition((ExtendedProperty) newOppositeKey.getProperty(), key.getDataObject());
				oppositeAction = new AddElementAction(newOppositeKey, key.getDataObject(), (BusinessObject) value, originalAction, position, ci.getInputObjectCreator());				
			}
			ci.getOutputRoot().getObjectPersister().addAction(oppositeAction);		
		}
	}
	
	private Object getPosition(ExtendedProperty property, BusinessObject collectionElement) {
		if(property == null)
			return null;
		
		if(!property.isDataType()) {
			if(property.getPositionProperty() != null)
				return ((ExtendedProperty)property.getPositionProperty()).getValue(collectionElement);
		}
		
		return null;
	}

	@Override
	protected Object setPropertyTarget(CallInfo ci, Object propertyTarget) {
		/*
		if(ci.getStage() != ProcessingStage.UPDATE)
			return propertyTarget;
			*/
		if(!shouldUpdate(ci)) {
			return propertyTarget;
		}

		// TODO: If the property is a mapKey or an indexKey then the toMany side needs to know about the reposition action 

		// Immediately make the change if in the CREATE stage and not in a delayed manner
		if(ci.getOutputProperty().isMany() || ci.getStage() == ProcessingStage.CREATE)
			super.setPropertyTarget(ci, propertyTarget);
		else 
			linkToOne(ci, propertyTarget);

		return propertyTarget;
	}

	@Override
	protected boolean isIdentifier(CallInfo ci)
	{
		if (ci.getSettings().getAction() == AggregateAction.TO_DOMAIN) {
			return false;
		}

		return super.isIdentifier(ci);
	}

	@Override
	protected void setResult(Object target) {
		this.result = target;
	}	

	@Override
	public Object getResult() {
		return result;
	}	

}
