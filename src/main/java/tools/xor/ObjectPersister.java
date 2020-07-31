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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import tools.xor.action.CollectionUpdateAction;
import tools.xor.action.ElementAction;
import tools.xor.action.Executable;
import tools.xor.action.MigratorActionFactory;
import tools.xor.action.PropertyKey;
import tools.xor.action.SetterAction;
import tools.xor.util.ClassUtil;

public class ObjectPersister {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	// Includes add, remove and reposition collection elements
	private Map<PropertyKey, List<Executable>> propertyActions = new Object2ObjectOpenHashMap<PropertyKey, List<Executable>>();
	private Map<PropertyKey, List<Executable>> deferredActions = new Object2ObjectOpenHashMap<PropertyKey, List<Executable>>();
	
	public void processActions(Settings settings) {
		Map<PropertyKey, List<Executable>> immediateActions = new Object2ObjectOpenHashMap<PropertyKey, List<Executable>>();
		for(Map.Entry<PropertyKey, List<Executable>> entry: propertyActions.entrySet()) {
			if(entry.getKey().getProperty().isOpenContent()) {
				deferredActions.put(entry.getKey(), entry.getValue());
			} else {
				immediateActions.put(entry.getKey(), entry.getValue());
			}
		}
		
		process(settings, immediateActions);
	}
	
	public void processOpenPropertyActions(Settings settings) {
		process(settings, deferredActions);
	}

	public void process(Settings settings, Map<PropertyKey, List<Executable>> currentActions) {

//		if(settings.getSessionContext() instanceof CustomPersister) {
//			((CustomPersister) settings.getSessionContext()).addActions(currentActions);
//			return;
//		}

		// Process the uni-directional actions
		Set<PropertyKey> uniDirKeys = new HashSet<PropertyKey>();
		for(PropertyKey key: currentActions.keySet()) {
			if( !((ExtendedProperty)key.getProperty()).isBiDirectional() )
				uniDirKeys.add(key);
		}
		for(PropertyKey uniDirKey: uniDirKeys) {
			List<Executable> actions = currentActions.remove(uniDirKey);
			processActions(uniDirKey, actions);
		}

		// Currently used by tests for immediate actions
		if(currentActions != deferredActions) {
			settings.getInterceptor().preBiDirActionStage( Collections.unmodifiableMap(currentActions) );
		}

		for(Map.Entry<PropertyKey, List<Executable>> entry: currentActions.entrySet())
			processActions(entry.getKey(), currentActions.get(entry.getKey()));
	}

	private void processActions(PropertyKey key, List<Executable> actions) {
		if(key.getProperty().isMany()) {
			for(Executable action: actions) {
				action.execute();
			}
		} else
			processToOne(actions);
	}

	public void processToOne(List<Executable> actions) {
		// We follow the system where we execute the action that has a non-null value if one is present or execute the null setting action
		// This policy is present to automatically fix broken links
		// The other policy we can have (maybe through configuration) is to throw an exception if a conflict is detected.
		// A conflict can occur if two actions set different values. It is still desirable to throw an exception if two different non-null values are being set.

		SetterAction actionToExecute = null;
		for(Executable action: actions) {
			SetterAction setterAction = (SetterAction) action;
			if(actionToExecute == null) {
				actionToExecute = setterAction;
			}
			if( ClassUtil.getInstance(setterAction.getValue()) != null) {
				if(ClassUtil.getInstance(actionToExecute.getValue()) != null && ClassUtil.getInstance(actionToExecute.getValue()) != ClassUtil.getInstance(setterAction.getValue()) ) {
					throw new IllegalArgumentException(
						"Two different objects cannot share a reference to the same object in a ToOne bi-directional relationship. PropertyKey details: "
							+ actionToExecute.toString() + ", previous PropertyKey details: " + setterAction.toString());
				} else {
					actionToExecute = setterAction;
				}
			}
		}
		actionToExecute.execute();		
	}

	/**
	 * If this is a element action, the migrator action is found and the element action is recorded on it
	 * @param action update action object
	 */
	public void addAction(Executable action) {

		List<Executable> actions = getActions(action.getKey());

		if (ElementAction.class.isAssignableFrom(action.getClass())) {
			CollectionUpdateAction collectionAction = getOrCreateMigratorAction(action.getKey());
			collectionAction.addTriggeredByOppositeAction((ElementAction) action);
		} else {
			actions.add(action);
		}
	}

	private List<Executable> getActions(PropertyKey key) {
		List<Executable> actions = propertyActions.get(key);
		if(actions == null) {
			actions = new ArrayList<Executable>();
			propertyActions.put((PropertyKey) key, actions);
		}		

		return actions;
	}

	public CollectionUpdateAction getOrCreateMigratorAction(PropertyKey key) {
		// actions should not be null
		List<Executable> actions = getActions(key);

		CollectionUpdateAction migrator = null;		
		if(actions.size() == 0) {
			migrator = MigratorActionFactory.getInstance(key);
			addAction(migrator);
			return migrator;
		}

		if(actions.size() == 1) {
			migrator = (CollectionUpdateAction) actions.get(0);
		} else
			throw new IllegalStateException("Collection property should have only one migrator action");

		return migrator;
	}

}
