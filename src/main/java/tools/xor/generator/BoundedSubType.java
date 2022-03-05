package tools.xor.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Type;
import tools.xor.util.GraphUtil;
import tools.xor.util.graph.StateGraph;

public class BoundedSubType extends DefaultGenerator
{
    List<EntityType> boundedSubTypes;

    public BoundedSubType (String[] arguments)
    {
        super(arguments);
    }

    @Override public void validate (ExtendedProperty property)
    {
        Type entityType = GraphUtil.getPropertyEntityType(property, null);
        SubTypeChoices.checkValidity(entityType, getValues());

        if(getValues().length != 1) {
            throw new IllegalArgumentException("The generator requires 1 argument representing the bounded subtype classname");
        }

        Map<String, EntityType> subTypes = new HashMap<String, EntityType>();
        for(EntityType type: ((EntityType)entityType).getSubtypes()) {
            subTypes.put(type.getInstanceClass().getName(), type);
        }

        EntityType boundedSubType = subTypes.get(getValues()[0]);
        boundedSubTypes = new ArrayList<>(boundedSubType.getSubtypes());
        boundedSubTypes.add(boundedSubType);
    }

    @Override public EntityType getSubType (EntityType entityType, StateGraph stateGraph)
    {
        return getSubType(boundedSubTypes, stateGraph);
    }

    @Override public boolean isApplicableToCollectionElement ()
    {
        return true;
    }
}
