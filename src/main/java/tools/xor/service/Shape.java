package tools.xor.service;

import tools.xor.AbstractType;
import tools.xor.EntityType;
import tools.xor.ExternalType;
import tools.xor.OpenType;
import tools.xor.Property;
import tools.xor.SimpleType;
import tools.xor.SimpleTypeFactory;
import tools.xor.Type;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
public class Shape
{
    // Move the following to Shape abstraction
    protected Map<String, Type> types = new ConcurrentHashMap<String, Type>();
    protected Map<String, Type> derivedTypes = new ConcurrentHashMap<String, Type>();
    protected DataAccessService das;
    protected String name;
    protected Shape parent;

    public Shape(String name, Shape parent, DataAccessService das) {
        this.name = name;
        this.das = das;
        this.parent = parent;
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

    protected void addDerivedType(String className, Type type) {
        addType(className, type, derivedTypes);
    }

    /**
     * We do not look to the parent shape, since a Shape should be self sufficient
     * in all the types it is responsible for.
     *
     * @param name of the type
     * @return type instance
     */
    public Type getType(String name) {
        return types.get(name);
    }

    /**
     * We do not look to the parent shape, since a Shape should be self sufficient
     * in all the types it is responsible for.
     *
     * @param name of the type
     * @return external type instance
     */
    public Type getExternalType(String name) {
        return derivedTypes.get(name);
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
            addDerivedType(clazz.getName(), result);
        }

        return result;
    }

    public void addOpenType(OpenType type) {
        if(types.containsKey(type.getName())) {
            throw new RuntimeException("A type with the same name exists, please choose a different name for the open type: " + type.getName());
        }
        type.setProperty(das);
        addType(type.getName(), type);

        Class<?> derivedClass = das.getTypeMapper().toExternal(type.getInstanceClass());
        if(derivedClass != null) {
            ExternalType derived = das.getTypeMapper().createExternalType(
                (EntityType)type,
                derivedClass);
            derivedTypes.put(derived.getName(), derived);
            derived.setProperty(das, this);
            setBiDirectionOnDerivedType(derived);
        }
    }

    protected void setBiDirectionOnDerivedType(ExternalType derivedType) {
        derivedType.setOpposite(das);
    }

    public void addProperty (EntityType type, Property openProperty) {
        type.addProperty(openProperty);

        if(derivedTypes.containsKey(type.getName())) {
            ExternalType derived = (ExternalType) derivedTypes.get(type.getName());
            if(derived == null) {
                throw new RuntimeException("Cannot find the derived type for: " + type.getName());
            }
            Property derivedProperty = derived.defineProperty(das, openProperty, this);
            derived.addProperty(derivedProperty);
        }
    }

    protected void initDerived() {
        for(Type type: getUniqueTypes()) {
            if(SimpleType.class.isAssignableFrom(type.getClass()) || type.isOpen()) {
                continue;
            }
            Class<?> derivedClass = das.getTypeMapper().toExternal(type.getInstanceClass());
            if(derivedClass != null) {
                Type derived = das.getTypeMapper().createExternalType(
                    (EntityType)type,
                    derivedClass);
                derivedTypes.put(derived.getName(), derived);
            }
        }

        // init the derived properties
        for (Type type : getUniqueDerivedTypes()) {
            if (ExternalType.class.isAssignableFrom(type.getClass())) {
                ExternalType derivedType = (ExternalType) type;

                if(isOpenDomainType(derivedType)) {
                    continue;
                }
                derivedType.setProperty(das, this);
            }
        }

        for (Type type : getUniqueDerivedTypes()) {
            if (!type.isOpen() && ExternalType.class.isAssignableFrom(type.getClass())) {
                ExternalType derivedType = (ExternalType) type;

                if(isOpenDomainType(derivedType)) {
                    continue;
                }
                setBiDirectionOnDerivedType(derivedType);
            }
        }
    }


    public Set<Type> getUniqueTypes() {
        return new HashSet<Type>(types.values());
    }

    private Set<Type> getUniqueDerivedTypes() {
        return new HashSet<Type>(derivedTypes.values());
    }

    public void initRootType() {
        for (Type type : getUniqueTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass()) && !type.isOpen()) {
                ((AbstractType)type).initRootEntityType(das, this);
            }
        }
        for (Type type : getUniqueDerivedTypes()) {
            if (AbstractType.class.isAssignableFrom(type.getClass())) {
                ((AbstractType)type).initRootEntityType(das, this);
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

    private boolean isOpenDomainType(ExternalType derivedType) {
        // If the domain type is open, then we cannot
        // infer the properties. For e.g., an open domain type
        // does not have a Java class and is populated dynamically
        return derivedType.getDomainType().isOpen();
    }

}
