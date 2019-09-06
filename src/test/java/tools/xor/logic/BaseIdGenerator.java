package tools.xor.logic;

import tools.xor.CollectionOwnerGenerator;
import tools.xor.Property;
import tools.xor.generator.DefaultGenerator;
import tools.xor.util.graph.StateGraph;

/*
 * A generator for BaseId values
 */
public class BaseIdGenerator extends DefaultGenerator
{
    private static final int Base64 = 64;
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

    static {
        for(int i = 15; i > 8; i--) {
            VALUE_INIT[i] = 'A';
        }
    }

    // A sampling of type names
    private static final String[] types = {
        "AC!GAEL",
        "AC2+AEL",
        "AC4iAEL",
        "AC8wAEL",
        "AC92AEL",
        "ACkOAEL",
        "ACkYAEL",
        "ACnWAEL",
        "ACoSAEL",
        "ACqeAEL",
        "ACqoAEL",
        "ACsCAEL",
        "ACtmAEL",
        "ACtwAEL",
        "ACvKAEL",
        "ACwuAEL",
        "ACySAEL",
        "ACz2AEL",
        "AD!KAEL",
        "AD1oAEL",
        "AD24AEL",
        "AD4IAEL",
        "AD5YAEL",
        "AD74AEL",
        "AD8WAEL",
        "ADa6AEL",
        "ADeMAEL",
        "ADEuAEL",
        "ADF+AEL",
        "ADfSAEL",
        "ADgYAEL",
        "ADheAEL",
        "ADIeAEL",
        "ADikAEL",
        "ADjgAEL",
        "ADJuAEL",
        "ADK+AEL",
        "ADlYAEL",
        "ADMOAEL",
        "ADmoAEL",
        "ADnQAEL",
        "ADtCAEL",
        "ADU+AEL",
        "ADUCAEL",
        "ADWEAEL",
        "ADWOAEL",
        "ADXeAEL",
        "ADxQAEL",
        "ADxuAEL",
        "ADYkAEL",
        "ADzIAEL",
        "AEa0AEL",
        "AEB0AEL",
        "AECIAEL",
        "AEcYAEL",
        "AEd8AEL",
        "AEDYAEL",
        "AEFuAEL",
        "AEG0AEL",
        "AEhEAEL",
        "AEHwAEL",
        "AEioAEL",
        "AEIsAEL",
        "AEJoAEL",
        "AElwAEL",
        "AEMwAEL",
        "AEOUAEL",
        "AEP4AEL",
        "AEpMAEL",
        "AEqmAEL",
        "AETAAEL",
        "AEtaAEL",
        "AEu0AEL",
        "AEUkAEL",
        "AEv6AEL",
        "AEWIAEL",
        "AExAAEL",
        "AEZQAEL",
        "AG2cAEL",
        "AG3EAEL",
        "AHL2AEL",
        "AHMKAEL",
        "AHQOAEL",
        "AHT0AEL",
        "AG6+AEL"
    };

    private CollectionOwnerGenerator type;
    private CollectionOwnerGenerator value;

    public static String toBase64 (long num)
    {
        assert num >= 0 : "the long passed into MathUtil.toBase64 must be positive.";

        // Long.MAX_VALUE can more than fit in array of size 16.
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

    public BaseIdGenerator (String[] arguments, CollectionOwnerGenerator type, CollectionOwnerGenerator value)
    {
        super(arguments);

        this.type = type;
        this.value = value;
    }
}
