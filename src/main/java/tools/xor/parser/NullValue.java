package tools.xor.parser;

import org.json.JSONObject;

public class NullValue implements Value
{
    public NullValue (GraphQLParser.NullValueContext ctx)
    {
    }

    @Override
    public Object getValue ()
    {
        return null;
    }

    @Override
    public Object getJSONValue ()
    {
        return JSONObject.NULL;
    }
}
