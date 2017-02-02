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

import tools.xor.Settings;

public final class Edge<V extends Vertex> {

	private final String name;
	private final V start;
	private final V end;
	private final boolean qualified;

	public String getName() {
		return name;
	}
	
	public boolean isQualified() {
		return qualified;
	}

	public V getStart() {
		return start;
	}

	public V getEnd() {
		return end;
	}

	public Edge(String name, V start, V end) {
		this(name, start, end, false);
	}

	public Edge(String name, V start, V end, boolean qualify) {
		this.name = name;
		this.start = start;
		this.end = end;
		this.qualified = qualify;
	}

	public String getQualifiedName() {
		if(qualified) {
			return name + Settings.PATH_DELIMITER;
		} else {
			return name;
		}
	}
	
	@Override
	public String toString() {
		return start.getName() + "--" + getName() + "-->" + end.getName();
	}
}
