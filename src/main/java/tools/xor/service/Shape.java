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
import tools.xor.ExternalType;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.SimpleType;
import tools.xor.SimpleTypeFactory;
import tools.xor.Type;
import tools.xor.TypeNarrower;
import tools.xor.exception.MultipleClassForPropertyException;
import tools.xor.util.AggregatePropertyPaths;
import tools.xor.util.DFAtoRE;
import tools.xor.util.Edge;
import tools.xor.util.GraphUtil;
import tools.xor.util.Vertex;
import tools.xor.util.graph.DirectedGraph;
import tools.xor.util.graph.DirectedSparseGraph;
import tools.xor.view.AggregateView;
import tools.xor.view.QueryViewProperty;
import tools.xor.view.UnmodifiableView;
import tools.xor.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    // This functionality helps to get the correct narrowed class (subtype) based on the properties
    // defined by the view
    // It is map between the given class and the view and the narrowed class
    protected Map<Class<?>, Map<String, Class<?>>> narrowedClassByView = new ConcurrentHashMap<Class<?>, Map<String,Class<?>>>();
    private volatile boolean needsUpdate;

    protected ShapeStrategy shapeStrategy = ShapeStrategy.SHARED;

    public enum ShapeStrategy {
        SHARED,
        COPY
    };

    public Shape(String name, Shape parent, DataAccessService das) {
        this.name = name;
        this.das = das;
        this.parent = parent;
    }

    public ShapeStrategy getShapeStrategy() {
        return this.shapeStrategy;
    }

    public String getName() {
        return this.name;
    }

    public void addType(String className, Type type) {
        addType(className, type, types);
    }

    protected void addType(String className, Type type, Map<String, Type> typeMap) {
        if(typeMap.containsKey(className)) {
            return;
        }

        typeMap.put(className, type);

        if(EntityType.class.isAssignableFrom(type.getClass())) {
            ((EntityType)type).setDAS(das);
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

    protected void addExternalType (String className, Type type) {
        addType(className, type, externalTypes);
    }

    /**
     * We do not look to the parent shape, since a Shape should be self sufficient
     * in all the types it is responsible for.
     *
     * @param name of the type
     * @return type instance
     */
    public Type getType(String name) {
        if(this.shapeStrategy == ShapeStrategy.COPY) {
            return types.get(name);
        } else if(this.shapeStrategy == ShapeStrategy.SHARED) {
            if(types.containsKey(name)) {
                return types.get(name);
            } else if(parent != null) {
                return parent.getType(name);
            }
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

        if(this.shapeStrategy == ShapeStrategy.COPY) {
            return externalTypes.get(name);
        } else if(this.shapeStrategy == ShapeStrategy.SHARED) {
            if(externalTypes.containsKey(name)) {
                return externalTypes.get(name);
            } else if(parent != null) {
                return parent.getExternalType(name);
            }
        }

        return null;
    }

    public Type getType(Class<?> clazz) {

        Type result = getType(clazz.getName());

        // create a Type object for this class
        if(result == null) {
            //result = new SimpleType(clazz);
            result = SimpleTypeFactory.getType(clazz, das);

            addType(clazz.getName(), result);
        }

        return result;
    }

    public Type getExternalType(Class<?> clazz) {
        Type result = getExternalType(clazz.getName());

        // create a Type object for this class
        if (result == null) {
            //result = new SimpleType(clazz);
            result = SimpleTypeFactory.getType(clazz, das);
            addExternalType(clazz.getName(), result);
        }

        return result;
    }

    public void addOpenType(OpenType type) {
        if(types.containsKey(type.getName())) {
            throw new RuntimeException("A type with the same name exists, please choose a different name for the open type: " + type.getName());
        }
        type.setProperty(das);
        addType(type.getName(), type);

        Class<?> externalClass = das.getTypeMapper().toExternal(type.getInstanceClass());
        if(externalClass != null) {
            ExternalType externalType = das.getTypeMapper().createExternalType(
                (EntityType)type,
                externalClass);
            externalTypes.put(externalType.getName(), externalType);
            externalType.setProperty(das, this);
            setBiDirectionOnExternalType(externalType);
        }
    }

    protected void setBiDirectionOnExternalType (ExternalType externalType) {
        externalType.setOpposite(das);
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

    public void addProperty (EntityType type, Property openProperty) {
        type.addProperty(openProperty);

        if(externalTypes.containsKey(type.getName())) {
            ExternalType externalType = (ExternalType) externalTypes.get(type.getName());
            if(externalType == null) {
                throw new RuntimeException("Cannot find the external type for: " + type.getName());
            }
            Property externalProperty = externalType.defineProperty(das, openProperty, this);
            externalType.addProperty(externalProperty);
        }
    }

    public void removeProperty (EntityType type, Property openProperty) {
        type.removeProperty(openProperty);

        if(externalTypes.containsKey(type.getName())) {
            ExternalType externalType = (ExternalType) externalTypes.get(type.getName());
            if(externalType == null) {
                throw new RuntimeException("Cannot find the external type for: " + type.getName());
            }

            Property externalProperty = externalType.getProperty(openProperty.getName());
            externalType.removeProperty(externalProperty);
        }
    }

    protected void deriveExternal () {
        for(Type type: getUniqueTypes()) {
            if(SimpleType.class.isAssignableFrom(type.getClass()) || type.isOpen()) {
                continue;
            }
            Class<?> externalClass = das.getTypeMapper().toExternal(type.getInstanceClass());
            if(externalClass != null) {
                Type externalType = das.getTypeMapper().createExternalType(
                    (EntityType)type,
                    externalClass);
                externalTypes.put(externalType.getName(), externalType);
            }
        }

        // init the properties
        for (Type type : getUniqueExternalTypes()) {
            if (ExternalType.class.isAssignableFrom(type.getClass())) {
                ExternalType externalType = (ExternalType) type;

                if(isOpenDomainType(externalType)) {
                    continue;
                }
                externalType.setProperty(das, this);
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
                ((AbstractType)type).initRootEntityType(das, this);
            }
        }
        for (Type type : getUniqueExternalTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).initRootEntityType(das, this);
            }
        }
    }

    public void initEnd() {
        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).initEnd(this);
            }
        }
    }

    public void initPositionProperty() {
        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).initPositionProperty();
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

    public View getView(EntityType type) {

        String viewName = AbstractType.getViewName(type);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(type, viewName);
            Set<String> paths = AggregatePropertyPaths.enumerate(type, this);

            DFAtoRE dfaRE = new DFAtoRE(type, this);
            result.addTypeGraph(type, dfaRE.getFullStateGraph());

            updateView(result, viewName, paths);
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
            result = new AggregateView(type, viewName);
            Set<String> paths = AggregatePropertyPaths.enumerateBase(type);

            updateView(result, viewName, paths);
        }

        return getView(viewName);
    }

    public View getRefView(EntityType type) {

        String viewName = AbstractType.getRefViewName(type);
        View result = getView(viewName);

        if(result == null) {
            result = new AggregateView(type, viewName);
            Set<String> paths = AggregatePropertyPaths.enumerateRef(type);

            updateView(result, viewName, paths);
        }

        return getView(viewName);
    }

    private void updateView(View view, String viewName, Set<String> paths) {
        view.setAttributeList(new ArrayList<String>(paths));
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

            selected = selected.copy();
            selected.setShape(this);
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
                dg.addEdge(new Edge<AggregateView>(edge, start, end), start, end);
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
        narrowedClassByView.put(superClass, new HashMap<String, Class<?>>());

        // do the population for all views
        Map<String, Class<?>> byViews = narrowedClassByView.get(superClass);
        nextView: for(View view: views.values()) {

            Class<?> narrowedClass = null;
            Set multipleNarrowedClass = new HashSet();
            for(String propertyPath: view.getAttributeList()) {
                String propertyName = QueryViewProperty.getRootName(propertyPath);
                Class<?> potentialNarrowedClass = null;
                try {
                    potentialNarrowedClass = typeNarrower.narrow(superClass, propertyName);
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
        // First set all the classes to be eligble
        Map<Class<?>, Boolean> eligibleClass = new HashMap<Class<?>, Boolean>();

        for(Object obj: result) {
            if(Class.class.isAssignableFrom(obj.getClass()))
                eligibleClass.put((Class<?>) obj, Boolean.TRUE);
            else if(MultipleClassForPropertyException.class.isAssignableFrom(obj.getClass())) {
                MultipleClassForPropertyException me = (MultipleClassForPropertyException) obj;
                for(Class<?> clazz: me.getMatchedClasses())
                    eligibleClass.put((Class<?>) obj, Boolean.TRUE);
            }
        }

        // markInEligible
        for(Object obj: result)
            markInEligible(eligibleClass, (Class<?>) obj);

        Set<Class<?>> multipleCommonClasses = new HashSet<Class<?>>();
        for(Map.Entry<Class<?>, Boolean> eligibleEntry: eligibleClass.entrySet()) {
            if(eligibleEntry.getValue())
                multipleCommonClasses.add(eligibleEntry.getKey());
        }

        if(multipleCommonClasses.size() == 0) {
            return null;
        } else if(multipleCommonClasses.size() == 1) {
            return multipleCommonClasses.iterator().next();
        } else {
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
}
