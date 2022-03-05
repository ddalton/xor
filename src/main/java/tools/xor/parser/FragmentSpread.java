package tools.xor.parser;

public class FragmentSpread implements Selection
{
    private String name;

    public FragmentSpread (GraphQLParser.FragmentSpreadContext context)
    {
        this.name = context.fragmentName().name().getText();
    }
}
