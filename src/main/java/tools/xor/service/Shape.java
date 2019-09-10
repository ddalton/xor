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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.AbstractType;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.ExternalType;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.QueryType;
import tools.xor.Settings;
import tools.xor.SimpleType;
import tools.xor.SimpleTypeFactory;
import tools.xor.Type;
import tools.xor.TypeNarrower;
import tools.xor.exception.MultipleClassForPropertyException;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the shape of the type system.
 */
public class Shape
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    // Move the following to Shape abstraction
    protected Map<String, Type> types = new ConcurrentHashMap<String, Type>();
    protected Map<String, Type> externalTypes = new ConcurrentHashMap<String, Type>();
    protected DataAccessService das;
    protected String name;
    protected Shape parent;
    protected Map<String, View> views = new ConcurrentHashMap<String, View>();
    protected Map<String, Map<String, Property>> domainProperties = new ConcurrentHashMap<>();
    protected Map<String, Map<String, Property>> externalProperties = new ConcurrentHashMap<>();
    protected StateGraph<State, Edge<State>> orderedGraph;

    // This functionality helps to get the correct narrowed class (subtype) based on the properties
    // defined by the view
    // It is map between the given class and the view and the narrowed class
    protected Map<Class<?>, Map<String, Class<?>>> narrowedClassByView = new ConcurrentHashMap<Class<?>, Map<String,Class<?>>>();
    private volatile boolean needsUpdate;

    protected ShapeStrategy shapeStrategy = ShapeStrategy.SHARED;

    // Used to signal if the shape has finished being being
    private volatile boolean buildFinished;

    public boolean isBuildFinished ()
    {
        return buildFinished;
    }

    public void setBuildFinished (boolean value)
    {
        this.buildFinished = value;
    }

    public enum ShapeStrategy {
        SHARED,
        COPY
    };

    public Shape(String name, Shape parent, DataAccessService das) {
        this.name = name;
        this.das = das;
        this.parent = parent;
    }

    public DataAccessService getDAS() {
        return das;
    }

    public ShapeStrategy getShapeStrategy() {
        return this.shapeStrategy;
    }

    public String getName() {
        return this.name;
    }

    public Shape getParent() {
        return this.parent;
    }

    /**
     * Add a type to the shape system. Will just add it without checking parent type.
     * It is the client's responsibility to decide whether to add it to the shape.
     *
     * @param className by which the type is referenced by
     * @param type to be added
     */
    public void addType(String className, Type type) {
        addType(className, type, types);
    }

    protected void addExternalType (String className, Type type) {
        addType(className, type, externalTypes);
    }

    private void addType(String className, Type type, Map<String, Type> typeMap) {
        typeMap.put(className, type);

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
    public EntityType createQueryType(EntityType rootType, Map<String, String> aliases, Map<String, String> typeMappings) {

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

    /**
     * Returns true if this Shape is the owner of the type whether domain or external
     * @param entityType entityType
     * @return true if this Shape created this type
     */
    public boolean hasType(EntityType entityType) {
        if(entityType.isDomainType() && types.containsKey(entityType.getName())) {
            return true;
        }

        if(!entityType.isDomainType() && externalTypes.containsKey(entityType.getName())) {
            return true;
        }

        return false;
    }

    /**
     * Depending on the type sharing strategy, the type might either be completely managed by
     * this Shape instance or could be shared with a parent Shape instance.
     *
     * @param name of the type
     * @return type instance
     */
    public Type getType(String name) {
        Type result = null;

        if(this.shapeStrategy == ShapeStrategy.COPY) {
            result = getTypeCaseInsensitive(name);
        } else if(this.shapeStrategy == ShapeStrategy.SHARED) {
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

    /**
     * We do not look to the parent shape, since a Shape should be self sufficient
     * in all the types it is responsible for.
     *
     * @param name of the type
     * @return external type instance
     */
    public Type getExternalType(String name) {
        Type result = null;

        if(this.shapeStrategy == ShapeStrategy.COPY) {
            result = getExternalTypeCaseInsensitive(name);
        } else if(this.shapeStrategy == ShapeStrategy.SHARED) {
            result = getExternalTypeCaseInsensitive(name);
            if(result == null && parent != null) {
                return parent.getExternalType(name);
            }
        }

        return result;
    }

    private Type getExternalTypeCaseInsensitive(String name) {
        if(externalTypes.containsKey(name)) {
            return externalTypes.get(name);
        } else if(externalTypes.containsKey(name.toUpperCase())) {
            return externalTypes.get(name.toUpperCase());
        }

        return null;
    }

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

    public Type getExternalType(Class<?> clazz) {
        Type result = getExternalType(clazz.getName());

        // create a Type object for this class
        if (result == null) {
            //result = new SimpleType(clazz);
            result = SimpleTypeFactory.getType(clazz, this);
            addExternalType(clazz.getName(), result);
        }

        return result;
    }

    public void addOpenType(OpenType type) {
        if(types.containsKey(type.getName())) {
            throw new RuntimeException("A type with the same name exists, please choose a different name for the open type: " + type.getName());
        }

        type.setShape(this);
        type.setProperty();
        addType(type.getName(), type);

        Class<?> externalClass = getDAS().getTypeMapper().toExternal(type.getInstanceClass());
        if(externalClass != null) {
            ExternalType externalType = getDAS().getTypeMapper().createExternalType(
                type,
                externalClass);
            addExternalType(externalType.getName(), externalType);
            externalType.setProperty(this);
            setBiDirectionOnExternalType(externalType);
        }
    }

    /**
     * We derive an external type from the QueryType.
     * QueryType are not part of the shape. Similarly we do not add external type to the shape.
     *
     * @param type QueryType for which we need to derive the external type
     * @return external type
     */
    public ExternalType deriveExternalType(QueryType type) {
        ExternalType externalType = null;
        Class<?> externalClass = getDAS().getTypeMapper().toExternal(type.getInstanceClass());
        if(externalClass != null) {
            externalType = getDAS().getTypeMapper().createExternalType(
                type,
                externalClass);
            externalType.setProperty(this, false);
        }

        return externalType;
    }

    protected void setBiDirectionOnExternalType (ExternalType externalType) {
        externalType.setOpposite(this);
    }

    protected void setSuperTypeOnExternalType (ExternalType externalType) {
        if(externalType.getDomainType().getSuperType() == null) {
            return;
        }

        Type type = getExternalType(externalType.getDomainType().getSuperType().getName());
        if(type instanceof EntityType) {
            externalType.setSuperType((EntityType)type);
        }
    }

    /**
     * Get a full map of all the properties by the property name.
     *
     * @param type entity type
     * @return a map of all the properties.
     */
    public Map<String, Property> getProperties(EntityType type) {
        Map<String, Property> result = null;

        if(this.shapeStrategy == ShapeStrategy.SHARED && parent != null) {
            result = parent.getProperties(type);
        }

        Map<String, Property> directProperties = getDirectProperties(type);
        if(directProperties != null) {
            if(result != null) {
                // We are modifying the result so make a copy
                result = new LinkedHashMap<>(result);
                result.putAll(directProperties);
            } else {
                result = directProperties;
            }
        }

        return result == null ? null : Collections.unmodifiableMap(result);
    }

    /**
     * Method to optimally retrieve a single property. It also looks at the super types.
     *
     * @param type entity type
     * @param name of the property
     * @return property meta object
     */
    public Property getProperty(EntityType type, String name) {
        Property result = null;

        EntityType current = type;
        do {
            if (getDirectProperties(current) != null && getDirectProperties(current).containsKey(name)) {
                result = getDirectProperties(current).get(name);
            }
            current = current.getSuperType();
        } while(result == null && current != null);

        if(result == null && this.shapeStrategy == ShapeStrategy.SHARED && parent != null) {
            result = parent.getProperty(type, name);
        }

        return result;
    }

    /**
     * Check if a property is declared only on that type. This method does not check
     * the super types to find the property. @See Shape#getProperty
     * @param type
     * @param name
     * @return
     */
    public Property getDeclaredProperty(EntityType type, String name) {
        Property result = null;

        if (getDirectProperties(type) != null && getDirectProperties(type).containsKey(name)) {
            result = getDirectProperties(type).get(name);
        }

        if(result == null && this.shapeStrategy == ShapeStrategy.SHARED && parent != null) {
            result = parent.getProperty(type, name);
        }

        return result;
    }

    public Map<String, Property> getDirectProperties(EntityType type) {
        if(type.isDomainType()) {
            return domainProperties.get(type.getName());
        } else {
            return externalProperties.get(type.getName());
        }
    }

    public void addDirectProperty(EntityType type, Property property) {
        addProperty(type, property);
    }

    public void addProperty (Property property) {
        EntityType type = (EntityType)property.getContainingType();
        addProperty(type, property);
    }

    /**
     * Irrespective of the ShapeStrategy, a property can be overridden by a child Shape.
     * i.e., a type does not have to be defined in this Shape in order to define a property.
     *
     * @param type to which this property belongs
     * @param property that is to be added
     */
    public void addProperty (EntityType type, Property property)
    {
        if(type.isDomainType()) {
            Map<String, Property> properties = domainProperties.get(type.getName());
            if (properties == null) {
                properties = new LinkedHashMap<>();
                domainProperties.put(type.getName(), properties);
            }
            properties.put(property.getName(), property);
        } else {
            Map<String, Property> properties = externalProperties.get(type.getName());
            if (properties == null) {
                properties = new LinkedHashMap<>();
                externalProperties.put(type.getName(), properties);
            }
            properties.put(property.getName(), property);
        }
    }

    public void removeProperty (Property openProperty) {
        EntityType type = (EntityType)openProperty.getContainingType();
        removeProperty(type, openProperty);
    }

    public void removeProperty (EntityType type, Property openProperty) {
        if(type.isDomainType()) {
            if (domainProperties.containsKey(type.getName())) {
                domainProperties.get(type.getName()).remove(openProperty.getName());
            }
        } else {
            if (externalProperties.containsKey(type.getName())) {
                externalProperties.get(type.getName()).remove(openProperty.getName());
            }
        }
    }

    public void addOpenProperty(Property property) {
        EntityType type = (EntityType)property.getContainingType();

        Property domainProperty = type.isDomainType() ? property : ((ExtendedProperty)property).getDomainProperty();
        Property externalProperty = !type.isDomainType() ? property : null;
        if(externalProperty == null && type.isDomainType()) {
            ExternalType externalType = (ExternalType)getExternalType(type.getName());
            if(externalType != null) {
                externalProperty = externalType.defineProperty(domainProperty, this);
            }
        }

        addProperty(domainProperty);
        addProperty(externalProperty);
    }

    public void removeOpenProperty(Property property) {
        EntityType type = (EntityType)property.getContainingType();

        Property domainProperty = type.isDomainType() ? property : ((ExtendedProperty)property).getDomainProperty();
        Property externalProperty = !type.isDomainType() ? property : null;

        removeProperty(domainProperty);
        if(externalProperty != null) {
            removeProperty(externalProperty);
        }
    }

    protected void deriveExternal () {
        for(Type type: getUniqueTypes()) {
            if(SimpleType.class.isAssignableFrom(type.getClass()) || type.isOpen()) {
                continue;
            }
            Class<?> externalClass = getDAS().getTypeMapper().toExternal(type.getInstanceClass());
            if(externalClass != null) {
                Type externalType = getDAS().getTypeMapper().createExternalType(
                    (EntityType)type,
                    externalClass);
                addExternalType(externalType.getName(), externalType);
            }
        }

        // init the properties
        for (Type type : getUniqueExternalTypes()) {
            if (ExternalType.class.isAssignableFrom(type.getClass())) {
                ExternalType externalType = (ExternalType) type;

                if(isOpenDomainType(externalType)) {
                    continue;
                }
                externalType.setProperty(this);
            }
        }

        for (Type type : getUniqueExternalTypes()) {
            if(!ExternalType.class.isAssignableFrom(type.getClass())) {
                continue;
            }

            ExternalType externalType = (ExternalType) type;
            setSuperTypeOnExternalType(externalType);
            if (!type.isOpen()) {
                if(isOpenDomainType(externalType)) {
                    continue;
                }
                setBiDirectionOnExternalType(externalType);
            }
        }
    }


    public Set<Type> getUniqueTypes() {
        return new HashSet<Type>(types.values());
    }

    private Set<Type> getUniqueExternalTypes () {
        return new HashSet<Type>(externalTypes.values());
    }

    public void initRootType() {
        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).initRootEntityType();
            }
        }
        for (Type type : getUniqueExternalTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).initRootEntityType();
            }
        }
    }

    public void initEnd() {
        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).unfoldProperties(this);
            }
        }
        for (Type type : getUniqueExternalTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).unfoldProperties(this);
            }
        }

        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).initEnd(this);
            }
        }
    }

    public void initPositionProperty() {
        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).initPositionProperty(this);
            }
        }
    }

    private boolean isOpenDomainType(ExternalType externalType) {
        // If the domain type is open, then we cannot
        // infer the properties. For e.g., an open domain type
        // does not have a Java class and is populated dynamically
        return externalType.getDomainType().isOpen();
    }

    public List<String> getViewNames() {
        List<String> result = new ArrayList<String>();

        for(View view: views.values()) {
            result.add(view.getName());
        }

        return result;
    }

    public View getView(String viewName) {
        View existing = views.get(viewName);
        return existing == null ? null : new UnmodifiableView(views.get(viewName));
    }

    public List<View> getViews() {
        return new ArrayList<View>(views.values());
    }

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

    public void addView(AggregateView view) {
        if(views.containsKey(view.getName())) {
            throw new RuntimeException("There is an existing view with this name: " + view.getName());
        }
        if(!view.isExpanded()) {
            view.expand();
        }

        views.put(view.getName(), view);
    }

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

    /**
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
        needsUpdate = true;
    }

    public void refresh(TypeNarrower typeNarrower) {
        if(needsUpdate) {
            if(narrowedClassByView != null) {
                for(Class<?> clazz: narrowedClassByView.keySet())
                    populateNarrowedClass(clazz, typeNarrower);
            }

            needsUpdate = false;
        }
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

    public Class<?> getNarrowedClass(Class<?> entityClass, String viewName) {
        Map<String, Class<?>> narrowedByView = narrowedClassByView.get(entityClass);
        Object result = narrowedByView.get(viewName);

        if(logger.isDebugEnabled()) {
            logger.debug("AggregateViews#getNarrowedClass(entityClass: " + entityClass.getName()
                    + ", viewName: " + viewName);
            for(Map.Entry<String, Class<?>> entry: narrowedByView.entrySet()) {
                logger.debug("view: " + entry.getKey() + " narrowed class: " + ((Class<?>)entry.getValue()).getName());
            }
            if (result != null) {
                logger.debug("getNarrowedClass: " + ((Class<?>)result).getName());
            }
        }

        if(result == null)
            return null; // The entityClass is not applicable for this view

        if(Class.class.isAssignableFrom(result.getClass()))
            return (Class<?>) result;

        if(Set.class.isAssignableFrom(result.getClass())) {
            logger.error("Multiple sub-classes found, use a dynamic/custom TypeNarrower class to resolve this");
            return entityClass;
        }

        return null;
    }

    public void populateNarrowedClass(Class<?> superClass, TypeNarrower typeNarrower) {

        if(narrowedClassByView.get(superClass) != null) // already populated
            return;

        // Re-build in case the view has changed
        narrowedClassByView.put(superClass, new HashMap<>());

        // do the population for all views
        Map<String, Class<?>> byViews = narrowedClassByView.get(superClass);
        nextView: for(View view: views.values()) {

            Class<?> narrowedClass = null;
            Set multipleNarrowedClass = new HashSet();
            for(String propertyPath: view.getAttributes()) {
                String propertyName = Settings.getRootName(propertyPath);
                Class<?> potentialNarrowedClass = null;
                try {
                    potentialNarrowedClass = typeNarrower.narrow(this, superClass, propertyName);
                    if(potentialNarrowedClass == null)
                        continue nextView; // This view property does not exist for this superClass or its sub-classes

                    logger.debug("narrowedClass: " + (narrowedClass!=null ? narrowedClass.getName() : "null" )
                            + ", potentialNarrowedClass: " + potentialNarrowedClass.getName());

                    if(narrowedClass == null) {
                        narrowedClass = potentialNarrowedClass;
                    } else {
                        if(!narrowedClass.isAssignableFrom(potentialNarrowedClass))
                            multipleNarrowedClass.add(potentialNarrowedClass);
                        else
                            narrowedClass = potentialNarrowedClass; // Update it
                    }
                }
                catch (MultipleClassForPropertyException e) {
                    // The exception contains multiple classes
                    multipleNarrowedClass.add(e);
                }
            }

            if(multipleNarrowedClass.size() > 0) { // Might contain multiple classes or one or more MultipleClassForPropertyException objects
                if(narrowedClass != null) {
                    multipleNarrowedClass.add(narrowedClass);
                }

                Object commonClass = findCommonClass(multipleNarrowedClass);
                if(commonClass != null) {
                    byViews.put(view.getName(), (Class<?>)commonClass);
                }
            } else {
                if(narrowedClass != null) {
                    byViews.put(view.getName(), narrowedClass);
                }
            }
        }
    }

    private Object findCommonClass(Set result) {
        // First set all the classes to be eligible
        Map<Class<?>, Boolean> eligibleClass = new HashMap<>();

        for(Object obj: result) {
            if(Class.class.isAssignableFrom(obj.getClass()))
                eligibleClass.put((Class<?>) obj, Boolean.TRUE);
            else if(MultipleClassForPropertyException.class.isAssignableFrom(obj.getClass())) {
                MultipleClassForPropertyException me = (MultipleClassForPropertyException) obj;
                for(Class<?> clazz: me.getMatchedClasses())
                    eligibleClass.put(clazz, Boolean.TRUE);
            }
        }

        // markInEligible
        for(Object obj: result)
            markInEligible(eligibleClass, obj);

        Set<Class<?>> multipleCommonClasses = new HashSet<>();
        for(Map.Entry<Class<?>, Boolean> eligibleEntry: eligibleClass.entrySet()) {
            if(eligibleEntry.getValue())
                multipleCommonClasses.add(eligibleEntry.getKey());
        }

        if(multipleCommonClasses.size() == 0) {
            return null;
        } else if(multipleCommonClasses.size() == 1) {
            return multipleCommonClasses.iterator().next();
        } else {
            for(Class<?> clazz: multipleCommonClasses) {
                Type type = getType(clazz);
            }
            // This will be treated as an error in getNarrowedClass method
            return multipleCommonClasses;
        }
    }

    private void markInEligible(Map<Class<?>, Boolean> eligibleClass, Object obj) {
        next: for( Map.Entry<Class<?>, Boolean> entry : eligibleClass.entrySet()) {
            if(!entry.getValue()) // already ineligible
                continue;

            if(Class.class.isAssignableFrom(obj.getClass())) {
                if( ((Class<?>) obj).isAssignableFrom(entry.getKey()))
                    continue;

            } else if(MultipleClassForPropertyException.class.isAssignableFrom(obj.getClass())) {
                MultipleClassForPropertyException me = (MultipleClassForPropertyException) obj;

                for(Class<?> clazz: me.getMatchedClasses())
                    if( clazz.isAssignableFrom(entry.getKey()))
                        continue next;
            }
            eligibleClass.put(entry.getKey(), Boolean.FALSE);
        }
    }

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

    /**
     * If the shape was configured to created a topological ordering
     * then this graph is accessed using this method
     *
     * @return topological ordering of the shape
     */
    public StateGraph<State, Edge<State>> getOrderedGraph() {
        return this.orderedGraph;
    }
}
