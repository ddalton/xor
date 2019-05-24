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

import java.util.List;

import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.SimpleType;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.graph.StateGraph;


public class GraphUtil {

	public static Type getPropertyType(Property property, Shape shape) {
		Type result = property.getType();

		// Coerce to the correct shape
		if(shape != null) {
			result = shape.getType(result.getName());
		}

		return result;
	}

	public static Type getPropertyEntityType(Property property, Shape shape) {
		Type result = property.getType();

		if(result instanceof SimpleType) {
			if(property.isMany())
				result = ((ExtendedProperty)property).getElementType();
		}

		// Coerce to the correct shape
		if(shape != null) {
			result = shape.getType(result.getName());
		}

		return result;
	}

	public static String printCycles(List<List<Vertex>> cycles) {
		StringBuilder result = new StringBuilder();
		
		for(List<Vertex> cycle: cycles) {
			StringBuilder sb = new StringBuilder();
			for(Vertex s: cycle) {
				sb.append(sb.length() > 0 ? "->" : "");
				sb.append(s.getName());
			}
			result.append("Cycle: ").append(sb.toString()).append("\r\n");
		}
		result.append("\r\n----E N D-----");
		
		return result.toString();
	}

	public static String printGraph(StateGraph<State, Edge<State>> sg) {
		StringBuilder result = new StringBuilder();
		
		for(State s: sg.getVertices()) {
			result.append("Out edges for state: " + s.getName() + "\r\n");
			for(Edge<State> t: sg.getOutEdges(s)) {
				result.append(Constants.Format.getIndentString(1) + t.getName() + ", start: " + sg.getStart(t) + ", end: " + sg.getEnd(t) + "\r\n");
			}
		}
		return result.toString();
	}
}
