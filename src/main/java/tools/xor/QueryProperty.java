/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2020, Dilip Dalton
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.xor.service.Shape;

/**
 * Needed when QueryType is a DQOR. DQOR types do not have a basedOn type.
 * So QueryProperty instances need to created. 
 * This is needed to support the use case of the type definition being specified in the view
 *
 * Native queries can use a built-in type or a DQOR type
 *
 */
public class QueryProperty extends AbstractProperty {

    private final JSONObjectProperty jsonObjectProperty;

    public QueryProperty(String name, Type type, EntityType parentType) {
        super(name, type, parentType);
        jsonObjectProperty = new JSONObjectProperty(this);
    }

    public QueryProperty(String name, Type type, EntityType parentType, RelationshipType relType, Type elementType) {
        super(name, type, parentType, relType, elementType);
        jsonObjectProperty = new JSONObjectProperty(this);
    }

    @Override
    public boolean isMap() {
        return Map.class.isAssignableFrom(getType().getInstanceClass());
    }

    @Override
    public boolean isList() {
        return List.class.isAssignableFrom(getType().getInstanceClass());
    }

    @Override
    public boolean isSet() {
        return Set.class.isAssignableFrom(getType().getInstanceClass());
    }

    @Override
    public boolean isCollectionOfReferences() {
        return false;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isMany() {
        return isMap() || isList() || isSet();
    }

    @Override
    public Object getDefault() {
        return null;
    }

    @Override
    public boolean isNullable() {
        return true;
    }

    @Override
    public List<?> getInstanceProperties() {
        return new ArrayList<Object>();
    }

    @Override
    public String getStringValue(BusinessObject dataObject)
    {
        return this.jsonObjectProperty.getStringValue(dataObject);
    }

    @Override
    public Object query(Object dataObject) {
        return this.jsonObjectProperty.query(dataObject);
    }

    @Override
    public Object getValue(BusinessObject dataObject)
    {
        return this.jsonObjectProperty.getValue(dataObject);
    }

    @Override
    public void setValue(Settings settings, Object dataObject, Object propertyValue)
    {
        this.jsonObjectProperty.setValue(settings, dataObject, propertyValue);
    }

    @Override
    public void addElement(BusinessObject dataObject, Object element) {

        this.jsonObjectProperty.addElement(dataObject, element);
    }

    @Override
    public void addMapEntry(Object dataObject, Object key, Object value) {
        this.jsonObjectProperty.addMapEntry(dataObject, key, value);
    }

    @Override
    public Object get(Property property) {
        return null;
    }
/*
    @Override
    public void init(Shape shape) {
        // QueryType is not used in copying between external and domain types
        // as there is no domain type involved. 
        // QueryType is both external and the domain type
    }
*/
    @Override
    public void initMappedBy(Shape shape) {
        // bi-directional relationship is not automatically handled in QueryType
    }

}
