package tools.xor.parser;

import tools.xor.Type;
import tools.xor.service.Shape;
import java.util.List;

public class FragmentDefinition
{
    private String name;
    private Type type;
    private List<Directive> directives;
    private SelectionSet selectionSet;
    private Shape shape;

    public FragmentDefinition(GraphQLParser.FragmentDefinitionContext context, Shape shape) {
        this.shape = shape;

        this.name = context.fragmentName().name().getText();
        this.type = shape.getType(context.typeCondition().namedType().name().getText());

        this.directives = OperationDefinition.createDirectives(context.directives());
        this.selectionSet = OperationDefinition.createSelectionSet(context.selectionSet());
    }
}
