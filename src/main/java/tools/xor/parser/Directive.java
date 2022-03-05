package tools.xor.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * http://spec.graphql.org/June2018/#sec-Language.Directives
 *
 * Directives:
 *   Directive list
 * Directive
 *   ampersand Name Arguments
 */
public class Directive
{
    private String name;
    private List<Argument> arguments;

    public Directive(GraphQLParser.DirectiveContext ctx) {
        this.name = ctx.name().getText();
        this.arguments = createArguments(ctx.arguments());
    }

    static List<Argument> createArguments(GraphQLParser.ArgumentsContext ctx) {
        List<Argument> result = new ArrayList<Argument>();

        if(ctx != null) {
            for(GraphQLParser.ArgumentContext argCtx: ctx.argument()) {
                result.add(new Argument(argCtx));
            }
        }

        return result;
    }
}
