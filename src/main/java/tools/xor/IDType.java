package tools.xor;

public class IDType extends StringType
{
    public IDType (Class<?> clazz)
    {
        super(clazz);
    }

    @Override
    public String getGraphQLName () {
        return Scalar.ID;
    }
}
