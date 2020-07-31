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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ExtendedProperty;
import tools.xor.ProcessingStage;
import tools.xor.exception.BidirOutOfSyncException;
import tools.xor.operation.GraphTraversal;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.util.ObjectCreator;


public class SetUpdateAction extends CollectionUpdateAction {
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);

	private PropertyKey key;
	private List<AddElementAction>               addActions = new ObjectArrayList<AddElementAction>();
	private List<RemoveElementAction>            removeActions = new ObjectArrayList<RemoveElementAction>(); 
	private Map<BusinessObject, List<ElementAction>> actionsOnElementMap = new Reference2ObjectOpenHashMap<BusinessObject, List<ElementAction>>();

	public SetUpdateAction(PropertyKey key) {
		this.key = key;
	}

	private void mapActionsOnElement(BusinessObject collectionElement, ElementAction action) {
		List<ElementAction> actions = actionsOnElementMap.get(collectionElement);
		if(actions == null) {
			actions = new LinkedList<ElementAction>();
			actionsOnElementMap.put(collectionElement, actions);
		}
		actions.add(action);
	}

	@Override
	public void execute() {
		boolean collectionModified = false;

		// Create a map between the collectionElement and the actions acted upon it

		Set<RemoveElementAction> rActions = new HashSet<RemoveElementAction>(removeActions);
		rActions.addAll(removeByOppositeActions);

		// Remove obsolete elements
		for(RemoveElementAction action: rActions) {
			// Remove the element from the set
			removeFromSet(action);
			mapActionsOnElement(action.getCollectionElement(), action);
			collectionModified = true;
		}

		// Add new elements
		Set<AddElementAction> actions = new HashSet<AddElementAction>(addActions);
		actions.addAll(addByOppositeActions);

		for(AddElementAction action: actions) {
			// Add the element to the set
			addToSet(action);
			mapActionsOnElement(action.getCollectionElement(), action);			
			collectionModified = true;			
		}

		checkOutOfSync();

	}

	private void checkOutOfSync() {
		// It is an error to only have a triggered by action doing a removal while the collectionOwner is present in the input and is processed
		for(Map.Entry<BusinessObject, List<ElementAction>> entry: actionsOnElementMap.entrySet()) {
			ElementAction lastAction = null;
			boolean isUnlinkedOnOneSide = false;
			for(ElementAction action: entry.getValue()) {

				if(action instanceof RemoveElementAction)
					isUnlinkedOnOneSide = true;
				lastAction = action;
				if(action.getTriggeringAction() == null) {
					lastAction = null; // The same action is also present in the owner, so the change is in sync
					break;
				}
			}

			if(lastAction != null && isUnlinkedOnOneSide) { // potential out-of-sync condition
				BusinessObject collectionOwner = lastAction.getCollectionOwner();
				
				// is out-of-sync
				ObjectCreator ioc = lastAction.getInputObjectCreator();
				BusinessObject sourceCollectionOwner = ioc.getRoot().getEntity(collectionOwner, null);
				if(sourceCollectionOwner != null && sourceCollectionOwner.isVisited()) // The owner is present in the input and was processed
					throw new BidirOutOfSyncException(collectionOwner, lastAction.getKey().getProperty(), lastAction.getCollectionElement(), lastAction.getKey().getProperty().getOpposite());
			}
		}		
	}

	protected void addToSet (AddElementAction action)
	{

		BusinessObject collectionOwner = action.getCollectionOwner();
		Object collection = ((ExtendedProperty)action.getKey().getProperty()).getValue(
			collectionOwner);
		Object collectionElement = action.getCollectionElement();
		ExtendedProperty collectionProperty = (ExtendedProperty)action.getKey().getProperty();

		// It is possible for the collection object to be null, if this is due to the MANY_TO_ONE end only being set
		try {
			if (collection == null) {
				collection = createCollection(collectionOwner, collectionProperty);
			} else {
				collection = ClassUtil.getInstance(collection);
			}
		}
		catch (Exception e) {
			throw ClassUtil.wrapRun(e);
		}

		((java.util.Collection<Object>)collection).add(ClassUtil.getInstance(collectionElement));
	}	

	protected void removeFromSet(RemoveElementAction action) {	

		BusinessObject collectionOwner  = action.getCollectionOwner();
		Object collection                   = ((ExtendedProperty) action.getKey().getProperty()).getValue(collectionOwner);
		Object collectionElement            = action.getCollectionElement();
		ExtendedProperty collectionProperty = (ExtendedProperty) action.getKey().getProperty();

		((java.util.Collection<Object>)collection).remove(ClassUtil.getInstance(collectionElement));		
	}		

	@Override
	public void addAction(Executable action) {

		if(action instanceof AddElementAction)
			addActions.add((AddElementAction) action);
		else if(action instanceof RemoveElementAction)
			removeActions.add((RemoveElementAction) action);		
		else
			throw new IllegalArgumentException("A set can have only add and remove actions");
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
				owLogger.debug(next.getIndentString() + "[Creating a new set element]");
			}
		}
				
		oper.processAttribute(next);
		
		// Do not modify the data graph unless we are in the right stage
		if(next.getStage() != ProcessingStage.UPDATE) {
			return;
		}

		// AddElementAction
		if(isNew)
			addSetElement(next, (BusinessObject) next.getOutput());

	}	

	protected void addSetElement(CallInfo callInfo, BusinessObject collectionElement) {
		BusinessObject collectionDataObject = (BusinessObject) callInfo.getParent().getOutput();
		BusinessObject collectionOwner = (BusinessObject) callInfo.getParent().getParent().getOutput();

		PropertyKey propertyKey = new PropertyKey(collectionDataObject, callInfo.getParent().getOutputProperty());
		Executable originalAction = new AddElementAction(propertyKey, collectionElement, collectionOwner, null, null, callInfo.getInputObjectCreator());
		this.addAction(originalAction);		

		// Set the backRef of value to point to the parent target (or to the ancester in case of embedded property)
		// TODO: handle MANY_TO_MANY backRef
		if(callInfo.getParent().getOutputProperty().isBiDirectional()) {
			PropertyKey newOppositeKey = new PropertyKey(collectionElement, callInfo.getParent().getOutputProperty().getOpposite());			
			callInfo.getOutputRoot().getObjectPersister().addAction(new SetterAction(callInfo.getSettings(), collectionOwner, newOppositeKey, originalAction));
		}
	}		

	@Override
	public PropertyKey getKey() {
		return key;
	}

}
