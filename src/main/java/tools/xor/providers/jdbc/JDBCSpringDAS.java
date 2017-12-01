package tools.xor.providers.jdbc;

import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

public class JDBCSpringDAS extends JDBCDAS
{
    @Autowired
    private DataSource dataSource;

    @Override public DataSource getDataSource ()
    {
        return this.dataSource;
    }
}
