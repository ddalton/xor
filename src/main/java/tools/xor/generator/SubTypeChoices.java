package tools.xor.generator;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubTypeChoices extends DefaultGenerator
{
    List<EntityType> subTypeList = new ArrayList<EntityType>();

    public SubTypeChoices (String[] arguments)
    {
        super(arguments);
    }

    public static void checkValidity(Type root, String[] values) {
        if( !(root instanceof EntityType) ) {
            throw new IllegalStateException("This generator can only be configured on a property referencing standalone entity or entities");
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
        checkValidity(property.getType(), getValues());

        Map<String, EntityType> subTypes = new HashMap<String, EntityType>();
        for(EntityType type: ((EntityType)property.getType()).getSubtypes()) {
            subTypes.put(type.getInstanceClass().getName(), type);
        }
        subTypes.put(property.getType().getInstanceClass().getName(), (EntityType)property.getType());

        for(String value: getValues()) {
            EntityType type = subTypes.get(value);
            if(type != null) {
                subTypeList.add(subTypes.get(value));
            } else {
                throw new RuntimeException("Unable to find type for generator value: " + value);
            }
        }
    }

    @Override public EntityType getSubType (EntityType entityType)
    {
        if(subTypeList.size() == 1) {
            subTypeList.get(0);
        }

        int index =  (int) (Math.random() * (subTypeList.size()+1));
        if(index == subTypeList.size()) {
            index--;
        }

        return subTypeList.get(index);
    }
}
