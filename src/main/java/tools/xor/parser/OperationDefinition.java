package tools.xor.parser;

import tools.xor.service.Shape;
import java.util.ArrayList;
import java.util.List;

public class OperationDefinition
{
    // We only support QUERY and MUTATION
    private OperationType type;
    private String name;
    private List<Variable> variables;
    private List<Directive> directives;
    private SelectionSet selectionSet;
    private Shape shape;

    public OperationDefinition (GraphQLParser.OperationDefinitionContext ctx, Shape shape)
    {
        this.shape = shape;

        initOperationType(ctx);

        if(ctx.name() != null) {
            this.name = ctx.name().getText();
        }

        initVariableDefinitions(ctx.variableDefinitions());
        this.directives = createDirectives(ctx.directives());
        this.selectionSet = createSelectionSet(ctx.selectionSet());
    }

    private void initOperationType (GraphQLParser.OperationDefinitionContext ctx)
    {
        // http://spec.graphql.org/June2018/#sec-Language.Operations
        // Query shorthand
        if (ctx.operationType() == null) {
            type = OperationType.QUERY;
        }
        else {
            switch (ctx.operationType().getText()) {
            case "query":
                type = OperationType.QUERY;
                break;
            case "mutation":
                type = OperationType.MUTATION;
                break;
            case "subscription":
            default:
                throw new RuntimeException(
                    String.format("Unknown operation type: %s", ctx.operationType().getText()));
            }
        }
    }

    private void initVariableDefinitions(GraphQLParser.VariableDefinitionsContext context) {
        this.variables = new ArrayList<Variable>();

        if(context != null) {
            for (GraphQLParser.VariableDefinitionContext ctx : context.variableDefinition()) {
                this.variables.add(new Variable(ctx, this.shape));
            }
        }
    }

    static List<Directive> createDirectives(GraphQLParser.DirectivesContext context) {
        List<Directive> result = new ArrayList<Directive>();

        if(context != null) {
            for (GraphQLParser.DirectiveContext ctx : context.directive()) {
                result.add(new Directive(ctx));
            }
        }

        return result;
    }

    static SelectionSet createSelectionSet(GraphQLParser.SelectionSetContext context) {
        if(context == null) {
            return null;
        }

        return new SelectionSet(context);
    }
}
