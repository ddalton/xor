package tools.xor.parser;

public class ValueHelper
{
    public static Value createValue (GraphQLParser.ValueContext ctx) {
        if(ctx.variable() != null) {
            return new VariableValue(ctx.variable());
        }
        else if(ctx.intValue() != null) {
            return new IntValue(ctx.intValue());
        }
        else if(ctx.floatValue() != null) {
            return new FloatValue(ctx.floatValue());
        }
        else if(ctx.stringValue() != null) {
            return new StringValue(ctx.stringValue());
        }
        else if(ctx.booleanValue() != null) {
            return new BooleanValue(ctx.booleanValue());
        }
        else if(ctx.nullValue() != null) {
            return new NullValue(ctx.nullValue());
        }
        else if(ctx.enumValue() != null) {
            return new EnumValue(ctx.enumValue());
        }
        else if(ctx.listValue() != null) {
            return new ListValue(ctx.listValue());
        }
        else if(ctx.objectValue() != null) {
            return new ObjectValue(ctx.objectValue());
        } else {
            throw new RuntimeException(String.format("Unknown value passed: %s", ctx.getText()));
        }
    }
}
