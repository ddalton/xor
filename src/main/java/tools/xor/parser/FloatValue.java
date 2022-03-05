package tools.xor.parser;

import org.json.JSONObject;

/**
 * Based on the spec there is no limitation on the size of this value
 * So it needs to be interpreted based on the target field's type
 */
public class FloatValue implements Value
{
    private Number value;

    public FloatValue (GraphQLParser.FloatValueContext ctx)
    {
        this.value = (Number) JSONObject.stringToValue(ctx.getText());
    }

    @Override
    public Number getValue ()
    {
        return this.value;
    }

    @Override
    public Number getJSONValue ()
    {
        return this.value;
    }
}
