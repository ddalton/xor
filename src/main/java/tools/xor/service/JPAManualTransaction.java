package tools.xor.service;

import javax.persistence.EntityTransaction;

public class JPAManualTransaction implements Transaction
{
    private EntityTransaction transaction;

    public JPAManualTransaction (EntityTransaction transaction) {
        this.transaction = transaction;
    }

    @Override public void begin ()
    {
        transaction.begin();
    }

    @Override public void commit ()
    {
        transaction.commit();
    }

    @Override public void rollback ()
    {
        transaction.rollback();
    }
}
