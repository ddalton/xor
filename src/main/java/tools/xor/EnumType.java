package tools.xor;

import org.json.JSONObject;
import tools.xor.generator.Generator;
import tools.xor.util.graph.StateGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnumType extends SimpleType
{
    private List<String> values;
    private Set<String> uniqueValues;
    private String name;

    public EnumType (Class<?> clazz)
    {
        super(clazz);
    }

    public EnumType(String name) {
        super(null);

        this.name = name;
    }

    public void setValues(Set<String> values) {
        this.uniqueValues = new HashSet<>();
        this.uniqueValues.addAll(values);
        this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    @Override
    public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
                           StateGraph.ObjectGenerationVisitor visitor) {

        Generator gen = property.getGenerator(visitor.getRelationshipName());

        if(gen != null) {
            String value = gen.getStringValue(property, visitor);
            if(this.uniqueValues.contains(value)) {
                return value;
            } else {
                throw new RuntimeException(String.format("Generated value is not one of the enum values: %s", value));
            }
        } else {
            if(Enum.class.isAssignableFrom(getInstanceClass())) {
                Class<Enum> enumClazz = (Class<Enum>)getInstanceClass();
                Enum[] constants = enumClazz.getEnumConstants();
                return constants[(int)(Math.random()*constants.length)];
            } else {
                return values.get((int)(Math.random()*this.values.size()));
            }
        }
    }

    @Override
    public TypeKind getKind() { return TypeKind.ENUM; }

    @Override
    public String getGraphQLName () {
        return name == null ? getInstanceClass().getSimpleName() : name;
    }
}
