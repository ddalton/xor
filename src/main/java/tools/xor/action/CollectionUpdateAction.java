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

package tools.xor.action;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.util.ClassUtil;


public abstract class CollectionUpdateAction implements Executable {
	protected Set<AddElementAction> addByOppositeActions = new HashSet<AddElementAction>();
	protected Set<RemoveElementAction> removeByOppositeActions = new HashSet<RemoveElementAction>();	

	public abstract void addAction(Executable action);

	public abstract void linkElement(CallInfo callInfo, String dynamicPropertyName, boolean isNew) throws Exception;

	public BusinessObject getTriggerObject() {
		throw new UnsupportedOperationException("It is not meaningful to call this method on a CollectionUpdateAction since a collection can be updated from multiple sources and does not represent a single execution");
	}

	public void addTriggeredByOppositeAction(ElementAction action) {

		if(action instanceof AddElementAction)
			addByOppositeActions.add((AddElementAction) action);
		else if(action instanceof RemoveElementAction)
			removeByOppositeActions.add((RemoveElementAction) action);		
	}

	@Override
	public Executable getTriggeringAction() {
		return null;
	}
	
	protected Object createCollection(BusinessObject collectionOwner, ExtendedProperty collectionProperty) throws Exception {
		BusinessObject collectionDataObject = (BusinessObject) collectionOwner.createDataObject((Object)null, collectionProperty.getType());
		Object collection = collectionDataObject.getInstance();
		collectionProperty.setValue(collectionOwner, collection);
		
		// Update the container attributes
		if(collectionProperty.isContainment()) {
			collectionDataObject.setContainer(collectionOwner);
			collectionDataObject.setContainmentProperty(collectionProperty);
		}		
		
		return collection;
	}

	public void linkElements(CallInfo callInfo) throws Exception {
		BusinessObject input  = ((BusinessObject)callInfo.getInput());
		BusinessObject output = ((BusinessObject)callInfo.getOutput());

		Map outputMap = new HashMap<Object, BusinessObject>();
		for(BusinessObject element: output.getList()) {
			if (element.getCollectionElementKey(callInfo.getOutputProperty()) != null) {
				outputMap.put(
					element.getCollectionElementKey(callInfo.getOutputProperty()),
					element);
			}
		}

		if(input != null) {
			// Create the data objects and load them into the input ObjectCreator cache
			input.getList();
			processLinks(outputMap, input, callInfo);
		}
	}

	public void processLinks (Map outputMap,
							  BusinessObject input,
							  CallInfo callInfo) throws
		Exception
	{
		Collection collection = null;
		if (input.getInstance() instanceof Collection) {
			collection = (Collection)input.getInstance();
		}
		else if (input.getInstance() instanceof JSONArray) {
			collection = ClassUtil.jsonArrayToList((JSONArray)input.getInstance());
		}

		for (Object obj : collection) {
			processLink(null, obj, callInfo, outputMap);
		}
	}
	
	protected void processLink(String key, Object sourceInstance, CallInfo callInfo, Map outputMap) throws Exception {
		BusinessObject input  = ((BusinessObject)callInfo.getInput());
		BusinessObject sourceElement = input.getObjectCreator().getExistingDataObject(sourceInstance);
		// element should not be null since it should have been loaded earlier
		// through call to list
		Object id = sourceElement.getCollectionElementKey(callInfo.getInputProperty());
		boolean isNew = id == null || !outputMap.containsKey(id);

		CallInfo next = new CallInfo();
		next.init(sourceElement, null, callInfo, null);
		
		EntityType targetType = callInfo.getDomainType((tools.xor.EntityType)sourceElement.getType());
		next.setOutput(callInfo.getOutputObjectCreator().createTarget(next, null, targetType));
		
		linkElement(next, key, isNew);		
	}

	public void unlinkElements(CallInfo callInfo) {

		/* algorithm
		 * 1. All the objects in the targetCollection should have ids since these are entities
		 * 2. We get a list of all ids from the sourceCollection
		 * 3. All ids that are not present in the sourceCollection are obsolete and needs to be removed
		 * 
		 * For a Map, we only honor entity in the value and not in the key. So we just need to check the values collection
		 * of the source and target maps.
		 */		
		BusinessObject input  = ((BusinessObject)callInfo.getInput());
		BusinessObject output = ((BusinessObject)callInfo.getOutput());

		Map targetKeys = new HashMap<Object, Object>();
		for(BusinessObject element: output.getList()) {
			targetKeys.put(element.getCollectionElementKey(callInfo.getOutputProperty()), element);
		}

		Map sourceKeys = new HashMap<Object, Object>();		
		if(callInfo.getInput() != null) 
			for(BusinessObject element: input.getList()) {
				Object idValue = element.getCollectionElementKey(callInfo.getInputProperty());
				if(idValue != null)
					sourceKeys.put(idValue, element);
			}			

		for(Object entry : targetKeys.entrySet()) {
			Object targetKey = ((Map.Entry) entry).getKey();
			Object targetElement = ((Map.Entry) entry).getValue();

			if(!sourceKeys.containsKey(targetKey)) { // obsolete element being marked for removal
				CallInfo next = new CallInfo();
				next.init(null, targetElement, callInfo, null);
				next.setOutput(callInfo.getOutputObjectCreator().createTarget(next, targetElement, null));
				unlinkElement(next, (BusinessObject) next.getOutput(), targetKey);
			}
		}		

	}

	protected void unlinkElement(CallInfo callInfo, BusinessObject collectionElement, Object position) {
		BusinessObject collectionOwner = (BusinessObject) callInfo.getParent().getParent().getOutput();

		PropertyKey propertyKey = new PropertyKey((BusinessObject) callInfo.getParent().getOutput(), callInfo.getParent().getOutputProperty());
		Executable originalAction = new RemoveElementAction(propertyKey, collectionElement, collectionOwner, null, position, callInfo.getInputObjectCreator());
		this.addAction(originalAction);		

		// Set the backRef value to null. This is an asynchronous activity
		if(callInfo.getParent().getOutputProperty().isBiDirectional()) {
			PropertyKey newOppositeKey = new PropertyKey(collectionElement, callInfo.getParent().getOutputProperty().getOpposite());			
			callInfo.getOutputRoot().getObjectPersister().addAction(new SetterAction(callInfo.getSettings(), null, newOppositeKey, originalAction)); 	
		}

	}		
}
