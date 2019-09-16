package tools.xor.service;

public interface Transaction
{
    void begin();

    void commit();

    void rollback();
}
