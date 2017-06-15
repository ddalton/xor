package tools.xor.providers.jdbc;

import tools.xor.view.Query;

public class JDBCBatchContext
{

    private boolean shouldBatch;
    private Query query;

    public Query getQuery ()
    {
        return query;
    }

    public void setQuery (Query query)
    {
        this.query = query;
    }

    public boolean isShouldBatch ()
    {
        return shouldBatch;
    }

    public void setShouldBatch (boolean shouldBatch)
    {
        this.shouldBatch = shouldBatch;
    }
}
