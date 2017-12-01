package tools.xor.providers.jdbc;

import javax.sql.DataSource;

public class JDBCConfigDAS extends JDBCDAS
{
    private DataSource dataSource;

    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
    }

    @Override public DataSource getDataSource ()
    {
        return dataSource;
    }
}
