package tools.xor.providers.jdbc;

import org.springframework.beans.factory.annotation.Autowired;
import tools.xor.TypeMapper;
import tools.xor.service.DASFactory;

import javax.sql.DataSource;

public class JDBCSpringDAS extends JDBCDAS
{
    @Autowired
    private DataSource dataSource;

    public JDBCSpringDAS (DASFactory dasFactory, TypeMapper typeMapper)
    {
        super(dasFactory, typeMapper);
    }

    @Override public DataSource getDataSource ()
    {
        return this.dataSource;
    }
}
