package tools.xor.logic;

import tools.xor.CollectionOwnerGenerator;
import tools.xor.Property;
import tools.xor.generator.DefaultGenerator;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

/*
 * A generator for values encoded in Base64
 */
public class Base64Generator extends DefaultGenerator
{
    private static final int Base64 = 64;
    private static final int SampleCount = 1000;
    private static final char base64ConvertTable[] =
    {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '+', '!'
    };

    private static final char[] VALUE_INIT = new char[16];
    
    // A sampling of type names
    private static final String[] types = new String[SampleCount];
    
    static {
        for(int i = 15; i > 8; i--) {
            VALUE_INIT[i] = 'A';
        }
        
        int offset = 1000000;
        int interval = 1000000;
        for(int i = 0; i < SampleCount; i++) {
            types[i] = toBase64(offset + (long)(interval*ClassUtil.nextDouble()));
        }
    }

    private CollectionOwnerGenerator type;
    private CollectionOwnerGenerator value;

    public static String toBase64 (long num)
    {
        assert num >= 0 : "the long passed into toBase64 must be positive.";

        char[] buffer = new char[16];
        int offset = toBase64(num, buffer, 15);

        // Pad with 'A' character
        return "AAAAAAA".substring(15-offset) + new String(buffer, offset + 1, 15 - offset);
    }

    public static int toBase64 (long num, char[] chr, int off)
    {
        if (num == 0) {
            chr[off] = 'A';
            off--;
        }
        else {
            for (; num > 0; off--) {
                chr[off] = base64ConvertTable[(int)(num % 64)];
                num = num / Base64;
            }
        }
        return off;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return (type != null ? types[type.getIntValue(visitor)] : "AAAAAAA") +
            ((value != null) ? toBase64(value.getIntValue(visitor)) : toBase64(type.getInvocationCount()));
    }

    public Base64Generator (String[] arguments, CollectionOwnerGenerator type, CollectionOwnerGenerator value)
    {
        super(arguments);

        this.type = type;
        this.value = value;
    }
}
