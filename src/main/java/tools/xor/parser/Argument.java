package tools.xor.parser;

public class Argument
{
    private String name;
    private Value value;

    public Argument(GraphQLParser.ArgumentContext argCtx) {
        this.name = argCtx.name().getText();
        this.value = ValueHelper.createValue(argCtx.value());
    }
}
