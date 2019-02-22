/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

import tools.xor.service.DataAccessService;
import tools.xor.service.Shape;

import java.util.List;

/**
 * We do not implement ExtendedProperty since most the functionality from ExtendedProperty
 * is not captured in JDBC. Main motivation for this decision is to keep this simple.
 */
public class JDBCProperty extends AbstractProperty
{
    protected String   columnName;
    private boolean    isMany;
    private boolean    isContainment; // Should be true if there is cascade delete on this relationship
    protected Type     elementType;
    private boolean    nullable;

    public JDBCProperty(String name, String columnName, Type type, EntityType parentType, boolean nullable) {

        super(name, type, parentType);

        this.columnName = columnName;
        this.nullable = nullable;
    }

    /**
     * This is usually created for convenience since we navigate from top-down and the
     * foreign key relationships are bottom-up.
     *
     * @param isContainment true if the relationship has a cascade delete
     * @param elementType of the MANY side of the TO_MANY relationship
     * @param mappedBy is the property owning the TO_MANY relationship from a DB perspective
     */
    public void markToMany(boolean isContainment, Type elementType, String mappedBy) {
        this.isMany = true;
        this.elementType = elementType;
        this.isContainment = isContainment;

        if(mappedBy != null) {
            setMappedBy(this.elementType.getProperty(mappedBy), mappedBy);
        }
    }

    @Override public String getName ()
    {
        return this.name;
    }

    public String getColumnName ()
    {
        // If this is a TO_MANY property then columnName is the alias of the MANY side
        // this will be checked and added by the builder
        if(!isMany()) {
            return this.columnName;
        } else {
            throw new IllegalStateException("The MANY side of a TO_MANY relationship is an entity and not mapped to a column");
        }
    }

    @Override
    public void initBusinessLogicAnnotations ()
    {}

    @Override public boolean isMany ()
    {
        return this.isMany;
    }

    @Override public boolean isContainment ()
    {
        return this.isContainment;
    }

    @Override public void init (Shape shape)
    {

    }

    @Override public Object getDefault ()
    {
        return null;
    }

    @Override public void initMappedBy (DataAccessService das)
    {

    }

    @Override public boolean isNullable ()
    {
        return this.nullable;
    }

    @Override public boolean isOpenContent ()
    {
        return false;
    }

    @Override public List<?> getInstanceProperties ()
    {
        return null;
    }

    @Override public Object get (Property property)
    {
        return null;
    }

    @Override public boolean isManaged ()
    {
        return false;
    }

    @Override public boolean isInherited ()
    {
        return false;
    }

    @Override public boolean isMap ()
    {
        return false;
    }

    @Override public boolean isList ()
    {
        return false;
    }

    @Override public boolean isSet ()
    {
        return false;
    }

    @Override public boolean isCollectionOfReferences ()
    {
        return false;
    }
}
