package tools.xor.view;

import tools.xor.service.PersistenceOrchestrator;

import java.util.List;

public class QueryHandle
{
    private final String queryString;
    private final PersistenceOrchestrator.QueryType queryType;
    private final Object queryInput;
    private List<String> columns;
    private List<BindParameter> params;

    public QueryHandle(String queryString, PersistenceOrchestrator.QueryType queryType, Object queryInput) {
        this.queryString = queryString;
        this.queryType = queryType;
        this.queryInput = queryInput;
    }

    public String getQueryString() {
        return this.queryString;
    }

    public PersistenceOrchestrator.QueryType getQueryType() {
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

    public void setParams(List<BindParameter> params) {
        this.params = params;
    }

    public List<BindParameter> getParams() {
        return this.params;
    }

    public Query create(PersistenceOrchestrator po) {
        Query query = po.getQuery(queryString, queryType, queryInput);

        if(columns != null) {
            query.setColumns(columns);
        }

        if(params != null) {
            query.updateParamMap(params);
        }

        return query;
    }
}
