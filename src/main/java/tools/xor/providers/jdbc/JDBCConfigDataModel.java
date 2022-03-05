package tools.xor.providers.jdbc;

import javax.sql.DataSource;

import tools.xor.TypeMapper;
import tools.xor.service.DataModelFactory;

public class JDBCConfigDataModel extends JDBCDataModel
{
    private DataSource dataSource;

    public JDBCConfigDataModel (DataModelFactory dasFactory, TypeMapper typeMapper)
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
