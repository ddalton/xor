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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import tools.xor.AbstractType;
import tools.xor.BasicType;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.JSONObjectProperty.Converter;
import tools.xor.MutableJsonType;
import tools.xor.Property;
import tools.xor.PropertyProxy;
import tools.xor.Settings;
import tools.xor.SimpleTypeFactory;
import tools.xor.Type;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.ApplicationConfiguration;
import tools.xor.util.Constants;
import tools.xor.util.DFAtoNFA;
import tools.xor.util.DFAtoRE;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.State;
import tools.xor.util.Vertex;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.DirectedSparseGraph;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.UnmodifiableView;
import tools.xor.view.View;

public abstract class AbstractShape implements Shape
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    // Move the following to Shape abstraction
    protected Map<String, Type> types = new ConcurrentHashMap<String, Type>();
    protected DataModel das; // It is possible this is null, especially if it is shared across DAS instances. A child shape can be created with the DAS populated for each DAS.
    protected String name;
    protected Shape parent;
    protected Map<String, View> views = new ConcurrentHashMap<String, View>();
    protected StateGraph<State, Edge<State>> orderedGraph;
    
    // Once a shape is created, its shape and type inheritance cannot be changed
    final protected Inheritance shapeInheritance;
    final protected Inheritance typeInheritance;
    private   Map<Class, Converter> convertersByClass = new ConcurrentHashMap<Class, Converter>();
    
    // Contains the entityType's declared properties for Inheritance.REFERENCE typeInheritance
    // and both declared and overridden properties for Inheritance.VALUE typeInheritance
    protected Map<String, Map<String, Property>> properties = new ConcurrentHashMap<>();

    // Used to signal if the shape has finished being being
    private volatile boolean buildFinished;

    public AbstractShape(String name, Shape parent, DataModel das) {
        this(name, parent, das, Inheritance.REFERENCE);
    }   
    
    public AbstractShape(String name, Shape parent, DataModel das, Inheritance typeInheritance) {
        this.name = name;
        this.das = das;
        this.parent = parent;
        this.typeInheritance = typeInheritance;
        
        // Types are shared
        this.shapeInheritance = Inheritance.REFERENCE;
    }    
    
    @Override
    public boolean isBuildFinished ()
    {
        return buildFinished;
    }

    @Override
    public void setBuildFinished (boolean value)
    {
        this.buildFinished = value;
    }
    
    @Override
    public DataModel getDataModel() {
        return das;
    }

    @Override
    public Inheritance getShapeInheritance() {
        return this.shapeInheritance;
    }
    
    @Override
    public Inheritance getTypeInheritance() {
        return this.typeInheritance;
    }    

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Shape getParent() {
        return this.parent;
    }
    
    @Override
    public void setParent(Shape shape) {
        this.parent = shape;
    }

    @Override
    public void addType(String className, Type type) {
        addType(className, type, types);
    }

    private void addType(String className, Type type, Map<String, Type> typeMap) {
        typeMap.put(className, type);
        logger.info("Adding type for entity: " + className);

        if(EntityType.class.isAssignableFrom(type.getClass())) {
            ((EntityType)type).setShape(this);
            String entityName = ((EntityType)type).getEntityName();
            if(entityName != null && !className.equals(entityName)) {
                if(typeMap.containsKey(entityName)) {
                    if(!className.equals(typeMap.get(entityName).getInstanceClass().getName()))
                        throw new RuntimeException("Type " + typeMap.get(entityName).getName() + " already exists for entityName: " + entityName);
                } else
                    typeMap.put(entityName, type);
            }
        }
    }

    @Override
    public boolean hasType(EntityType entityType) {
        if(entityType.isDomainType() && types.containsKey(entityType.getEntityName())) {
            return true;
        }

        return false;
    }

    @Override
    public Type getType(String name) {
        Type result = null;

        if(this.shapeInheritance == Inheritance.VALUE) {
            result = getTypeCaseInsensitive(name);
        } else if(this.shapeInheritance == Inheritance.REFERENCE) {
            result = getTypeCaseInsensitive(name);
            if(result == null && parent != null) {
                return parent.getType(name);
            }
        }

        return result;
    }

    private Type getTypeCaseInsensitive(String name) {
        if(types.containsKey(name)) {
            return types.get(name);
        } else if(types.containsKey(name.toUpperCase())) {
            return types.get(name.toUpperCase());
        }

        return null;
    }

    @Override
    public Type getType(Class<?> clazz) {

        Type result = getType(clazz.getName());

        // create a Type object for this class
        if(result == null) {
            //result = new SimpleType(clazz);
            result = SimpleTypeFactory.getType(clazz, this);

            addType(clazz.getName(), result);
        }

        return result;
    }

    @Override
    public void addType(Type type) {
        addType(type.getName(), type);   
        
        if(type instanceof EntityType) {
            addType(((EntityType)type).getEntityName(), type);
        }
    }    

    @Override
    public Map<String, Property> getProperties(EntityType type) {
        // Important this is null, as this is used to check for existence of properties
        Map<String, Property> result = null;

        if (this.shapeInheritance == Inheritance.REFERENCE && parent != null) {
            Map<String, Property> temp = parent.getProperties(type);
            if(temp != null) {
                result = new LinkedHashMap<>(parent.getProperties(type));
            }
        }
        
        EntityType current = type;
        Stack<EntityType> ancestors = new Stack<>();
        while (current != null) {
            ancestors.add(current);
            current = current.getParentType();
        }

        // NOTE: for this algorithm to work correctly, the containing type should
        // be the last type popped from the stack
        while (!ancestors.isEmpty()) {
            current = ancestors.pop();
            Map<String, Property> directProperties = this.properties.get(current.getEntityName());
            if (directProperties != null) {
                if (result == null) {
                    // We are modifying the result so make a copy
                    result = new LinkedHashMap<>();
                }
                
                // The containing type properties is the last to get added
                // as it overrides the parent type properties
                // So we create a proxy to model copy-on-write semantics for
                // properties defined in the ancestor types.
                // So if an ancestor type property is modified, then a copy
                // is made and added to the containing type
                if(this.typeInheritance == Shape.Inheritance.VALUE && type == current) {
                    Map<String, Property> temp = new LinkedHashMap<>();
                    for(Map.Entry<String, Property> entry: result.entrySet()) {
                        Property proxy = (new PropertyProxy()).bind((ExtendedProperty) entry.getValue(), type);
                        temp.put(entry.getKey(), proxy);
                    }
                    result = temp;
                }
                result.putAll(directProperties);
            }
        }

        return result == null ? null : Collections.unmodifiableMap(result);
    }

    @Override
    public Property getProperty(EntityType type, String name) {
        Property result = null;

        EntityType current = type;
        do {
            if (this.properties.get(current.getEntityName()) != null && this.properties.get(current.getEntityName()).containsKey(name)) {
                result = this.properties.get(current.getEntityName()).get(name);
            }
            current = current.getParentType();
        } while (result == null && current != null);
        
        // For VALUE semantics create a proxy for copy-on-write
        String containingTypeName = result != null ? ((EntityType)result.getContainingType()).getEntityName() : null;
        if(result != null && this.typeInheritance == Shape.Inheritance.VALUE && !type.getEntityName().equals(containingTypeName)) {
            result = (new PropertyProxy()).bind((ExtendedProperty) result, type);
        }

        if(result == null && this.shapeInheritance == Inheritance.REFERENCE && parent != null) {
            result = parent.getProperty(type, name);
        }

        return result;
    }

    @Override
    public Property getDeclaredProperty(EntityType type, String name) {
        Property result = null;

        if (getDeclaredProperties(type) != null && getDeclaredProperties(type).containsKey(name)) {
            result = getDeclaredProperties(type).get(name);
        }

        if(result == null && this.shapeInheritance == Inheritance.REFERENCE && parent != null) {
            result = parent.getDeclaredProperty(type, name);
        }

        return result;
    }

    @Override
    public Map<String, Property> getDeclaredProperties(EntityType type) {
        Map<String, Property> result = null;

        if(this.shapeInheritance == Inheritance.REFERENCE && parent != null) {
            result = parent.getDeclaredProperties(type);
        }        
        
        // Override with current shape declared properties
        Map<String, Property> entityProperties = this.properties.get(type.getEntityName());
        if (entityProperties != null) {
            if(result == null) {
                result = new LinkedHashMap<>();
            }
            for (Map.Entry<String, Property> entry : entityProperties.entrySet()) {
                if (entry.getValue().getContainingType().getName().equals(type.getName())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }       
        
        return result;
    }

    @Override
    public void addProperty (Property property) {
        EntityType type = (EntityType)property.getContainingType();
        addProperty(type, property);
    }

    @Override
    public void addProperty (EntityType type, Property property)
    {
//            System.out.println(String.format("Adding property %s to type %s", property.getName(), type.getEntityName()));
        Map<String, Property> directProps = this.properties.get(type.getEntityName());
        if (directProps == null) {
            directProps = new LinkedHashMap<>();
            this.properties.put(type.getEntityName(), directProps);
        }
        
        directProps.put(property.getName(), property);
    }

    @Override
    public void removeProperty (Property openProperty) {
        EntityType type = (EntityType)openProperty.getContainingType();
        removeProperty(type, openProperty);
    }

    @Override
    public void removeProperty (EntityType type, Property openProperty) {
        if (this.properties.containsKey(type.getEntityName())) {
            this.properties.get(type.getEntityName()).remove(openProperty.getName());
        }        
    }

    @Override
    public Set<Type> getUniqueTypes() {
        return new HashSet<Type>(types.values());
    }

    public void initRootType(Collection<Type> types) {
        for (Type type : types) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).initRootEntityType();
            }
        }
    }

    public void initEnd(Collection<Type> types) {
        for (Type type : types) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).unfoldProperties(this);
            }
        }

        for (Type type : types) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).initEnd(this);
            }
        }
    }

    public void initPositionProperty(Collection<Type> types) {
        for (Type type : types) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).initPositionProperty(this);
            }
        }
    }

    @Override
    public List<String> getViewNames() {
        List<String> result = new ArrayList<String>();

        for(View view: views.values()) {
            result.add(view.getName());
        }

        return result;
    }

    @Override
    public View getView(String viewName) {
        View existing = views.get(viewName);
        return existing == null ? null : new UnmodifiableView(views.get(viewName));
    }

    @Override
    public List<View> getViews() {
        return new ArrayList<View>(views.values());
    }

    @Override
    public View getView(EntityType type) {

        String viewName = AbstractType.getViewName(type);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(viewName);

            DFAtoRE dfaRE = new DFAtoRE(type, this);
            result.addTypeGraph(type, dfaRE.getExactStateGraph(), StateGraph.Scope.TYPE_GRAPH);
            result.addTypeGraph(type, dfaRE.getFullStateGraph(), StateGraph.Scope.FULL_GRAPH);

            updateView(result, viewName, new HashSet<>());
        }

        return getView(viewName);
    }

    @Override
    public void addView(AggregateView view) {
        if(views.containsKey(view.getName())) {
            throw new RuntimeException("There is an existing view with this name: " + view.getName());
        }
        if(!view.isExpanded()) {
            view.expand();
        }

        views.put(view.getName(), view);
    }

    @Override
    public View getBaseView(EntityType type) {

        String viewName = AbstractType.getBaseViewName(type);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(viewName);
            Set<String> paths = AggregatePropertyPaths.enumerateBase(type);

            updateView(result, viewName, paths);
        }

        return getView(viewName);
    }

    @Override
    public View getMigrateView(EntityType type) {

        String viewName = AbstractType.getMigrateViewName(type);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(viewName);
            Set<String> paths = AggregatePropertyPaths.enumerateMigrate(type);

            updateView(result, viewName, paths);
        }

        return getView(viewName);
    }

    @Override
    public View getRelationshipView(EntityType type, Property property) {

        String viewName = AbstractType.getRelationshipViewName(type, property);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(viewName);
            Set<String> paths = AggregatePropertyPaths.enumerateRelationship(type, property);

            updateView(result, viewName, paths);
        }

        return getView(viewName);
    }

    @Override
    public View getRefView(EntityType type) {

        String viewName = AbstractType.getRefViewName(type);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(viewName);
            Set<String> paths = AggregatePropertyPaths.enumerateRef(type);

            updateView(result, viewName, paths);
        }

        return getView(viewName);
    }

    private void updateView(View view, String viewName, Set<String> paths) {
        view.setAttributeList(new ArrayList<>(paths));
        view.setShape(this);

        // built-in views are always expanded
        ((AggregateView)view).setExpanded(true);

        views.put(viewName, view);
    }

    /*
     * Expand view references
     */
    private void denormalize() {
        checkViewCycles();

        for(View view: views.values()) {
            if(view.hasViewReference()) {
                view.expand();
            }
        }
    }

    @Override
    public void sync(AggregateManager am, Map<String, List<AggregateView>> avVersions) {

        for(List<AggregateView> versions: avVersions.values()) {
            AggregateView selected = versions.get(0);
            if(am.getViewVersion() < selected.getVersion()) {
                throw new IllegalStateException("Unable to find a view for the selected version: " + am.getViewVersion());
            }
            for(int i = versions.size()-1; i >= 0; i--) {
                if(versions.get(i).getVersion() <= am.getViewVersion()) {
                    selected = versions.get(i);
                    break;
                }
            }
            selected.setShape(this);

            selected = selected.copy();
            views.put(selected.getName(), selected);
        }
        denormalize();
    }

    private void checkViewCycles() {
        DirectedGraph<AggregateView, Edge> dg = new DirectedSparseGraph<AggregateView, Edge>();

        // Add the views as state objects
        for(View view: views.values()) {
            Set<String> viewReferences = view.getViewReferences();
            for(String edge: viewReferences) {
                AggregateView start = (AggregateView)view;
                AggregateView end = (AggregateView)views.get(edge);

                // self loop
                if(start == end) {
                    throw new IllegalStateException("Self-loop cycle found in view references: " + edge);
                }
                dg.addEdge(new Edge<>(edge, start, end), start, end);
            }
        }

        List<List<Vertex>> cycles = dg.getCircuits();
        if(cycles.size() > 0) {
            throw new IllegalStateException("Cycle found in view references: " + GraphUtil.printCycles(
                cycles));
        }
    }

    @Override
    public void createOrderedGraph() {
        // State graph of all the entity types in topological order
        // Entity state graph is most likely a forest, so there is no root state
        this.orderedGraph = new StateGraph<>(null, this);
        for(Type type: getUniqueTypes()) {
            if(EntityType.class.isAssignableFrom(type.getClass())) {
                orderedGraph.addVertex(new State(type, false));
            }
        }

        orderedGraph.populateEdges(this);
        DFAtoNFA.processInheritance(orderedGraph, DFAtoNFA.TypeCategory.ALL);

        if (!ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_SKIP)
            || !ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_SKIP)) {
            try {
                orderedGraph.toposort(this);
            }
            catch (RuntimeException re) {
                throw re;
            }

            orderedGraph.orderTypes();

            // Print out the graph if so configured
            if (ApplicationConfiguration.config().containsKey(Constants.Config.TOPO_VISUAL)
                && ApplicationConfiguration.config().getBoolean(Constants.Config.TOPO_VISUAL)) {
                Settings settings = new Settings();
                settings.setGraphFileName("ApplicationStateGraph" + this.getName() + ".dot");
                orderedGraph.generateVisual(settings);
            }
        }
    }

    @Override
    public StateGraph<State, Edge<State>> getOrderedGraph() {
        return this.orderedGraph;
    }
    
    @Override
    public void registerConverter(Class<?> clazz, Converter converter) {
        if(!convertersByClass.containsKey(clazz)) {
            convertersByClass.put(clazz, converter);
        }
    }    
    
    @Override
    public Converter getConverter(ExtendedProperty property) {
        if(convertersByClass.containsKey(property.getType().getInstanceClass())) {
            return convertersByClass.get(property.getType().getInstanceClass());
        }       
        return null;
    }
    
    private String getSchemaURI() {
        // This can be externalized
        StringBuilder sb = new StringBuilder("http://localhost/");
        sb.append(getName());
        
        return sb.toString();
    }
    
    private void processParentTypes(JSONObject definitions, Type parent) {
        if(parent == null || !(parent instanceof EntityType)) {
            return;
        }
        if(parent.getParentTypes() == null) {
            return;
        }
        
        EntityType type = (EntityType) parent;
        if(!definitions.has(type.getEntityName())) {
            definitions.put(type.getEntityName(), processType(type));
        }
        
        for(Type parentType: parent.getParentTypes()) {
            processParentTypes(definitions, parentType);
        }
    }
    
    private JSONObject processType(EntityType type) {
        JSONObject typeJson = new JSONObject();

        boolean hasParent = type.getParentType() != null;
        JSONObject currentJson = hasParent ? new JSONObject() : typeJson;
        if(hasParent) {
            JSONArray contents = new JSONArray();
            contents.put(currentJson);
            typeJson.put(MutableJsonType.SCHEMA_ALLOF, contents);
            
            for(Type parentType: type.getParentTypes()) {
                JSONObject parentJson = new JSONObject();
                contents.put(parentJson);
                String entityName = parentType instanceof EntityType ? ((EntityType)parentType).getEntityName() : parentType.getName();
                parentJson.put(MutableJsonType.SCHEMA_REF, getJsonId(entityName) );
            }
        }
        
        currentJson.put("$id", getJsonId(((EntityType)type).getEntityName()));
        currentJson.put(MutableJsonType.SCHEMA_TYPE, "object");
        
        // populate properties  
        Map<String, Property> propertyMap = getDeclaredProperties(type);
        if(propertyMap != null && propertyMap.size() > 0) {
            JSONObject propertiesJson = new JSONObject();
            currentJson.put(MutableJsonType.SCHEMA_PROPERTIES, propertiesJson);
           
            Set<String> required = new HashSet<>();
            for(Map.Entry<String, Property> entry: getDeclaredProperties(type).entrySet()) {
                
                Property property = entry.getValue();
                if(!property.isNullable()) {
                    required.add(property.getName());
                }
                
                JSONObject propertyJson = new JSONObject();
                propertiesJson.put(entry.getKey(), propertyJson);
                
                String propertyType = ((BasicType)property.getType()).getJsonType();
                propertyJson.put(MutableJsonType.SCHEMA_TYPE, propertyType);
                
                if(propertyType.equals(MutableJsonType.JSONSCHEMA_ARRAY_TYPE)) {
                    JSONObject itemsJson = new JSONObject();
                    propertyJson.put(MutableJsonType.SCHEMA_ITEMS, itemsJson);
                    
                    Type elementType = ((ExtendedProperty)property).getElementType();
                    if(elementType.isDataType()) {
                        itemsJson.put(MutableJsonType.SCHEMA_TYPE, ((BasicType)elementType).getJsonType());
                    } else {
                        itemsJson.put(MutableJsonType.SCHEMA_REF, getJsonId(((EntityType)elementType).getEntityName()));
                    }
                }
            }
            
            if(!required.isEmpty()) {
                JSONArray requiredJson = new JSONArray();
                for(String propertyName: required) {
                    requiredJson.put(propertyName);
                }
                
                currentJson.put(MutableJsonType.SCHEMA_REQUIRED, requiredJson);
            }
        }
        
        return typeJson;
    }
    
    private String getJsonId(String entityName) {
        return "#" + entityName;
    }    
    
    @Override
    public JSONObject getJsonSchema() {
        
        JSONObject json = new JSONObject();
        json.put("$schema", getSchemaURI());
        JSONObject definitions = new JSONObject();
        json.put("definitions", definitions);
        
        for(Type type: getUniqueTypes()) {
            if(type.isDataType()) {
                continue;
            }
            definitions.put(((EntityType)type).getEntityName(), processType((EntityType) type));
            processParentTypes(definitions, type);
        }
        
        return json;
    }
}
