package tools.xor.parser;

public class BooleanValue implements Value
{
    private String value;
    private Boolean booleanValue;

    public BooleanValue(GraphQLParser.BooleanValueContext ctx) {
        this.value = ctx.getText();
        this.booleanValue = Boolean.parseBoolean(this.value);
    }

    @Override
    public Object getValue ()
    {
        return this.booleanValue;
    }

    @Override
    public Object getJSONValue ()
    {
        return getValue();
    }
}
