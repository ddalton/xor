package tools.xor.service;

import java.sql.Connection;

public interface ConnectionUtil
{
    /**
     * Will get the active connection from an existing transaction
     * Not all providers support directly retrieving the connection.
     *
     * @return JDBC connection
     */
    Connection getConnection();

    /**
     * Execute a piece of code in the scope of a JDBC connection
     * This pattern is supported by those providers that do not directly allow access to
     * a JDBC connection
     *
     * @param context code to be executed
     */
    void execute (Object context);
}
