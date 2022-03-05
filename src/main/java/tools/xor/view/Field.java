package tools.xor.view;

import tools.xor.Settings;
import tools.xor.util.ClassUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * propertyName - required.
 * alias - optional
 * typeName - alias type, can be a subtype of the type of propertyName
 * viewName - optional, represents the properties added to the alias
 * subFields - If this is provided, then it overrides the view.
 * elementType - if the typeName is a list of an EntityType, then elementType refers to that EntityType
 */
public class Field
{
    String alias;
    String propertyName;
    String typeName;
    String viewName;        // Could give rise to an interquery edge if the view is a custom view
    String elementType;
    List<Field> subFields;  // If typeName is an EntityType, then refers to the fields of that entity
                            // This is also valid if the typeName is a list type and the element type
                            // is an entity type.
                            // Multiple fields can refer to the propertyName and can be distinguished
                            // by the typeName

    // Default constructor needed for marshalling (XML to Object)
    public Field() {}

    public Field (String alias,
                  String propertyName,
                  String typeName,
                  String viewName)
    {
        this.alias = alias;
        this.propertyName = propertyName;
        this.typeName = typeName;
        this.viewName = viewName;

        assert (this.alias != null);
    }

    public Field (String alias,
                  String propertyName,
                  String typeName,
                  String viewName,
                  String elementType)
    {
        this(alias, propertyName, typeName, viewName);

        this.elementType = elementType;
    }

    public String getAlias ()
    {
        return alias;
    }

    public void setAlias (String alias)
    {
        this.alias = alias;
    }

    public String getPropertyName ()
    {
        return propertyName;
    }

    public void setPropertyName (String propertyName)
    {
        this.propertyName = propertyName;
    }

    public String getTypeName ()
    {
        return typeName;
    }

    public void setTypeName (String typeName)
    {
        this.typeName = typeName;
    }

    public String getViewName ()
    {
        return viewName;
    }

    public void setViewName (String viewName)
    {
        this.viewName = viewName;
    }

    public String getElementType ()
    {
        return elementType;
    }

    public void setElementType (String elementType)
    {
        this.elementType = elementType;
    }

    public boolean isViewReference ()
    {
        return this.viewName != null && !"".equals(this.viewName.trim());
    }

    public String getOriginal ()
    {
        return Settings.getBaseName(this.propertyName);
    }

    @Override
    public boolean equals (Object o)
    {
        if (this == o) {
            return true;
        }

        if (!Field.class.isAssignableFrom(o.getClass())) {
            return false;
        }

        Field other = (Field)o;
        if (!alias.equals(other.alias)) {
            return false;
        }

        if( (propertyName != null ? propertyName.equals(other.propertyName) : other.propertyName == null) &&
            (typeName != null ? typeName.equals(other.typeName) : other.typeName == null) &&
            (viewName != null ? viewName.equals(other.viewName) : other.viewName == null) &&
            (elementType != null ? elementType.equals(other.elementType) : other.elementType == null)
        )
        {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode ()
    {
        int h = 17;

        h = 31 * h + alias.hashCode();
        h = propertyName != null ? (31 * h + propertyName.hashCode()) : h;
        h = typeName != null ? (31 * h + typeName.hashCode()) : h;
        h = viewName != null ? (31 * h + viewName.hashCode()) : h;
        h = elementType != null ? (31 * h + elementType.hashCode()) : h;

        return h;
    }

    public Field copy ()
    {
        Field fieldCopy = new Field(alias, propertyName, typeName, viewName, elementType);

        if(subFields != null) {
            List<Field> subFieldsCopy = new ArrayList<>();
            for(Field subField: subFields) {
                subFieldsCopy.add(subField.copy());
            }

            fieldCopy.setSubFields(subFieldsCopy);
        }

        return fieldCopy;
    }

    public Class getTypeJavaClass ()
    {
        return getJavaClass(this.typeName);
    }

    public Class getElementTypeJavaClass ()
    {
        return getJavaClass(this.elementType);
    }

    public Class getJavaClass (String tname)
    {
        if (typeToClassMap.containsKey(tname.toUpperCase())) {
            return typeToClassMap.get(tname.toUpperCase());
        }

        // Try to find the class using the class loader
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(tname);
        }
        catch (ClassNotFoundException e) {
            throw ClassUtil.wrapRun(e);
        }
    }

    private static Map<String, Class> typeToClassMap = new HashMap<>();

    static {
        typeToClassMap.put("LIST", List.class);
        typeToClassMap.put("OBJECT", Object.class);
        typeToClassMap.put("STRING", String.class);
        typeToClassMap.put("BIGDECIMAL", BigDecimal.class);
        typeToClassMap.put("BIGINTEGER", BigInteger.class);
        typeToClassMap.put("BOOLEAN", Boolean.class);
        typeToClassMap.put("INTEGER", Integer.class);
        typeToClassMap.put("LONG", Long.class);
        typeToClassMap.put("FLOAT", Float.class);
        typeToClassMap.put("DOUBLE", Double.class);
        typeToClassMap.put("BYTEARRAY", Byte[].class);
        typeToClassMap.put("DATE", Date.class);
    }

    public boolean isSimple ()
    {
        // This is a simple type if it doesn't represent an TO_ONE or a TO_MANY relationship to an entity type
        return !typeName.toUpperCase().equals("OBJECT") && (elementType == null || !elementType
            .toUpperCase().equals("OBJECT"));
    }

    public List<Field> getSubFields ()
    {
        return subFields;
    }

    public void setSubFields (List<Field> subFields)
    {
        this.subFields = subFields;
    }
}
