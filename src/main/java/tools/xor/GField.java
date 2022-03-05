package tools.xor;

import org.json.JSONArray;

/**
 * Represents a GraphQL field
 */
public class GField extends MutableJsonProperty
{
    private Type domainType;

    public GField (ExtendedProperty domainProperty,
                   Type type,
                   ExternalType parentType,
                   Type elementType)
    {
        super(domainProperty, type, parentType, elementType);

        setDomainType(domainProperty);
    }

    /**
     * Needed to support dynamic entry properties.
     * For example: key and value properties of a map
     *
     * Null values should not be allowed on the list holding the map tuple type
     *
     * @param name of the dynamic property
     * @param domainProperty null for map tuple properties
     * @param type of the property
     * @param parentType map tuple type
     * @param elementType null for map tuple properties
     */
    public GField (String name,
                   ExtendedProperty domainProperty,
                   Type type,
                   ExternalType parentType,
                   Type elementType)
    {
        super(name, domainProperty, type, parentType, elementType);

        setDomainType(domainProperty);
    }

    private void setDomainType(ExtendedProperty domainProperty) {
        this.domainType = domainProperty.getType();

        // http://spec.graphql.org/June2018/#sec-ID
        // ID type is always serialized as a string
        if(domainProperty.isIdentifier()) {
            setType(new IDType(String.class));
        }
    }

    /**
     * GraphQL representation of the type
     * @return Type definition in GraphQL format
     */
    @Override
    public String toString ()
    {
        StringBuilder str = new StringBuilder(
            String.format("%s%s: %s", getName(), getArgString(), getTypeString()));

        return str.toString();
    }

    private String getArgString() {
        StringBuilder str = new StringBuilder();

        if(getArguments().size() > 0) {
            str.append("(");
            StringBuilder argStr = new StringBuilder();
            for(InputValue arg: getArguments()) {
                if(argStr.length() > 0) {
                    argStr.append(", ");
                    argStr.append(arg.toString());
                }
            }
            str.append(argStr).append(")");
        }

        return str.toString();
    }

    private String getTypeString() {
        return String.format("%s%s", getType().getGraphQLName(), isNullable() ? "" : "!");
    }
}
