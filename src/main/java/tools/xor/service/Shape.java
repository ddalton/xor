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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JSONObjectProperty.Converter;
import tools.xor.Property;
import tools.xor.Type;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.View;

/**
 * Represents the shape of the type system.
 * 
 * Currently the following 3 implementations are supported
 * DomainShape -  Represents the type system as published by the ORM
 *                or the tables in a RDBMS (jdbcShape)
 * DynamicShape - Represents the type system declaratively specified. For e.g., 
 *                1. swagger.json
 *                2. views and an instance of DomainShape
 *                3. TypeMapper and an instance of DomainShape
 * 
 * DynamicShape is composed of ExternalType and ExternalProperty instances.
 * JDBCShape is composed of JDBCType and JDBCProperty instances.
 * DomainShape is composed of provider specific types. For e.g., JPAType etc 
 * 
 * A DomainShape can optionally be associated with a JDBCShape to get any table
 * specific information
 * 
 */
public interface Shape
{
    /**
     * Describes how the types in a parent shape are inherited by a child shape 
     * Also controls how properties within a parent and child type within a shape are shared
     * If the inheritance strategy is REFERENCE then the types are shared by the child shape
     * else if the inheritance strategy is VALUE then the types are copied to the child shape
     *
     */
    public enum Inheritance {
        REFERENCE,
        VALUE
    };

    /**
     * Check to see if the Shape has finished building all the types it is made of
     * 
     * @return true if the shape has finished construction false otherwise
     */
    public boolean isBuildFinished ();

    /**
     * Used to signal if the Shape has finished being constructed
     * @param value true or false 
     */
    public void setBuildFinished (boolean value);

    /**
     * Return the DataModel associated with this shape. 
     * It is possible that the DataModel is null. That means that the shape is applicable to more than 1 DataModel.
     * A child shape associated with the DataModel needs to be created, with this being the parent shape.
     * This will help to utilize memory efficiently while sharing shapes across DataModel instances.
     * 
     * @return DataModel associated with this shape
     */
    public DataModel getDataModel();

    /**
     * Returns the shape inheritance strategy of the Shape object being created. Whether the shape has a copy of the types from
     * the parent shape or if the shape shares the types with the parent shape.
     * @return shape inheritance value
     */
    public Inheritance getShapeInheritance();
    
    /**
     * Returns the type inheritance strategy of the Shape object being created. Whether a type has a copy of the properties from
     * the parent type or if the type shares the properties with the parent type.
     * @return type inheritance value
     */
    public Inheritance getTypeInheritance();    

    /**
     * Get the name of the shape
     * @return shape name
     */
    public String getName();

    /**
     * Get the parent shape 
     * @return parent shape or null if no parent
     */
    public Shape getParent();
    
    /**
     * Set the parent shape of this shape.
     * If there are many shapes with common types or properties, then we can avoid duplication 
     * by capturing the common information in a parent shape
     * @param shape representing the parent shape
     */
    public void setParent(Shape shape);

    /**
     * Add a type to the shape system. Will just add it without checking parent type.
     * It is the client's responsibility to decide whether to add it to the shape.
     *
     * @param className by which the type is referenced by
     * @param type to be added
     */
    public void addType(String className, Type type);

    /**
     * Returns true if this Shape is the owner of the type whether domain or external
     * @param entityType entityType
     * @return true if this Shape created this type
     */
    public boolean hasType(EntityType entityType);

    /**
     * A simple mechanism to signal an event that the type structure has changed.
     * Currently supported only for JDBC to signal that a temporary table has been added.
     */
    public void signalEvent ();

    /**
     * Depending on the type sharing strategy, the type might either be completely managed by
     * this Shape instance or could be shared with a parent Shape instance.
     *
     * @param name of the type
     * @return type instance
     */
    public Type getType(String name);

    /**
     * Get the type by its Java class name.
     * @param clazz java class name
     * @return Type instance
     */
    public Type getType(Class<?> clazz);

    /**
     * Add the type to the Shape
     * @param type to be added
     */
    public void addType(Type type);

    /**
     * Get a full map of all the properties by the property name.
     * If shape inheritance is by reference, then we also include properties from parent shape for this type
     * If shape inheritance is by value, then we look at properties only in the current shape
     * 
     * If type inheritance is by reference, then we include direct properties from parent types
     * if type inheritance is by value, then we retrieve all properties for the type (including parent types)
     *
     * @param type entity type
     * @return a map of all the properties.
     */
    public Map<String, Property> getProperties(EntityType type);

    /**
     * Method to optimally retrieve a single property. It also looks at the super types.
     *
     * @param type entity type
     * @param name of the property
     * @return property meta object
     */
    public Property getProperty(EntityType type, String name);

