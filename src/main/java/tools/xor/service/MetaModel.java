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

package tools.xor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tools.xor.EntityType;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.view.View;

public class MetaModel {

    protected AggregateManager am;

	public MetaModel(AggregateManager am) {
	    this.am = am;
	}

	protected DataModel getDAS() {
		DataModel das = am.getDataModelFactory().create(am.getTypeMapper());

		return das;
	}

	public List<String> getViewList() {
		List<String> result = getDAS().getShape().getViewNames();
		Collections.sort(result);
		
		return result;		
	}

	public List<String> getTypeList() {
		List<Type> types = new ArrayList<>(getDAS().getShape().getUniqueTypes());
		List<String> result = new ArrayList<String>(types.size());

		for(Type type: types) {
			result.add(type.getName());
		}
		Collections.sort(result);

		return result;
	}

	public List<String> getEntityNames() {

		Shape shape = getDAS().getShape();
		ArrayList<Type> types = new ArrayList<>(shape.getUniqueTypes());

		if(shape.getShapeInheritance() == Shape.Inheritance.REFERENCE && shape.getParent() != null) {
			types.addAll(shape.getParent().getUniqueTypes());
		}

		List<String> result = new ArrayList<String>(types.size());
		for(Type type: types) {
			if(type instanceof EntityType) {
				result.add(type.getName());
			}
		}
		Collections.sort(result);

		return result;
	}

	public List<String> getEntityProperties(String entityName) {
		Type type = getDAS().getShape().getType(entityName);

		if( type == null || !(type instanceof EntityType)) {
			throw new RuntimeException("The provided name is not an entity: " + entityName);
		}

		List<String> result = new ArrayList<>();
		for(Property property: type.getProperties()) {
			result.add(property.getName());
		}

		Collections.sort(result);

		return result;
	}

	public List<String> getExpandedNaturalKey(String entityName) {
		Type type = getDAS().getShape().getType(entityName);

		if( type == null || !(type instanceof EntityType)) {
			throw new RuntimeException("The provided name is not an entity: " + entityName);
		}

		return ((EntityType)type).getExpandedNaturalKey();
	}

	public List<String> getViewAttributes(String viewName) {
		View view = getDAS().getShape().getView(viewName);
		return view.getAttributeList();
	}

	public List<String> getAggregateAttributes(String aggregateName) {
		Type type = getDAS().getShape().getType(aggregateName);
		List<String> paths = new ArrayList<String>();
		paths.addAll(AggregatePropertyPaths.enumerateRegEx(type, getDAS().getShape()));
		
		paths = sortPaths(paths);
		
		return paths;
	}
	
	/**
	 * Sorts the attributes by their part paths and hence by their length in an ordered form
	 * Path gets modified in this method so it cannot be final
	 * @param path attribute path
	 * @return sorted paths
	 */
	private List<String> sortPaths(List<String> path) {
		int totalSize = path.size();
		List<String> result = new ArrayList<String>();
		
		int i = 1; // number of path parts 
		while(result.size() != totalSize) {
			List<String> samePartCountPaths = new ArrayList<String>();
			List<String> remaining = new ArrayList<String>();
			for(String p: path) {
				if(countPathParts(p) == i-1)
					samePartCountPaths.add(p);
				else
					remaining.add(p);
			}
			Collections.sort(samePartCountPaths);
			result.addAll(samePartCountPaths);
			path = remaining;
			i++;
		}
		
		return result;
	}
	
	private static int countPathParts(String path)
	{
	    int count = 0;
	    for (int i=0; i < path.length(); i++)
	        if (path.charAt(i) == Settings.PATH_DELIMITER.charAt(0))
	             count++;

	    return count;
	}	
	
}
