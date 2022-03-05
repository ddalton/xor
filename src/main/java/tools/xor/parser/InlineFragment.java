package tools.xor.parser;

import java.util.List;

public class InlineFragment implements Selection
{
    private String onType;
    private List<Directive> directives;
    private SelectionSet selectionSet;

    public InlineFragment (GraphQLParser.InlineFragmentContext context)
    {
        if(context.typeCondition() != null) {
            this.onType = context.typeCondition().namedType().name().getText();
        }

        if(context.directives() != null) {
            this.directives = OperationDefinition.createDirectives(context.directives());
        }

        this.selectionSet = OperationDefinition.createSelectionSet(context.selectionSet());
    }
}
