package tools.xor.parser;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class ObjectValue implements Value
{
    private Map<String, Value> objectValue;

    public ObjectValue(GraphQLParser.ObjectValueContext ctx) {
        objectValue = new HashMap<>();

        for(GraphQLParser.ObjectFieldContext objectCtx: ctx.objectField()) {
            objectValue.put(objectCtx.name().getText(), ValueHelper.createValue(objectCtx.value()));
        }
    }

    @Override
    public Object getValue ()
    {
        return objectValue;
    }

    @Override
    public Object getJSONValue ()
    {
        JSONObject json = new JSONObject();

        for(Map.Entry<String, Value> entry: objectValue.entrySet()) {
            json.put(entry.getKey(), entry.getValue().getJSONValue());
        }

        return json;
    }
}
