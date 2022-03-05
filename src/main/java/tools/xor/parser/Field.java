package tools.xor.parser;

import java.util.List;

public class Field implements Selection
{
    private String alias;
    private String name;
    private List<Argument> arguments;
    private List<Directive> directives;
    private SelectionSet selectionSet;

    public Field(GraphQLParser.FieldContext ctx) {
        if(ctx.alias() != null) {
            alias = ctx.alias().name().getText();
        }

        // If name is null then it represent the 'type' value
        if(ctx.name() == null) {
            this.name = "type";
        } else {
            this.name = ctx.name().getText();
        }

        this.arguments = Directive.createArguments(ctx.arguments());
        this.directives = OperationDefinition.createDirectives(ctx.directives());
        this.selectionSet = OperationDefinition.createSelectionSet(ctx.selectionSet());
    }
}
