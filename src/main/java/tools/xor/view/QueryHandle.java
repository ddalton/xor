package tools.xor.view;

import java.util.List;

import tools.xor.service.DataStore;

public class QueryHandle
{
    private final String queryString;
    private final DataStore.QueryType queryType;
    private final Object queryInput;
    private List<String> columns;
    private List<BindParameter> bindParams;

    public QueryHandle(String queryString, DataStore.QueryType queryType, Object queryInput) {
        this.queryString = queryString;
        this.queryType = queryType;
        this.queryInput = queryInput;
    }

    public String getQueryString() {
        return this.queryString;
    }

    public DataStore.QueryType getQueryType() {
        return this.queryType;
    }

    public Object getQueryInput() {
        return this.queryInput;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<String> getColumns() {
        return this.columns;
    }

    public void setBindParams(List<BindParameter> params) {
        this.bindParams = params;
    }

    public List<BindParameter> getBindParams() {
        return this.bindParams;
    }   

    public Query create(DataStore po) {
        Query query = po.getQuery(queryString, queryType, queryInput);

        if(columns != null) {
            query.setColumns(columns);
        }

        if(bindParams != null) {
            query.updateParamMap(bindParams);
        }

        return query;
    }
}
