package tools.xor.service;

import org.json.JSONObject;
import tools.xor.Settings;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.AbstractQuery;
import tools.xor.view.Query;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public abstract class AbstractEntityScroll implements EntityScroll
{
    protected abstract Settings getSettings();
    protected abstract Connection getConnection();
    protected abstract String getSQLString();
    protected abstract Query getQuery();

    private Statement statement;
    private ResultSet rs;

    @Override public boolean hasNext ()
    {
        try {
            if (this.statement == null) {
                this.statement =
                    getConnection().createStatement(
                        ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                this.statement.setFetchSize(getSettings().getBatchSize());

                this.rs = this.statement.executeQuery(getSQLString());
            }

            return this.rs.isAfterLast();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override public JSONObject next ()
    {
        try {
            boolean result = this.rs.next();

            if(!result) {
                return null;
            } else {
                return createJson();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override public void close () throws IOException
    {
        try {
            if (rs != null)
                rs.close();
        }
        catch (Exception e) {}
        ;
        try {
            if (this.statement != null)
                this.statement.close();
        }
        catch (Exception e) {}
        ;
    }

    private JSONObject createJson() {
        JSONObject result = new JSONObject();

        try {
            List values = AbstractQuery.extractResults(rs);

            // set the values in the JSONObject
            for(int i = 0; i < values.size(); i++) {
                StateGraph.setKeyValue(result, getQuery().getColumns().get(i), values.get(i));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
}
