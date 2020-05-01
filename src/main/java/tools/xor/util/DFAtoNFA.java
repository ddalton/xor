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

package tools.xor.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.util.graph.StateGraph;

/**
 * Include inheritance in the State Graph navigation. This causes the DFA to become a NFA. 
 * @author Dilip Dalton
 *
 */
public class DFAtoNFA {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public static final String UNLABELLED = "";

	public enum TypeCategory {
		SUBTYPES,
		SUPERTYPES,
		ALL
	}

	/**
	 * Add subtypes and supertypes to the stategraph.
	 * This method does not add any edges between these subtype and supertype states and
	 * the existing graph.
	 *
	 * @param stateGraph to be extended with subtypes and supertypes
	 * @param typeCategory selectively add subtypes or supertypes or both
	 */
	public static void processInheritance(StateGraph<State, Edge<State>> stateGraph, TypeCategory typeCategory) {
		Map<EntityType, State> entityTypeMap = new HashMap<EntityType, State>();
		// maintain a stack of inheritance states to process
		for(State state: stateGraph.getVertices()) {
			if(state.getType() instanceof EntityType) {
				entityTypeMap.put((EntityType)state.getType(), state);
			}
		}

		Collection<EntityType> entities = new HashSet(entityTypeMap.keySet());
		if(typeCategory == TypeCategory.SUBTYPES || typeCategory == TypeCategory.ALL) {
			// Add subtypes
			for (EntityType entityType : entities) {
				addSubTypes(entityType, stateGraph, entityTypeMap);
			}
		}

		if(typeCategory == TypeCategory.SUPERTYPES || typeCategory == TypeCategory.ALL) {
			// Add supertypes
			for (EntityType entityType : entities) {
				addSuperTypes(entityType, stateGraph, entityTypeMap);
			}
		}
	}

	private static void addSubTypes (EntityType entityType,
									 StateGraph<State, Edge<State>> stateGraph,
									 Map<EntityType, State> entityTypeMap) {

		State entityState = entityTypeMap.get(entityType);
		for(EntityType subType: entityType.getChildTypes()) {

			State subTypeState = entityTypeMap.get(subType);
			// extend the state set to include this new state
			if(subTypeState == null) {
				subTypeState = new State(subType, false);
			}

			// Add an empty edge from the supertype to the subtype
			// This makes it a NFA, since only NFA allows empty edges
			stateGraph.addEdge(
				new Edge(UNLABELLED, entityState, subTypeState),
				entityState,
				subTypeState);
			entityTypeMap.put(subType, subTypeState);

			addSubTypes(subType, stateGraph, entityTypeMap);
		}
	}

	private static void addSuperTypes(EntityType entityType, StateGraph<State, Edge<State>> stateGraph, Map<EntityType, State> entityTypeMap) {

		State entityState = entityTypeMap.get(entityType);
		EntityType superType = entityType.getParentType();

		if(superType != null) {

			State superTypeState = entityTypeMap.get(superType);

			// extend the state set to include this new state
			if(superTypeState == null) {
				superTypeState = new State(superType, false);
			}

			// Add an empty edge from the supertype to the subtype
			// This makes it a NFA, since only NFA allows empty edges
			stateGraph.addEdge(
				new Edge(UNLABELLED, superTypeState, entityState),
				superTypeState,
				entityState);
			entityTypeMap.put(superType, superTypeState);

			addSuperTypes(superType, stateGraph, entityTypeMap);
		}
	}
}
