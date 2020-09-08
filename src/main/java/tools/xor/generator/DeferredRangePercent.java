package tools.xor.generator;

import tools.xor.IteratorListener;
import tools.xor.util.graph.StateGraph;

public class DeferredRangePercent extends RangePercent implements IteratorListener
{
    public DeferredRangePercent (String[] arguments)
    {
        super(arguments);
    }

    @Override public void handleEvent (int sourceId, StateGraph.ObjectGenerationVisitor visitor)
    {
        // Initialize the next value
        super.getValue();
    }

    @Override protected Long getValue() {
        return this.value;
    }
}
