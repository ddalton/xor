package tools.xor.parser;

import java.math.BigInteger;

/**
 * Could represent a 32 bit int or a bigger int value.
 * This will be validated based on the type of the target field
 */
public class IntValue implements Value
{
    private String value;
    private BigInteger intValue;

    public IntValue(GraphQLParser.IntValueContext ctx) {
        this.value = ctx.getText();
        this.intValue = new BigInteger(this.value);
    }

    @Override
    public Object getValue ()
    {
        return this.intValue;
    }

    @Override
    public Object getJSONValue ()
    {
        try {
            // If it can be converted to a integer we return an int
            // else we return the string
            return Integer.parseInt(this.value);
        }
        catch (NumberFormatException e) {
            return this.value;
        }
    }
}
