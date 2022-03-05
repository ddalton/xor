package tools.xor;

import org.json.JSONArray;
import org.json.JSONObject;
import tools.xor.service.DataModel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphQLTypeMapper extends MutableJsonTypeMapper
{
    private static final Set<Class<?>> unchanged = new HashSet<Class<?>>();
    private static final Set<String> unchangedNames = new HashSet<>();
    private static final Map<String, Class<?>> mappedScalars = new HashMap<>();

    static {
        mappedScalars.put(java.util.Date.class.getName(), String.class);
        mappedScalars.put(Void.class.getName(), String.class);
        mappedScalars.put(Character.class.getName(), String.class);
        mappedScalars.put(Byte.class.getName(), Integer.class);
        mappedScalars.put(Short.class.getName(), Integer.class);
        mappedScalars.put(Long.class.getName(), String.class);
        mappedScalars.put(Float.class.getName(), Double.class);
        mappedScalars.put(BigDecimal.class.getName(), String.class);
        mappedScalars.put(BigInteger.class.getName(), String.class);

        // primitives
        mappedScalars.put(char.class.getName(), String.class);
        mappedScalars.put(byte.class.getName(), Integer.class);
        mappedScalars.put(short.class.getName(), Integer.class);
        mappedScalars.put(long.class.getName(), String.class);
        mappedScalars.put(float.class.getName(), Double.class);

        // unchanged
        unchanged.add(String.class);
        unchanged.add(Boolean.class);
        unchanged.add(Integer.class);
        unchanged.add(Double.class);

        // primitives
        unchanged.add(boolean.class);
        unchanged.add(int.class);
        unchanged.add(double.class);

        for(Class<?> clazz: unchanged) {
            unchangedNames.add(clazz.getName());
        }
    }

    public static synchronized void addMappedScalar(Class fromClazz, Class toClazz) {
        if(!unchanged.contains(toClazz.getName())) {
            throw new RuntimeException("Mapping to an non-GraphQL scalar is not supported");
        }

        mappedScalars.put(fromClazz.getName(), toClazz);
    }

    public GraphQLTypeMapper() {
        super();
    }

    public GraphQLTypeMapper (DataModel das,
                              MapperSide side,
                              String shapeName,
                              boolean persistenceManaged)
    {
        super(das, side, shapeName, persistenceManaged);
    }

    public static Set<Class<?>> getUnchanged() {
        return unchanged;
    }

    @Override
    /**
     * Handle the interpretation of returning the following classes:
     *
     * JsonObject
     * JsonArray
     * JsonNumber
     * JsonString
     * JsonValue.TRUE
     * JsonValue.FALSE
     * JsonValue.NULL
     */
    public Class<?> toExternal(Type type) {

        Class<?> domainClass = type == null ? null : type.getInstanceClass();

        // domainClass can be null for an open type
        if(domainClass != null) {
            if (getUnchanged().contains(domainClass)) {
                return domainClass;
            }

            if (mappedScalars.containsKey(domainClass.getName())) {
                return mappedScalars.get(domainClass.getName());
            }

            if (Set.class.isAssignableFrom(domainClass) ||
                List.class.isAssignableFrom(domainClass) ||
                domainClass.isArray()) {
                if(domainClass == byte[].class) {
                    // Should encode byte[] to string. For example: Base64
                    return String.class;
                } else {
                    return JSONArray.class;
                }
            }

            // The default type for any scalar is String
            if(SimpleTypeFactory.isScalar(domainClass)) {
                return String.class;
            }
        }

        return JSONObject.class;
    }

    @Override
    /**
     * Handle the interpretation of returning the following classes:
     *
     * JsonObject
     * JsonArray
     * JsonNumber
     * JsonString
     * JsonValue.TRUE
     * JsonValue.FALSE
     * JsonValue.NULL
     */
    public String toExternal(String typeName) {

        if(typeName != null) {
            if (unchangedNames.contains(typeName)) {
                return typeName;
            }

            if (mappedScalars.containsKey(typeName)) {
                return mappedScalars.get(typeName).getName();
            }

            Class<?> domainClass;
            try {
                domainClass = Class.forName(typeName);
                if(Collection.class.isAssignableFrom(domainClass)) {
                    return JSONArray.class.getName();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return JSONObject.class.getName();
    }

    @Override
    public boolean isExternal(String typeName) {
        return unchangedNames.contains(typeName) || JSONObject.class.getName().equals(typeName) || JSONArray.class.getName().equals(typeName);
    }

    @Override
    public ExternalType createExternalType(EntityType domainType, Class<?> derivedClass) {
        return new GType(domainType, derivedClass);
    }

    @Override
    protected TypeMapper createInstance(DataModel das, MapperSide side, String shapeName, boolean persistenceManaged) {
        return new GraphQLTypeMapper(das, side, shapeName, persistenceManaged);
    }
}
