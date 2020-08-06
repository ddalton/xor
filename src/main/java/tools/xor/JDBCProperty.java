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

import java.util.List;

import javax.persistence.metamodel.Attribute;

import tools.xor.providers.jdbc.JDBCDataModel;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Constants;
import tools.xor.view.QueryBuilder;

/**
 * We do not implement ExtendedProperty since most the functionality from ExtendedProperty
 * is not captured in JDBC. Main motivation for this decision is to keep this simple.
 */
public class JDBCProperty extends AbstractProperty implements Cloneable
{
    public static final String INVERSE = "-1";

    private boolean    isMany;
    private boolean    isComposition;
    private boolean    nullable;
    private List<JDBCDataModel.ColumnInfo> columns;
    private JDBCDataModel.ForeignKey       foreignKey;
    private final JSONObjectProperty jsonObjectProperty;

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
    public JDBCProperty(String name, List<JDBCDataModel.ColumnInfo> columns, Type type, EntityType parentType, JDBCDataModel.ForeignKey foreignKey) {
        super(name, type, parentType);

        this.columns = columns;
        this.foreignKey = foreignKey;

        if(this.foreignKey != null) {
            this.relType = this.foreignKey.getType();
        }

        this.nullable = true;
        // Even if a single column in NOT NULL, then the nullable flag should be false
        for(JDBCDataModel.ColumnInfo ci: this.columns) {
            if(!ci.isNullable()) {
                this.nullable = false;
                break;
            }
        }

        if(this.columns.size() == 1) {
            addConstraint(Constants.XOR.CONS_LENGTH, this.columns.get(0).getLength());
        }

        jsonObjectProperty = new JSONObjectProperty(this);
    }

    public JDBCProperty(String name, Type type, EntityType parentType)
    {
        super(name, type, parentType);
        this.relType = RelationshipType.TO_ONE;

        jsonObjectProperty = new JSONObjectProperty(this);
    }

    /**
     * JDBC property object constructor
     * @param name of the property
     * @param columns comprising this JDBC property
     * @param type of the property. Can be a simple JAVA wrapper or a POJO class.
     * @param parentType The EntityType containing this property
     */
    public JDBCProperty(String name, List<JDBCDataModel.ColumnInfo> columns, Type type, EntityType parentType) {
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

        jsonObjectProperty = new JSONObjectProperty(this);
    }

    @Override
    // Is this an identify column or a generated column?
    // Supported only for a single column
    public boolean isGenerated() {

        if(this.columns != null && this.columns.size() == 1) {
            return this.columns.get(0).isGenerated();
        }

        return false;
    }

    @Override public String getName ()
    {
        return this.name;
    }

    public String getSelectList(String alias) {
        StringBuilder selectList = new StringBuilder();
        for(JDBCDataModel.ColumnInfo ci: this.columns) {
            if(selectList.length() > 0) {
                selectList.append(QueryBuilder.COMMA_DELIMITER);
            }
            selectList.append(alias + Settings.PATH_DELIMITER + ci.getName());
        }

        return selectList.toString();
    }

    public String getOnClause(String leftAlias, String rightAlias) {
        // Get the foreign key and the direction, so the aliases are applied on
        // the correct side
        // i.e., if the property has a foreign key then the referencing table is leftAlias
        // and referenced table is rightAlias and vice verse for the inverse

        JDBCDataModel.ForeignKey fk = this.foreignKey;
        boolean inverse = getMappedBy() != null ? true : false;
        if(fk == null) {
            if(getMappedBy() != null) {
                fk = ((JDBCProperty)getMappedBy()).getForeignKey();
                inverse = true;
            } else {
                throw new RuntimeException("Unable to find foreign key for relationship");
            }
        }

        List<String> leftColumns = !inverse ? fk.getReferencingColumns() : fk.getReferencedColumns();
        List<String> rightColumns = !inverse ? fk.getReferencedColumns() : fk.getReferencingColumns();

        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < leftColumns.size(); i++) {
            if(builder.length() > 0) {
                builder.append(" AND ");
            }
            builder.append(leftAlias + Settings.PATH_DELIMITER + leftColumns.get(i))
                .append(" = ")
                .append(rightAlias + Settings.PATH_DELIMITER + rightColumns.get(i));
        }

