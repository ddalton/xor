package tools.xor.service;

import tools.xor.GType;
import tools.xor.Type;
import tools.xor.parser.GraphQLRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphQLService
{
    private Shape shape;

    public GraphQLService(Shape shape) {
        this.shape = shape;
    }

    /**
     * Return the schema in SDL format
     * @return schema string
     */
    public String getSchema() {
        List<GType> entityTypes = new ArrayList<GType>();
        for(Type type: shape.getUniqueTypes()) {
            if(type instanceof GType) {
                entityTypes.add((GType) type);
            }
        }
        Collections.sort(entityTypes);

        StringBuilder schema = new StringBuilder(addSchemaHeader()).append("\n");
        for(Type t: entityTypes) {
            schema.append(t.toString()).append("\n");
        }

        return schema.toString();
    }

    private String addSchemaHeader() {
        return String.format("schema {\n%squery: Query\n}", GType.INDENT);
    }

    public Object executeRequest(String document) {
        GraphQLRequest request = new GraphQLRequest(this.shape);
        request.execute(document);

        return request.toString();
    }
}
