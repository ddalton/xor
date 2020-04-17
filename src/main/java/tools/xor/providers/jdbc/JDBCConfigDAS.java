package tools.xor.providers.jdbc;

import tools.xor.TypeMapper;
import tools.xor.service.DataModelFactory;

import javax.sql.DataSource;

public class JDBCConfigDAS extends JDBCDataModel
{
    private DataSource dataSource;

    public JDBCConfigDAS (DataModelFactory dasFactory, TypeMapper typeMapper)
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