        return builder.toString();
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
        return isComposition || (foreignKey != null && foreignKey.isContainment()) ||
            // If this is on the inverse property
            (getMappedBy() != null && ((JDBCProperty)getMappedBy()).getForeignKey() != null && ((JDBCProperty)getMappedBy()).getForeignKey().isComposition());
    }

    public void setComposition(boolean value) {
        this.isComposition = value;
    }

    public List<JDBCDataModel.ColumnInfo> getColumns() {
        return this.columns;
    }

    @Override public Object getDefault ()
    {
        return null;
    }

    @Override public void initMappedBy (Shape shape) {
        initMappedBy(shape, this.foreignKey);
    }

    public void initMappedBy (Shape shape, JDBCDataModel.ForeignKey fk)
    {
        if(fk != null) {

            JDBCProperty inverse;

            String inverseRelationshipName = fk.getInverseRelationshipName();
            if(inverseRelationshipName == null) {
                inverseRelationshipName = getName() + INVERSE;
            }

            if(fk.getType() == RelationshipType.TO_MANY) {
                // create and link a new JDBCProperty this is the inverse
                // and representing the collection
                inverse = new JDBCProperty(
                    // We cannot use Iterable since we don't know a suitable concrete implementation
                    // for such an interface. So we resort to List.
                    //inverseRelationshipName, das.getType(java.lang.Iterable.class),
                    inverseRelationshipName, shape.getType(java.util.List.class),
                    (EntityType)getType(), getContainingType());
            } else {
                // the inverse is a ONE_TO_ONE relationship
                inverse = new JDBCProperty(
                    inverseRelationshipName, getContainingType(), (EntityType) getType());
            }
            shape.addProperty(inverse);
            inverse.setMappedBy(this, this.getName());
            inverse.setNullable(true);
        }
    }

    public JDBCDataModel.ForeignKey getForeignKey() {
        return this.foreignKey;
    }

    @Override public boolean isNullable ()
    {
        return this.nullable;
    }

    public void setNullable(boolean value) {
        this.nullable = value;
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
        // Only list collection is supported
        if(isMany()) {
            return true;
        }

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
        propertyValue = ClassUtil.getInstance(propertyValue);
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
    public Property refine (String name, Type type, EntityType parentType) {
        JDBCProperty result = null;
        try {
            result = (JDBCProperty)this.clone();
            result.name = name;

            if(!isMany()) {
                setType(type);
            } else { // changing the element type
                this.elementType = type;
            }
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public Attribute.PersistentAttributeType getAssociationType() {
        Attribute.PersistentAttributeType result = super.getAssociationType();
        if(getRelationshipType() == RelationshipType.TO_ONE) {
            result = Attribute.PersistentAttributeType.ONE_TO_ONE;
        }

        return result;
    }

    @Override
    public boolean doPropagateId() {
        // Valid only if a foreign key exists between the primary key of the
        // owner and dependant object
        // This can happen in 2 cases:
        // 1. Between a sub-type and super-type object in an inheritance hierarachy
        // 2. A composition relationship

        if(getMappedBy() != null && ((JDBCProperty)getMappedBy()).getForeignKey() != null) {
            // Case 1 - Check inverse inheritance relationship
            if(((JDBCProperty)getMappedBy()).getForeignKey().isInheritance()) {
                return true;
            }
            // Case 2 - Check composition flag
            else if(((JDBCProperty)getMappedBy()).getForeignKey().isComposition()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Copy the id values from the owner object to the dependant object
     * @param owner business object
     * @param dependant business object
     */
    public void propagateId(BusinessObject owner, BusinessObject dependant) {

        if(EntityType.class.isAssignableFrom(dependant.getType().getClass())) {
            EntityType dependantType = (EntityType) dependant.getType();
            Property idProperty = dependantType.getIdentifierProperty();

            if(idProperty == null) {
                // get Property by name from referenced table column name
                String name = ((JDBCProperty)getMappedBy()).getForeignKey().getReferencedColumns().get(0);
                idProperty = dependantType.getProperty(name);
            }

            dependant.set(idProperty, owner.getIdentifierValue());
        }
    }

    @Override
    public boolean isUpdatable() {
        return !(getForeignKey() != null && (getForeignKey().isComposition() || getForeignKey().isInheritance()));
    }
    
    @Override
    public String toString() {
        return String.format("Name: %s, Type: %s, isDataType: %s, isNullable: %s, Containing type: %s", getName(),
                getType().getName(), isDataType(), isNullable(),
                (getContainingType() != null) ? getContainingType().getName() : "N/A");
    }
}
