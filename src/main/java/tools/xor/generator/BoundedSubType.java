package tools.xor.generator;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoundedSubType extends DefaultGenerator
{
    List<EntityType> boundedSubTypes;

    public BoundedSubType (String[] arguments)
    {
        super(arguments);
    }

    @Override public void validate (ExtendedProperty property)
    {
        SubTypeChoices.checkValidity(property.getType(), getValues());

        if(getValues().length != 1) {
            throw new IllegalArgumentException("The generator requires 1 argument representing the bounded subtype classname");
        }

        Map<String, EntityType> subTypes = new HashMap<String, EntityType>();
        for(EntityType type: ((EntityType)property.getType()).getSubtypes()) {
            subTypes.put(type.getInstanceClass().getName(), type);
        }

        EntityType boundedSubType = subTypes.get(getValues()[0]);
        boundedSubTypes = new ArrayList<>(boundedSubType.getSubtypes());
        boundedSubTypes.add(boundedSubType);
    }

    @Override public EntityType getSubType (EntityType entityType)
    {
        if(boundedSubTypes.size() == 1) {
            return boundedSubTypes.get(0);
        }

        int index =  (int) (Math.random() * (boundedSubTypes.size()+1));
        if(index == boundedSubTypes.size()) {
            index--;
        }

        return boundedSubTypes.get(index);
    }

    @Override public boolean isApplicableToCollectionElement ()
    {
        return true;
    }
}
