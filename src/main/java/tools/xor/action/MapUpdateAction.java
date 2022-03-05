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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.BusinessObject;
import tools.xor.CallInfo;
import tools.xor.ExtendedProperty;
import tools.xor.ProcessingStage;
import tools.xor.operation.GraphTraversal;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;


public class MapUpdateAction extends CollectionUpdateAction {
	private static final Logger owLogger = LogManager.getLogger(Constants.Log.OBJECT_WALKER);

	private PropertyKey key;
	private List<AddElementAction>        addActions = new ArrayList<AddElementAction>();
	private List<RemoveElementAction>     removeActions = new ArrayList<RemoveElementAction>();

	public MapUpdateAction(PropertyKey key) {
		this.key = key;
	}

	@Override
	public void execute() {


		// TODO: Invoke RemoveElementAction, AddElementAction and RepositionElementAction
		// Any incomplete actions need to be resolved or an error raised
		// An action is incomplete 
		// 1. if the position is null or
		// 2. position is not null (duplicates will not be created) and the collection is a list or a map
		// 3. setModified flag on map data object

		for(AddElementAction action: addActions) {
			// Add the element to the Map
			addToMap(action);		
		}

	}

	@Override
	public void addAction(Executable action) {

		if(action instanceof AddElementAction)
			addActions.add((AddElementAction) action);
		else if(action instanceof RemoveElementAction)
			removeActions.add((RemoveElementAction) action);			
		else
			throw new IllegalArgumentException("A Map can have only add, remove actions");
	}

	@Override
	public void processLinks (Map outputMap,
							  BusinessObject input,
							  CallInfo callInfo) throws
		Exception
	{
		Map map = (Map) input.getInstance();

		for(Object item: map.entrySet()) {
			Map.Entry entry = (Map.Entry) item;
			processLink(entry.getKey().toString(), entry.getValue(), callInfo, outputMap);
		}
	}

	@Override
	public PropertyKey getKey() {
		return key;
	}

	public void linkElement(CallInfo next, String dynamicProperty, boolean isNew) throws Exception {
		GraphTraversal oper = (GraphTraversal) next.getOperation();
		/*
		BusinessObject targetElement = (BusinessObject) next.getOutput();
		if(next.getSettings().getAction() != AggregateAction.CREATE && targetElement != null)// updating an existing object
			next.setOutput(targetElement);
		else
			next.setOutput(oper.createTarget(next, null));
			*/
		
		if(owLogger.isDebugEnabled()) {
			if(isNew && next.getStage() == ProcessingStage.CREATE) {
				owLogger.debug(next.getIndentString() + "[Creating a new map element]");
			}
		}
		
		oper.processAttribute(next);
		
		// Do not modify the data graph unless we are in the right stage
		if(next.getStage() != ProcessingStage.UPDATE) {
			return;
		}

		// AddElementAction
		addMapElement(next, (BusinessObject) next.getOutput(), dynamicProperty);			

	}	

	protected void addMapElement(CallInfo callInfo, BusinessObject collectionElement, String position) {
		BusinessObject collectionOwner = (BusinessObject) callInfo.getParent().getParent().getOutput();

		PropertyKey propertyKey = new PropertyKey((BusinessObject) callInfo.getParent().getOutput(), callInfo.getParent().getOutputProperty());
		AddElementAction originalAction = new AddElementAction(propertyKey, collectionElement, collectionOwner, null, position, callInfo.getInputObjectCreator());
		this.addAction(originalAction);

		// Set the backRef of value to point to the parent target (or to the ancester in case of embedded property)
		// TODO: handle MANY_TO_MANY backRef			
		if(originalAction != null && callInfo.getParent().getOutputProperty().isBiDirectional()) {
			PropertyKey newOppositeKey = new PropertyKey(collectionElement, callInfo.getParent().getOutputProperty().getOpposite());			
			callInfo.getOutputRoot().getObjectPersister().addAction(new SetterAction(callInfo.getSettings(), collectionOwner, newOppositeKey, originalAction));
		}
	}	
	
	protected void addToMap(AddElementAction action) {

		BusinessObject collectionOwner  = action.getCollectionOwner();
		Object collection                   = ((ExtendedProperty) action.getKey().getProperty()).getValue(collectionOwner);
		Object collectionElement            = action.getCollectionElement();
		ExtendedProperty collectionProperty = (ExtendedProperty) action.getKey().getProperty();

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

		((java.util.Map)collection).put(action.getPosition().toString(), ClassUtil.getInstance(collectionElement));
	}			
}
