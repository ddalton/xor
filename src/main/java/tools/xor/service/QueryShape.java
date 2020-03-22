/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2017, Dilip Dalton
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.view.AggregateViewFactory;
import tools.xor.view.AggregateViews;
import tools.xor.view.View;

/**
 * Represents the shape of the type system.
 */
public class QueryShape extends Shape
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private Map<View, String> registrations = new HashMap<>();
	
    public QueryShape(String name, Shape parent, DataAccessService das) {
		super(name, parent, das);
	}
    
    /**
     * Extend the shape with the entities referred by the view.
     * 
     * @param view extending the shape
     * @param rootEntityName optional root entity. If not provided all the properties of the view need
     *     to be defined using aliases.
     */
    public void register(View view, String rootEntityName) {
    		registrations.put(view, rootEntityName);
    }
    
    private Set<String> collectEntities() {
    		AggregateViews queryViews = AggregateViewFactory.load("QueryViews.xml");

    		// TODO: complete this method
    		return null;
    }
    
    /**
     * After all the views have been registered, then the QueryType instances corresponding to those views
     * will be generated.
     */
    public void process() {
    		// Collect all the entities referred from the registered views
    		Set<String> entityNames = collectEntities();
    	
    		getDAS().processShape(this, null, entityNames);
    }
}