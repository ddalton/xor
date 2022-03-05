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

package tools.xor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.service.Shape;
import tools.xor.view.Field;

/**
 * A temporary type that is used for Queries. The user can modify the shape of this type
 * as desired and hence we don't persist it. It could be cached for performance if the shape
 * of this type rarely changes.
 *
 * Because it is temporary, it has a randomly generated name, that is mainly used for debugging.
 * Cached QueryType instances have a user provided name.
 */
public class QueryType extends AbstractType {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private String name;
    private EntityType basedOn; // Optional. If not provided, then all the properties need to be described using aliases
                                //   and the QueryType is a dynamic type
    private Map<String, Property> properties; // Properties in addition to the basedOn type
    private List<Field> selfJoins; // Needed for root query type as it does not have a parent,
                                                         // makes sense only for properties that have basedOn populated

    /**
     * Create a QueryType instance from properties belonging to basedOn type
     * 
     * @param basedOn EntityType on which this QueryType is based on
     * @param properties whose aliases are modelled in QueryType
     */
    public QueryType(EntityType basedOn, Map<String, Property> properties) {
        this(StringType.randomAlphanumeric(16).toUpperCase(), basedOn.getShape(), properties);
        this.basedOn = basedOn;
    }
    
    /**
     * Create a QueryType instance from Field instances
     * 
     * @param name user provided name for the QueryType
     * @param shape to which this QueryType belongs
     * @param properties that belong to the QueryType
     */
    public QueryType(String name, Shape shape, Map<String, Property> properties) {
        super();

        this.name = name;
        this.properties = properties != null ? new ConcurrentHashMap<>(properties) : new ConcurrentHashMap<>();
        this.selfJoins = new LinkedList<>();
        setShape(shape);
    }    

    public void addSelfJoin(Field alias) {
        selfJoins.add(alias);
    }

    @Override
    public String getName() {
    		//return (basedOn == null) ? name : basedOn.getName();
        return ((basedOn == null) ? name : basedOn.getName()) + " ["+name+"]";
    }

    @Override
    public String getEntityName() {
        return getName();
    }

    public EntityType getBasedOn() {
        return this.basedOn;
    }

    @Override
    public List<Type> getEmbeddableTypes() {
        return basedOn == null ? new ArrayList<>() : this.basedOn.getEmbeddableTypes();
    }

    @Override
    public String getURI() {
        return null;
    }

    @Override
    public Class<?> getInstanceClass() {
        // A query type does not have a corresponding domain class as it is a temporary type
        return null;
    }

    @Override
    public String toString() {
        return getName() + (basedOn == null ? "" : " [" + this.basedOn.getName() + "]");
    }

    @Override
    public boolean isInstance(Object object) {
        return false;
    }

    @Override
    public void addProperty(Property property) {
        this.properties.put(property.getName(), property);
    }

    @Override
    public Property getProperty(String path) {
        if(this.properties.containsKey(path)) {
            return this.properties.get(path);
        } else if(basedOn != null){
            return this.basedOn.getProperty(path);
        }
        
        return null;
    }

    @Override
    public List<Property> getProperties() {
        List<Property> result = super.getProperties();
        if(this.properties.size() > 0) {
            result.addAll(properties.values());
        }

        return result;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isSequenced() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return true;
    }

    @Override
    public List<Property> getDeclaredProperties() {
        return getProperties();
    }

    @Override
    public List<?> getAliasNames() {
        return new ArrayList<String>();
    }

    @Override
    public List<?> getInstanceProperties() {
        return new ArrayList<>();
    }

    @Override
    public Object get(Property property) {
        return null;
    }

    @Override
    public AccessType getAccessType() {
        throw new UnsupportedOperationException("A query type does not define an access mechanism");
    }

    @Override
    public Property getIdentifierProperty() {
        return basedOn == null ? null : this.basedOn.getIdentifierProperty();
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public boolean isEntity() {
        return true;
    }

    @Override
    public Property getVersionProperty() {
        return basedOn == null ? null : this.basedOn.getVersionProperty();
    }

    @Override
    public boolean supportsDynamicUpdate() {
        return false;
    }

    @Override
    public List<EntityType> findInSubtypes (String property) {
        return basedOn == null ? new ArrayList<>() : this.basedOn.findInSubtypes(property);
    }
}
