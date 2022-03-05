package tools.xor.parser;

public interface Value
{
    /**
     * Provides the Java representation of the input value
     * @return Java representation
     */
    Object getValue();

    /**
     * Useful for representing the input value in JSON representation
     * @return JSON representation
     */
    Object getJSONValue ();
}
