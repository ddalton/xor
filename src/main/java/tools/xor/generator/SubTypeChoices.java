package tools.xor.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Type;
import tools.xor.util.ClassUtil;
import tools.xor.util.GraphUtil;
import tools.xor.util.graph.StateGraph;

public class SubTypeChoices extends DefaultGenerator
{
    List<EntityType> subTypeList = new ArrayList<EntityType>();

    public SubTypeChoices (String[] arguments)
    {
        super(arguments);
    }

    public static void checkValidity(Type root, String[] values) {
        if( !(root instanceof EntityType) ) {
            throw new IllegalStateException("This generator can only be configured on a property referencing standalone entity or entities. Type: " + root.getName());
        }

        if(values.length < 1) {
            throw new IllegalArgumentException("The generator requires at least a single input argument");
        }

        Map<String, EntityType> subTypes = new HashMap<String, EntityType>();
        for(EntityType type: ((EntityType)root).getSubtypes()) {
            subTypes.put(type.getInstanceClass().getName(), type);
        }
        subTypes.put(root.getInstanceClass().getName(), (EntityType)root);

        for(String value: values) {
            if(!subTypes.containsKey(value)) {
                throw new IllegalArgumentException("The configured bounded subtype value of " + value + " is not a subtype of " + root.getName());
            }
        }
    }

    @Override public void validate (ExtendedProperty property)
    {
        Type entityType = GraphUtil.getPropertyEntityType(property, null);
        checkValidity(entityType, getValues());

        Map<String, EntityType> subTypes = new HashMap<String, EntityType>();
        for(EntityType type: ((EntityType)entityType).getSubtypes()) {
            subTypes.put(type.getInstanceClass().getName(), type);
        }
        subTypes.put(entityType.getInstanceClass().getName(), (EntityType)entityType);

        for(String value: getValues()) {
            EntityType type = subTypes.get(value);
            if(type != null) {
                subTypeList.add(subTypes.get(value));
            } else {
                throw new RuntimeException("Unable to find type for generator value: " + value);
            }
        }
    }

    @Override public EntityType getSubType (EntityType entityType, StateGraph stateGraph)
    {
        if(subTypeList.size() == 1) {
            subTypeList.get(0);
        }

        int index =  (int) (ClassUtil.nextDouble() * subTypeList.size());
        if(index == subTypeList.size()) {
            index--;
        }

        return subTypeList.get(index);
    }

    @Override public boolean isApplicableToCollectionElement ()
    {
        return true;
    }
}
