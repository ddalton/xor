package tools.xor.service;

import java.sql.Connection;

import tools.xor.providers.jdbc.JDBCSessionContext;

public class JDBCTransaction implements Transaction
{
    private JDBCSessionContext sc;

    public JDBCTransaction(JDBCSessionContext sc) {
        this.sc = sc;
    }

    @Override public void begin ()
    {
        sc.beginTransaction();
    }

    @Override public void commit ()
    {
        sc.commit();
    }

    @Override public void rollback ()
    {
        sc.rollback();
    }
    
    @Override public void bind(Connection connection) {
        sc.attachToExisting(connection);
    }
}
