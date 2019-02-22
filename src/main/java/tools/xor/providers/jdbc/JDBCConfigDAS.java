package tools.xor.providers.jdbc;

import tools.xor.TypeMapper;
import tools.xor.service.DASFactory;

import javax.sql.DataSource;

public class JDBCConfigDAS extends JDBCDAS
{
    private DataSource dataSource;

    public JDBCConfigDAS (DASFactory dasFactory, TypeMapper typeMapper)
    {
        super(dasFactory, typeMapper);
    }

    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
    }

    @Override public DataSource getDataSource ()
    {
        return dataSource;
    }
}
