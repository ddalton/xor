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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.AggregateAction;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ExtendedProperty;
import tools.xor.ProcessingStage;
import tools.xor.operation.AbstractOperation;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;


public class ListUpdateAction extends CollectionUpdateAction {
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);

	private PropertyKey key;
	private List<AddElementAction>        addActions = new ArrayList<AddElementAction>();
	private List<RemoveElementAction>     removeActions = new ArrayList<RemoveElementAction>();

	public ListUpdateAction(PropertyKey key) {
		this.key = key;
	}

	@Override
	public void execute() {
		boolean collectionModified = false;

		// TODO: remove elements

		// Sort by position
		Map<Integer, AddElementAction> listOrderedActions = new TreeMap<Integer, AddElementAction>();

		for(AddElementAction action: addActions) 
			listOrderedActions.put(Integer.parseInt(action.getPosition().toString()), action);
		// TODO: Handler conflicts
		for(AddElementAction action: addByOppositeActions) 
			listOrderedActions.put(Integer.parseInt(action.getPosition().toString()), action);		

		for(AddElementAction action: listOrderedActions.values()) {
			// Add the element to the List
			addToList(action);		
			collectionModified = true;			
		}
		
		// TODO: checkOutOfSync()
	
	}		

	@Override
	public Map<Object, Set<String>> getElementKeysMap(BusinessObject input) {
		List list = (List) input.getInstance();

		Map<Object, Set<String>> result = new HashMap<Object, Set<String>>();
		for(int i = 0; i < list.size(); i++) {
			Object object = list.get(i);
			Set<String> keys = result.get(object);
			if(keys == null) {
				keys = new HashSet<String>();
				result.put(object, keys);
			}
			keys.add(Integer.toString(i));
		}

		return result;
	}	

	@Override
	public void addAction(Executable action) {

		if(action instanceof AddElementAction)
			addActions.add((AddElementAction) action);
		else if(action instanceof RemoveElementAction)
			removeActions.add((RemoveElementAction) action);		
		else
			throw new IllegalArgumentException("A list can have only add, remove actions");
	}

	@Override
	public PropertyKey getKey() {
		return key;
	}

	public void unlinkElements(CallInfo callInfo) {
		super.unlinkElements(callInfo);

		BusinessObject input  = ((BusinessObject)callInfo.getInput());
		BusinessObject output = ((BusinessObject)callInfo.getOutput());

		if(output == null)
			return;

		List inputList = (List) input.getInstance();
		List outputList = (List) output.getInstance();

		if(outputList.size() > inputList.size()) // truncate the remaining portion
			truncateToSize(outputList, inputList.size());
	}

	private void truncateToSize(List outputList, int toSize) {
		assert(outputList.size() >= toSize);

		for(int i = outputList.size(); i > toSize; i--) 
			outputList.remove(i-1);
	}

	public void linkElement(CallInfo next, String dynamicProperty, boolean isNew) throws Exception {
		AbstractOperation oper = (AbstractOperation) next.getOperation();
		BusinessObject targetElement = (BusinessObject) next.getOutput();
		if(next.getSettings().getAction() != AggregateAction.CREATE && targetElement != null) // updating an existing object
			next.setOutput(targetElement);
		else
			next.setOutput(oper.createTarget(next, null));

		if(owLogger.isDebugEnabled()) {
			if(isNew && next.getStage() == ProcessingStage.CREATE) {
				owLogger.debug(next.getIndentString() + "[Creating a new list element]");
			}
		}
		
		oper.processAttribute(next);
		
		// Do not modify the data graph unless we are in the right stage
		if(next.getStage() != ProcessingStage.UPDATE) {
			return;
		}

		// AddElementAction
		int i = Integer.parseInt(dynamicProperty);
		addListElement(next, (BusinessObject) next.getOutput(), i);

	}

	protected void addListElement(CallInfo callInfo, BusinessObject collectionElement, Object position) {
		Object collection = callInfo.getParent().getOutput();
		BusinessObject collectionOwner = (BusinessObject) callInfo.getParent().getParent().getOutput();

		PropertyKey propertyKey = new PropertyKey((BusinessObject) collection, callInfo.getParent().getOutputProperty());
		Executable originalAction = new AddElementAction(propertyKey, collectionElement, collectionOwner, null, position, callInfo.getInputObjectCreator());
		this.addAction(originalAction);		

		// Set the backRef of value to point to the parent target (or to the ancester in case of embedded property)
		// TODO: handle MANY_TO_MANY backRef
		if(callInfo.getParent().getOutputProperty().isBiDirectional()) {
			PropertyKey newOppositeKey = new PropertyKey(collectionElement, callInfo.getParent().getOutputProperty().getOpposite());			
			callInfo.getOutputRoot().getObjectPersister().addAction(new SetterAction(callInfo.getSettings(), collectionOwner, newOppositeKey, originalAction));
		}
	}

	protected void addToList(AddElementAction action) {

		BusinessObject collectionOwner   = action.getCollectionOwner();
		Object collection                    = ((ExtendedProperty) action.getKey().getProperty()).getValue(collectionOwner);
		BusinessObject collectionElement = action.getCollectionElement();
		ExtendedProperty collectionProperty  = (ExtendedProperty) action.getKey().getProperty();

		// It is possible for the collection object to be null, if this is due to the MANY_TO_ONE end only being set
		try {
			if(collection == null) {
				collection = createCollection(collectionOwner, collectionProperty);
			} 
		} catch(Exception e) {
			throw ClassUtil.wrapRun(e);
		}


		if(((java.util.Collection<Object>)collection).size() == Integer.parseInt(action.getPosition().toString()) )
			((java.util.Collection<Object>)collection).add(ClassUtil.getInstance(collectionElement));
		else
			throw new IllegalArgumentException("Trying to add a list element at the wrong position: " + action.getPosition() + ", list size: " + 
					((java.util.Collection<Object>)collection).size());

	}		
}
