package tools.xor.parser;

public class VariableValue implements Value
{
    private String value;

    public VariableValue(GraphQLParser.VariableContext ctx) {
        this.value = ctx.name().getText();
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
