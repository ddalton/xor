package tools.xor.parser;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class ListValue implements Value
{
    private String value;
    private List<Value> values;

    public ListValue (GraphQLParser.ListValueContext ctx)
    {
        this.value = ctx.getText();
        this.values = new ArrayList<>(ctx.value().size());
        for(GraphQLParser.ValueContext valueCtx: ctx.value()) {
            this.values.add(ValueHelper.createValue(valueCtx));
        }
    }

    @Override
    public Object getValue ()
    {
        return this.values;
    }

    @Override
    public Object getJSONValue ()
    {
        JSONArray array = new JSONArray();
        for(Value value: values) {
            array.put(value.getJSONValue());
        }

        return array;
    }
}
