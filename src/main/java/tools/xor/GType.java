package tools.xor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.service.Shape;

/**
 * Represents a GraphQL type
 */
public class GType extends MutableJsonType implements Comparable<EntityType>
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public static final String INDENT = "   ";

    private boolean isTupleType;
    private Type ofType;
    private Type domainType;

    public GType (EntityType domainType,
                  Class<?> javaClass)
    {
        super(domainType, javaClass);

        this.domainType = domainType;
    }

    /**
     * Used to create a dynamic type to represent a map's tuple type
     * This is needed to support GraphQL's lack of support for map types.
     *
     * @param mapProperty property representing a map type in the domain model
     */
    public GType (ExtendedProperty mapProperty)
    {
        super(getTupleTypeName(mapProperty.getKeyType(), mapProperty.getElementType()),
            JSONObject.class);

        this.isTupleType = true;
    }

    public static String getTupleTypeName(Type keyType, Type elementType) {
        return String.format("%s%sTupleType", keyType.getName(), elementType.getName());
    }

    @Override
    public void setOfType(Type type) {
        this.ofType = type;
    }

    @Override
    public Property defineProperty(Property domainProperty, Shape dynamicShape, TypeMapper typeMapper) {

        Class<?> externalClass = typeMapper.toExternal(domainProperty.getType());
        if(externalClass == null) {
            throw new RuntimeException("The dynamic type is missing for the following domain class: " + domainProperty.getType().getInstanceClass().getName());
        }

        String typeName = domainProperty.getType().getName();
        if(domainProperty.getType() instanceof EntityType) {
            typeName = ((EntityType)domainProperty.getType()).getEntityName();
        }
        Type propertyType = dynamicShape.getType(typeName);
        Type elementType = null;
        if(((ExtendedProperty)domainProperty).getElementType() != null) {
            String elementTypeName = ((ExtendedProperty)domainProperty).getElementType().getName();
            if(((ExtendedProperty)domainProperty).getElementType() instanceof EntityType) {
                elementTypeName = ((EntityType)((ExtendedProperty)domainProperty).getElementType()).getEntityName();
            }
            elementType = dynamicShape.getType(elementTypeName);
        }
        if(propertyType == null) {
            Class<?> propertyClass = typeMapper.toExternal(domainProperty.getType());
            logger.debug("Name: " + domainProperty.getName() + ", Domain class: " + domainProperty.getType().getInstanceClass().getName() + ", property class: " + propertyClass.getName());
            propertyType = dynamicShape.getType(propertyClass);
        }

        MutableJsonProperty dynamicProperty = null;
        if(domainProperty.isOpenContent()) {
            dynamicProperty = new GField(domainProperty.getName(), (ExtendedProperty) domainProperty, propertyType, this, elementType);
        } else {
            dynamicProperty = new GField((ExtendedProperty) domainProperty, propertyType, this, elementType);
        }
        dynamicProperty.setDomainTypeName(domainProperty.getType().getInstanceClass().getName());
        dynamicProperty.setConverter(((ExtendedProperty)domainProperty).getConverter());

        return dynamicProperty;
    }

    @Override
    public Type ofType() { return this.ofType; }

    /**
     * GraphQL representation of the type
     * @return Type definition in GraphQL format
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(String
            .format("%s %s {", getKind() == TypeKind.INTERFACE ? "interface" : "type",
                getEntityName())).append("\n");

        for(Property p: getProperties()) {
            str.append(String.format("%s%s\n", INDENT, p.toString()));
        }

        str.append("}");

        return str.toString();
    }

    @Override
    public int compareTo (EntityType o)
    {
        return getGraphQLName().compareTo(o.getGraphQLName());
    }
}
