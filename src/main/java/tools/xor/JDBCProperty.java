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

import tools.xor.providers.jdbc.JDBCDAS;
import tools.xor.service.DataAccessService;
import tools.xor.service.Shape;

import java.util.List;

/**
 * We do not implement ExtendedProperty since most the functionality from ExtendedProperty
 * is not captured in JDBC. Main motivation for this decision is to keep this simple.
 */
public class JDBCProperty extends AbstractProperty
{
    public static final String INVERSE = "−1";

    private boolean    isMany;
    private boolean    isOwner;       // Should be true if there is cascade delete on inverse relationship
    private boolean    nullable;
    private List<JDBCDAS.ColumnInfo> columns;
    private JDBCDAS.ForeignKey foreignKey;

    /**
     * JDBC property object constructor
     * @param name of the property
     * @param columns comprising this JDBC property
     * @param type of the property. Can be a simple JAVA wrapper or a POJO class.
     * @param parentType The EntityType containing this property
     * @param foreignKey Optional. This foreign key relationship gives rise to 2 properies.
     *                     The inverse gives rise to a collection property that will be created
     *                     in a future step.
     */
    public JDBCProperty(String name, List<JDBCDAS.ColumnInfo> columns, Type type, EntityType parentType, JDBCDAS.ForeignKey foreignKey) {
        super(name, type, parentType);

        this.columns = columns;
        this.foreignKey = foreignKey;

        this.nullable = true;
        // Even if a single column in NOT NULL, then the nullage flag should be false
        for(JDBCDAS.ColumnInfo ci: this.columns) {
            if(!ci.isNullable()) {
                this.nullable = false;
                break;
            }
        }
    }

    /**
     * JDBC property object constructor
     * @param name of the property
     * @param columns comprising this JDBC property
     * @param type of the property. Can be a simple JAVA wrapper or a POJO class.
     * @param parentType The EntityType containing this property
     */
    public JDBCProperty(String name, List<JDBCDAS.ColumnInfo> columns, Type type, EntityType parentType) {
        this(name, columns, type, parentType, null);
    }

    /**
     * This is a constructor for the collection property that is the inverse of a foreign key relationship
     *
     * @param name of the collection property. Auto generated as
     * @param type Always of type java.util.List
     * @param parentType EntityType containing the collection
     * @param elementType EntityType of the element
     */
    public JDBCProperty(String name, Type type, EntityType parentType, EntityType elementType)
    {
        super(name, type, parentType, RelationshipType.TO_MANY, elementType);
        this.isMany = true;
    }

    @Override public String getName ()
    {
        return this.name;
    }

    public String getColumnName ()
    {
        if(this.columns.size() > 1) {
            throw new UnsupportedOperationException("Currently a property containing multiple columns is not supported");
        }

        // If this is a TO_MANY property then columnName is the alias of the MANY side
        // this will be checked and added by the builder
        if(!isMany()) {
            return columns.get(0).getName();
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

    public boolean isOwner() {
        return this.isOwner;
    }

    @Override public boolean isContainment ()
    {
        return foreignKey != null && foreignKey.isContainment();
    }

    @Override public void init (Shape shape)
    {

    }

    @Override public Object getDefault ()
    {
        return null;
    }

    @Override public void initMappedBy (Shape shape)
    {
        if(this.foreignKey != null) {
            // create and link a new JDBCProperty this is the inverse
            // and representing the collection
            DataAccessService das = shape.getDAS();
            JDBCProperty inverse = new JDBCProperty(getName() + INVERSE, das.getType(java.util.List.class),
                (EntityType)getType(), getContainingType());
            shape.addProperty(inverse);

            inverse.setMappedBy(this, this.getName());
        }
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
