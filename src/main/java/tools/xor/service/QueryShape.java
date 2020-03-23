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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.QueryType;
import tools.xor.Type;
import tools.xor.view.AggregateView;
import tools.xor.view.AggregateViewFactory;
import tools.xor.view.AggregateViews;
import tools.xor.view.View;

/**
 * Represents the shape of the type system.
 * 
 * QueryShape model entity types that are user defined,i.e., these are dynamic types
 * They help defining the type of the object that is returned from a query. 
 * Different queries can return data in different shapes
 * Also, the same SQL/OQL query can return data in different shapes depending on the view specification of 
 * the query types.
 * 
 * The supported built-in types are (case insensitive):
 *   Object
 *   List
 *   Date
 *   String
 *   Boolean
 *   Integer
 *   BigDecimal
 *   BigInteger
 *   Float
 *   Double
 *   
 * Helps with Dynamic Query Object Reconstitution
 */
public class QueryShape extends Shape
{
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	private Set<AggregateView> registrations = new HashSet<>();
	
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
    public void register(AggregateView view, String rootEntityName) {
    		registrations.add(view);
    }
    

    /**
     * Creates a QueryType with properties created from the rootType renamed based on aliases map
     * and with the type defined by typeMappings.
     * QueryType objects are temporary and hence are not part of the Shape system.
     *
     * @param rootType on which this QueryType is based.
     * @param aliases for the properties of rootType
     * @param typeMappings subclass types of the properties of the rootType. The key is alias name.
     * @return QueryType entity type
     */
    public EntityType addQueryType(EntityType rootType, Map<String, String> aliases, Map<String, String> typeMappings) {

        Map<String, Property> propertyMap = new HashMap<>(); // for existence check

        // check if the aliases have any missing type mappings
        // if so, they are added with the predefined type mapping
        for(Map.Entry<String, String> entry: aliases.entrySet()) {
            if(!typeMappings.containsKey(entry.getKey())) {
                String propertyName = aliases.get(entry.getKey());
                ExtendedProperty property = (ExtendedProperty)getProperty(rootType, propertyName);
                String typeName = property.isMany() ? property.getElementType().getName() : property.getType().getName();

                typeMappings.put(entry.getKey(), typeName);
            }
        }

        // We first create the properties and then create the QueryType instance from it.
        for(Map.Entry<String, String> entry: typeMappings.entrySet()) {
            // Get the domain property for this entry
            String propertyName = aliases.get(entry.getKey());
            // TODO: an alias may not be of rootType
            Property property = getProperty(rootType, propertyName);

            Type newPropertyType = getType(entry.getValue());
            Property queryProperty = ((ExtendedProperty) property).refine(entry.getKey(), newPropertyType, rootType);
            propertyMap.put(queryProperty.getName(), queryProperty);
        }

        // create QueryType for all the embedded types

        EntityType result = new QueryType(rootType, propertyMap);
        result.setShape(this);

        return result;
    }    
    
    private void extractQueryTypes(String fileName) {
    		List<AggregateView> views = new ArrayList<>();
    		
    		if(fileName != null) {
    			AggregateViews queryViews = AggregateViewFactory.load(fileName);
    			views = new ArrayList<>(queryViews.getAggregateView());
    		}
    		
    		for(AggregateView view: registrations) {
    			views.add(view);
    		}

    		extractQueryTypes(views);
    }
    
    /**
     * Construct QueryType and the corresponding property types from the view
     * and add them to the Shape
     * 
     * @param views whose QueryType instances need to be extracted
     * @return
     */
    private void extractQueryTypes(List<AggregateView> views) {
        // scan through the aliases, both the selected and the addendum (for object linking)
        // Get the objects with names and create QueryType object with those names and associate it against the anchor path
        // root QueryType has empty anchor path
        // for QueryType(s) that do not have a name, generate a name and do the same as above
        // Now populate the propeties for the QueryType(s) by processing the last element of the path
        // For this algorithm to work, all QueryType(s) need to marked with "Object" type, if not an error is thrown    	
    }
    
    /**
     * After all the views have been registered, then the QueryType instances corresponding to those views
     * will be generated.
     * 
     * @param fileName of the views that are read from a file. If not provided user should register the views using
     *   register method.
     * @see QueryShape#register(View, String)
     */
    public void process(String fileName) {
        extractQueryTypes(fileName);
    }
}