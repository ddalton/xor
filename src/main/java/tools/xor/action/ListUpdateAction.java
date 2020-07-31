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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ExtendedProperty;
import tools.xor.ProcessingStage;
import tools.xor.operation.GraphTraversal;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;


public class ListUpdateAction extends CollectionUpdateAction {
	private static final Logger logger = LogManager.getLogger(new Exception()
			.getStackTrace()[0].getClassName());
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
		for(AddElementAction action: addByOppositeActions) {
			// If the element does have position information, then it cannot be part of a list
			// as the ordering is not known
			if(action.getPosition() != null) {
				listOrderedActions.put(Integer.parseInt(action.getPosition().toString()), action);
			}
		}

		for(AddElementAction action: listOrderedActions.values()) {
			// Add the element to the List
			addToList(action);		
			collectionModified = true;			
		}
		
		// TODO: checkOutOfSync()
	
	}

	private List extractList(BusinessObject bo) {
		List list = new ArrayList();

		if(bo != null) {
			if (bo.getInstance() instanceof List) {
				list = (List)bo.getInstance();
			}
			else if (bo.getInstance() instanceof JSONArray) {
				list = ClassUtil.jsonArrayToList((JSONArray)bo.getInstance());
			}
		}

		return list;
	}

	@Override
	public void processLinks (Map outputMap,
							  BusinessObject input,
							  CallInfo callInfo) throws
		Exception
	{
		List list = extractList(input);

		for(int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			processLink((new Integer(i)).toString(), obj, callInfo, outputMap);
		}
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

		List inputList = extractList(input);
		Object outputList = output.getInstance();
		int outputListSize = getSize(outputList);

		if(outputListSize > inputList.size()) { // truncate the remaining portion
			truncateToSize(outputList, inputList.size());
		}
	}

	private static int getSize(Object outputList) {
		if(outputList instanceof List) {
			return ((List)outputList).size();
		} else if(outputList instanceof JSONArray) {
			return ((JSONArray)outputList).length();
		}

		return -1;
	}

	private static void addElement(Object outputList, Object element) {
		if(outputList instanceof List) {
			((List)outputList).add(element);
		} else if(outputList instanceof JSONArray) {
			((JSONArray)outputList).put(element);
		}
	}

	private static void setElement(Object outputList, Object element, int position) {
		if(outputList instanceof List) {
			((List)outputList).set(position, element);
		} else if(outputList instanceof JSONArray) {
			((JSONArray)outputList).put(position, element);
		}
	}

	private static void truncateToSize(Object outputObj, int toSize) {
		if(outputObj instanceof List ) {
			List outputList = (List) outputObj;
			for (int i = outputList.size(); i > toSize; i--) {
				outputList.remove(i - 1);
			}
		} else if(outputObj instanceof JSONArray) {
			JSONArray outputArray = (JSONArray) outputObj;
			for (int i = outputArray.length(); i > toSize; i--) {
				outputArray.remove(i - 1);
			}
		}
	}

	public void linkElement(CallInfo next, String dynamicProperty, boolean isNew) throws Exception {
		GraphTraversal oper = (GraphTraversal) next.getOperation();

		/*
		BusinessObject targetElement = (BusinessObject) next.getOutput();
		if(next.getSettings().getAction() != AggregateAction.CREATE && targetElement != null) // updating an existing object
			next.setOutput(targetElement);
		else
			next.setOutput(oper.createTarget(next, null));
*/
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
			} else {
				collection = ClassUtil.getInstance(collection);
			}
		} catch(Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		int listSize = getSize(collection);
		int position = Integer.parseInt(action.getPosition().toString());
		if(listSize == position ) {
			addElement(collection, ClassUtil.getInstance(collectionElement));
		} else if (listSize > position)
		{
			// replace the item at the postion
			setElement(collection, ClassUtil.getInstance(collectionElement), position);
		} else {
			throw new IllegalArgumentException(
				"Trying to add a list element at the wrong position: " + action.getPosition() + ", list size: "
					+ listSize
					+ ". Check if the ORM is using a different API for populating this property - "
					+ collectionProperty.getContainingType().getName() + "#" + collectionProperty.getName()
			);
		}

		if(collectionProperty.getPositionProperty() != null) {
			collectionElement.set(collectionProperty.getPositionProperty(), position);
		}
	}		
}
