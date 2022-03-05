package tools.xor.parser;

public class EnumValue implements Value
{
    private String enumValue;

    public EnumValue (GraphQLParser.EnumValueContext ctx)
    {
        this.enumValue = ctx.getText();
    }

    @Override
    public Object getValue ()
    {
        return this.enumValue;
    }

    @Override
    public Object getJSONValue ()
    {
        return this.enumValue;
    }
}
