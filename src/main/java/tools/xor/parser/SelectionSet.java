package tools.xor.parser;

import java.util.ArrayList;
import java.util.List;

public class SelectionSet
{
    List<Selection> selections;

    public SelectionSet (GraphQLParser.SelectionSetContext context) {
        this.selections = new ArrayList<Selection>();

        if(context != null) {
            for (GraphQLParser.SelectionContext ctx : context.selection()) {
                this.selections.add(createSelection(ctx));
            }
        }
    }

    private static Selection createSelection(GraphQLParser.SelectionContext ctx) {
        if(ctx.field() != null) {
            return new Field(ctx.field());
        } else if(ctx.fragmentSpread() != null) {
            return new FragmentSpread(ctx.fragmentSpread());
        } else if(ctx.inlineFragment() != null) {
            return new InlineFragment(ctx.inlineFragment());
        } else {
            throw new RuntimeException("A valid selection is not provided. Please refer http://spec.graphql.org/June2018/#Selection");
        }
    }
}
