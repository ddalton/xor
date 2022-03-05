package tools.xor.parser;

public class StringValue implements Value
{
    private String value;

    public StringValue (GraphQLParser.StringValueContext ctx)
    {
        this.value = ctx.getText();
    }

    @Override
    public Object getValue ()
    {
        return this.value;
    }

    @Override
    public String getJSONValue ()
    {
        return this.value;
    }
}
