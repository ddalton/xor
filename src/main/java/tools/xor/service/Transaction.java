package tools.xor.service;

import java.sql.Connection;

public interface Transaction
{
    /**
     * Begin a new transaction
     */
    void begin();

    /**
     * Commit the changes in the existing transaction to the Data store
     */
    void commit();

    /**
     * Rollback the changes in the existing transaction
     */
    void rollback();
    
    /**
     * Participate in an existing Transaction modeled
     * using the provided connection.
     * Not all Data stores support this functionality.
     * 
     * @param connection existing JDBC connection
     */
    default void bind(Connection connection) {}
}
