package tools.xor.providers.jdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.TypeMapper;
import tools.xor.service.DataModelFactory;

public class JDBCSpringDataModel extends JDBCDataModel
{
    @Autowired
    private DataSource dataSource;

    public JDBCSpringDataModel (DataModelFactory dasFactory, TypeMapper typeMapper)
    {
        super(dasFactory, typeMapper);
    }

    @Override public DataSource getDataSource ()
    {
        return this.dataSource;
    }
}