    /**
     * Check if a property is declared only on that type. This method does not check
     * the super types to find the property. @See Shape#getProperty
     * But it checks the parent shape if the property is not found in the current shape.
     * 
     * @param type having the property we are looking for
     * @param name of the property we need
     * @return property instance
     */
    public Property getDeclaredProperty(EntityType type, String name);

    /**
     * Get all the properties for the given type defined in the current shape instance.
     * We do not look for additional properties from the parent shape.
     * 
     * @param type whose properties we need
     * @return the properties for the type in the current shape
     */
    public Map<String, Property> getDeclaredProperties(EntityType type);

    /**
     * Add a property to the shape. The type will be inferred from the property instance.
     * @param property to be added
     */
    public void addProperty (Property property);

    /**
     * Add a direct property.
     * Irrespective of the ShapeStrategy, a property can be overridden by a child Shape.
     * i.e., a type does not have to be defined in this Shape in order to define a property.
     *
     * @param type to which this property belongs
     * @param property that is to be added
     */
    public void addProperty (EntityType type, Property property);

    /**
     * Remove the property from the shape
     * @param openProperty property to remove
     */
    public void removeProperty (Property openProperty);

    /**
     * Remove the property from the shape belonging to a certain type
     * @param type containing the property
     * @param openProperty property to remove
     */
    public void removeProperty (EntityType type, Property openProperty);

    /**
     * Return a list of all the types managed by this shape instance
     * We do not include the parent shape. 
     * @return set of types
     */
    public Set<Type> getUniqueTypes();

    /**
     * Return a list of names of all the views in the Shape
     * @return list of view names
     */
    public List<String> getViewNames();

    /**
     * Return a view given its name.
     * The view is a read only view. If any customization needs to be done to the view, 
     * then a copy of the view needs to be made to make it modifiable.
     * 
     * @param viewName view name
     * @return view instance
     */
    public View getView(String viewName);

    /**
     * Return a list of all the view instances.
     * The view objects are read only. If any customization needs to be done to the view, 
     * then a copy of the view needs to be made to make it modifiable.
     * 
     * @return list of view objects
     */
    public List<View> getViews();

    /**
     * Returns the default view for the EntityType. This means that the
     * StateGraph that is generated is also pre-defined and comes in 2 flavors:
     * 1. StateGraph with subtypes populated
     * 2. StateGraph with no subtypes
     *
     * NOTE: We do not set the paths for the built-in views, since they
     * can be enumerated from the TypeGraph
     * 
     * @param type representing the EntityType corresponding to the view
     * @return built-in view
     */
    public View getView(EntityType type);

    /**
     * Extend the shape by adding a new view instance
     * 
     * @param view to be added
     */
    public void addView(AggregateView view);

    /**
     * Get a built-in view having the Base scope of a given entity type 
     * @param type entity type
     * @return view instance
     */
    public View getBaseView(EntityType type);

    /**
     * Get a built-in view having the Migrate scope of a given entity type 
     * @param type entity type
     * @return view instance
     */
    public View getMigrateView(EntityType type);

    /**
     * Create a view that only scopes out the relationship between the given entity type
     * and the property representing the relationship.
     * This view is dedicated for a toMany relationship and can handle all types such as:
     * 1. Simple types
     * 2. Embedded types
     * 3. Entity types
     *
     * @param type whose relationship we need to scope out
     * @param property representing the toMany relationship
     * @return view of the relationship
     */
    public View getRelationshipView(EntityType type, Property property);

    /**
     * Get a built-in view having the Reference scope of a given entity type 
     * @param type entity type
     * @return view instance
     */    
    public View getRefView(EntityType type);

    /**
     * Update the views in the shape with the given list of views
     * 
     * @param am AggregateManager instance used for checking the valid versions
     * @param avVersions versioned (newer) view objects
     */
    public void sync(AggregateManager am, Map<String, List<AggregateView>> avVersions);

    /**
     * State graph of all the entity types in topological order.
     * This is useful to find the dependency information between the different types of the
     * Shape given the relationship information.
     * The parent shape is not taken into consideration.
     * This can be an expensive operation depending on the size of the Shape instance.
     */
    public void createOrderedGraph();

    /**
     * If the shape was configured to created a topological ordering
     * then this graph is accessed using this method
     *
     * @return topological ordering of the shape
     */
    public StateGraph<State, Edge<State>> getOrderedGraph();
    
    /**
     * Register a converter instance for a given type
     * @param clazz for which the converter will interpret the data
     * @param converter object
     */
    public void registerConverter(Class<?> clazz, Converter converter);
    
    /**
     * Return the converter for a property. It will find the appropriate converter based on the
     * property type.
     * 
     * @param property instance
     * @return converter instance
     */
    public Converter getConverter(ExtendedProperty property);
    
    /**
     * Export the shape contents in the form of a Json Schema
     * @return json schema object
     */
    public JSONObject getJsonSchema();
}
