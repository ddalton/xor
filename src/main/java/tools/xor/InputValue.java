package tools.xor;

/**
 * Used by GraphQL to represent an InputValue field
 */
public class InputValue implements Property
{
    private String name;
    private SimpleType type;
    private String defaultValue;
    private boolean nullable;

    /*
     * see http://spec.graphql.org/June2018/#InputObjectTypeDefinition
     * A separate property type is being created since Object and Interface types are not
     * allowed for input values.
     *
     * Also arguments are not allowed on InputValue fields
     */
    public InputValue(String name, SimpleType type) {
        this.name = name;
        this.type = type;
    }

    private void setDefaultValue(String value) {
        this.defaultValue = value;
    }

    @Override
    public String getName ()
    {
        return name;
    }

    @Override
    public Type getType ()
    {
        return type;
    }

    @Override
    public boolean isMany ()
    {
        return type instanceof ListType;
    }

    @Override
    public Object getDefault ()
    {
        return this.defaultValue;
    }

    @Override
    public boolean isNullable ()
    {
        return this.nullable;
    }

    public void setNullable(Boolean value) {
        this.nullable = value;
    }

    private String getDefaultValue() {
        return defaultValue == null ? "" : String.format(" = %s", defaultValue);
    }

    @Override
    public String toString() {
        return String.format("%s: %s%s", getName(), getType().getGraphQLName(), getDefaultValue());
    }
}
