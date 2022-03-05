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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.EntityType;
import tools.xor.ExternalType;
import tools.xor.Property;
import tools.xor.SimpleType;
import tools.xor.Type;
import tools.xor.TypeMapper;
import tools.xor.view.AggregateView;
import tools.xor.view.AggregateViewFactory;
import tools.xor.view.AggregateViews;

/**
 * Represents the external type system
 * 
 * @author Dilip Dalton
 *
 */
public class DynamicShape extends AbstractShape
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    
    private Set<AggregateView> registrations = new HashSet<>();

    public DynamicShape(String name, Shape parent, DataModel das) {
        super(name, parent, das);
    }
    
    public DynamicShape(String name, Shape parent, DataModel das, Inheritance typeInheritance) {
        super(name, parent, das, typeInheritance);
    }        
    
    public DynamicShape(String name, Shape parent, Shape domainShape, TypeMapper typeMapper) {
        // We are going to create the dynamic shape from the provided domain shape
        super(name, parent, domainShape.getDataModel());
        
        // Create the types belonging to this shape
        createTypes(domainShape, typeMapper);
    }   
    
    private void createTypes(Shape domainShape, TypeMapper typeMapper) {
        Map<Type, Type> externalTypeMap = new HashMap<>();
        for(Type type: domainShape.getUniqueTypes()) {
            if(SimpleType.class.isAssignableFrom(type.getClass()) || type.isOpen()) {
                continue;
            }
            ExternalType externalType = getOrCreateType(type, typeMapper);
            if(externalType != null) {
                externalTypeMap.put(externalType, type);
            }
        }

        // init ofType
        for(Type type: domainShape.getUniqueTypes()) {
            if(type.ofType() != null) {
                ExternalType ofType = getOrCreateType(type.ofType(), typeMapper);
                ExternalType externalType = (ExternalType)getType(((EntityType)type).getEntityName());
                externalType.setOfType(ofType);
            }
        }

        // init the properties
        for (Type type : externalTypeMap.keySet()) {
            ExternalType externalType = (ExternalType) type;
            externalType.setProperty(domainShape, this, typeMapper);
        }

        for (Type type : externalTypeMap.keySet()) {
            ExternalType externalType = (ExternalType) type;
            EntityType domainType = (EntityType) externalTypeMap.get(externalType);
            setSuperTypeOnExternalType(domainShape, externalType, domainType);
            externalType.setOpposite(this);
            externalType.initParentTypes(domainType, typeMapper);
        }
    }

    private ExternalType getOrCreateType(Type type, TypeMapper typeMapper) {
        Class<?> externalClass = typeMapper.toExternal(type);
        if(externalClass != null) {
            ExternalType externalType = getDataModel().getTypeMapper().createExternalType(
                (EntityType)type,
                externalClass);
            externalType.setDomainTypeName(((EntityType)type).getEntityName());
            addType(externalType.getName(), externalType);
            if(!externalType.getName().equals(externalType.getDomainTypeName())) {
                addType(externalType.getDomainTypeName(), externalType); // Used to find external type from Domain type entity name
            }

            return externalType;
        }

        return null;
    }
    
    private void setSuperTypeOnExternalType(Shape domainShape, ExternalType externalType, EntityType domainType) {
        if(domainType.getParentType() == null) {
            return;
        }

        Type superType = getType(domainType.getParentType().getEntityName());
        if(superType instanceof EntityType) {
            externalType.setParentType((EntityType)superType);
        }        
        
        // Also set up the root entity type
        externalType.setRootEntityType(domainType.getRootEntityType().getEntityName());
    }
    
    public void signalEvent () {
        // Currently no action is done for a dynamic shape
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
    
    private void extractTypes(String fileName, DomainShape domainShape, TypeMapper typeMapper) {
        List<AggregateView> views = new ArrayList<>();
        
        if(fileName != null) {
            AggregateViews queryViews = AggregateViewFactory.load(fileName);
            views = new ArrayList<>(queryViews.getAggregateView());
        }
        
        for(AggregateView view: registrations) {
            views.add(view);
        }

        extractTypes(views, domainShape, typeMapper);
    }
    
    private ExternalType createType(AggregateView view, Map<String, ExternalType> typeMap, TypeMapper typeMapper) {
        // Extract the aliases
        view.initAliases();
        
        Map<String, Property> properties = new HashMap<>();
        ExternalType et = null;
        // Create the ExternalType from the view transform instances
        //ExternalType et = typeMapper.createExternalType();
        //typeMap.put(et.getName(), et);
        
        if(view.getChildren() != null) {
            for(AggregateView child: view.getChildren()) {
                createType(child, typeMap, typeMapper);
            }
        }
        
        return et;
    }    
    
    /**
     * Construct ExternalType and the corresponding ExternalProperty types from the view
     * and add them to the Shape
     * 
     * @param views whose ExternalType instances need to be extracted
     * @param domainShape used for finding the property type. If this is not provided we have to 
     *        get this information from the transform object 
     */
    private void extractTypes(List<AggregateView> views, DomainShape domainShape, TypeMapper typeMapper) {
        // scan through the transform properties, both the selected and the addendum (for object linking)
        Map<String, ExternalType> queryTypeMap = new HashMap<>();
        
        // PASS 1
        for(AggregateView view: views) {
            // 1a
            ExternalType qt = createType(view, queryTypeMap, typeMapper);
            
            // 1b
            
            
            // We only add the root QueryType to the shape
            addType(qt.getName(), qt);
        }
        
        // PASS 2
    }    
    
    /**
     * After all the views have been registered, then the ExternalType instances corresponding to those views
     * will be generated.
     * 
     * @param fileName of the views that are read from a file. If not provided user should register the views using
     *   register method.
     * @param domainShape domain shape instance
     * @param typeMapper instance
     * @see DynamicShape#register(AggregateView, String)
     */
    public void process(String fileName, DomainShape domainShape, TypeMapper typeMapper) {
        extractTypes(fileName, domainShape, typeMapper);
    }    
    
    @Override
    // ExternalType can model multiple inheritance
    public Property getProperty(EntityType type, String name) {
        Property result = null;

        if (type instanceof ExternalType) {
            Queue<ExternalType> pending = new LinkedList<>();
            if(type.getParentTypes() != null) {
                pending.addAll(((ExternalType) type).getParentTypes());
            }
            ExternalType current = (ExternalType) type;
            do {
                if (getDeclaredProperties(current) != null && getDeclaredProperties(current).containsKey(name)) {
                    result = getDeclaredProperties(current).get(name);
                }
                current = (ExternalType) pending.poll();
                if(current != null && current.getParentTypes() != null) {
                    pending.addAll(current.getParentTypes());
                }
            } while (result == null && current != null);

            if (result == null && this.shapeInheritance == Inheritance.REFERENCE && parent != null) {
                result = parent.getProperty(type, name);
            }
        } else {
            return super.getProperty(type, name);
        }

        return result;
    }   
}
