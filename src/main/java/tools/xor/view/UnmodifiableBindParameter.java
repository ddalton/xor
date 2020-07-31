package tools.xor.view;

import java.sql.PreparedStatement;

import javax.persistence.ParameterMode;

import tools.xor.providers.jdbc.DBTranslator;

public class UnmodifiableBindParameter extends BindParameter
{

    private final BindParameter paramMap;

    public UnmodifiableBindParameter (BindParameter paramMap)
    {
        this.paramMap = paramMap;
    }

    private void raiseException ()
    {
        throw new UnsupportedOperationException(
            "Changes are not allowed on the ParameterMapping, make a copy of the view to make necessary changes.");
    }

    @Override public void setName (String name)
    {
        raiseException();
    }

    @Override public void setAttribute (String attribute)
    {
        raiseException();
    }

    @Override public void setType (String type)
    {
        raiseException();
    }

    @Override public void setDefaultValue (String defaultValue)
    {
        raiseException();
    }

    @Override public void setMode (ParameterMode mode)
    {
        raiseException();
    }

    @Override public boolean isReturnType ()
    {
        return paramMap.isReturnType();
    }

    @Override public void setReturnType (boolean value)
    {
        raiseException();
    }

    @Override public void setValue (PreparedStatement cs, DBTranslator translator, Object value)
    {
        raiseException();
    }
}
