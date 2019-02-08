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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.service.DataAccessService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A temporary type that is used for Queries. The user can modify the shape of this type
 * as desired and hence we don't persist it. It could be cached for performance if the shape
 * of this type rarely changes.
 *
 *
 * Because it is temporary, it has a randomly generated name, that is mainly used for debugging.
 */
public class QueryType extends AbstractType {
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private String name;
    private EntityType basedOn;
    private Map<String, Property> properties;

    public QueryType(EntityType basedOn, Map<String, Property> properties) {
        super();

        this.name = RandomStringUtils.randomAlphanumeric(16).toUpperCase();
        this.basedOn = basedOn;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEntityName() {
        return getName();
    }

    @Override
    public List<Type> getEmbeddableTypes() {
        return this.basedOn.getEmbeddableTypes();
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
    public boolean isInstance(Object object) {
        return false;
    }

    @Override
    public Property getProperty(String path) {
        if(this.properties.containsKey(path)) {
            return this.properties.get(path);
        } else {
            return this.basedOn.getProperty(path);
        }
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
    public List<Type> getBaseTypes() {
        return new ArrayList<>();
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
        return this.basedOn.getIdentifierProperty();
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
        return this.basedOn.getVersionProperty();
    }

    @Override
    public boolean supportsDynamicUpdate() {
        return false;
    }

}
