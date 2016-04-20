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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.service.DataAccessService;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;

import javax.swing.text.html.parser.Entity;

/**
 * Include inheritance in the State Graph navigation. This causes the DFA to become a NFA. 
 * @author Dilip Dalton
 *
 */
public class DFAtoNFA {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	public static void processInheritance(StateGraph<State, Edge<State>> stateGraph) {
		Map<EntityType, State> entityTypeMap = new HashMap<EntityType, State>();
		// maintain a stack of inheritance states to process
		for(State state: stateGraph.getVertices()) {
			if(state.getType() instanceof EntityType) {
				entityTypeMap.put((EntityType)state.getType(), state);
			}
		}

		for(Map.Entry<EntityType, State> entry:  entityTypeMap.entrySet()) {

			State superTypeState = entityTypeMap.get(entry.getKey());
			for(EntityType subType: entry.getKey().getSubtypes()) {

				State subTypeState = entityTypeMap.get(subType);
				// extend the state set to include this new state
				if(subTypeState == null) {
					subTypeState = new State(subType, false);
				}

				// Add an empty edge from the supertype to the subtype
				// This makes it a NFA, since only NFA allows empty edges
				stateGraph.addEdge(
					new Edge("", superTypeState, subTypeState),
					superTypeState,
					subTypeState);
			}
		}
	}
}
