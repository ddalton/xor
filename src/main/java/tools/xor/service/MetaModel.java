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
import java.util.Map;

import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.view.QueryView;

public class MetaModel {

	protected DASFactory dasFactory;	
	
	public MetaModel(AggregateManager am) {
		this.dasFactory = am.getDasFactory();
	}

	protected DataAccessService getDAS() {
		DataAccessService das = dasFactory.create();

		return das;
	}

	public List<String> getAggregateList() {
		List<String> result = getDAS().getAggregateList();
		Collections.sort(result);
		
		return result;
	}

	public List<String> getViewList() {
		List<String> result = getDAS().getViewNames();
		Collections.sort(result);
		
		return result;		
	}
	
	public List<String> getFilterFunctionList() {
		// TODO
		return null;
	}

	public Type getType(String aggregateName) {
		//TODO
		return null;
	}

	public QueryView getView(String viewName) {
		// TODO
		return null;
	}

	public List<String> getAttributePaths(String aggregateName) {
		Type type = getDAS().getType(aggregateName);
		List<String> paths = new ArrayList<String>();
		paths.addAll(AggregatePropertyPaths.enumerate(type));
		
		paths = sortPaths(paths);
		
		return paths;
	}
	
	/**
	 * Sorts the attributes by their part paths and hence by their length in an ordered form
	 * Path gets modified in this method so it cannot be final
	 * @param path
	 * @return
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
