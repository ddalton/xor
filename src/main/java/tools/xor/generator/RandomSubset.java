package tools.xor.generator;

import java.util.concurrent.ThreadLocalRandom;

import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.util.graph.StateGraph;

/**
 * We directly have to access the values array since
 * we dynamically build the values and the dynamically built values are accessed using
 * the overridden getValues() function
 */
public class RandomSubset extends FixedSet
{
    String dynamicValues[];

    public RandomSubset (String[] arguments)
    {
        super(arguments);
    }

    @Override public int getFanout (Property property, Settings settings, String path, StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(values[0]).intValue();
    }

    @Override
    public String[] getValues ()
    {
        return dynamicValues;
    }

    /**
     * The generator is parameterized as follows:
     * 1. Fan value
     * 2. Range start
     * 3. Range end
     * 4. Optional String template
     *
     * @param visitor context
     */
    @Override
    public void init (StateGraph.ObjectGenerationVisitor visitor) {

        int start = Double.valueOf(values[1]).intValue();
        int end = Double.valueOf(values[2]).intValue();

        // Generate the subset
        int fanOut = getFanout(null, null, null, null);
        int[] intValues = ThreadLocalRandom.current().ints(start, end).distinct().limit(fanOut).toArray();

        // Build the dynamic values
        // Check if there is a template
        if(values.length == 4) {
            String template = values[3];

            dynamicValues = new String[fanOut];
            StringTemplate stringTemplate = new StringTemplate(new String[] {template});
            for(int i = 0; i < fanOut; i++) {
                visitor.setContext(new Integer(intValues[i]).toString());
                dynamicValues[i] = stringTemplate.resolve(template, null, visitor);
            }
        } else {
            for(int i = 0; i < fanOut; i++) {
                dynamicValues[i] = new Integer(intValues[i]).toString();
            }
        }
    }
}
