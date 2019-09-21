package tools.xor.service;

import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class JPATransaction implements Transaction
{
    private JpaTransactionManager txManager;
    private TransactionStatus status;

    public JPATransaction (JpaTransactionManager txManager) {
        this.txManager = txManager;
    }

    @Override public void begin ()
    {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("rootTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        status = txManager.getTransaction(def);

    }

    @Override public void commit ()
    {
        txManager.commit(status);
    }

    @Override public void rollback ()
    {
        txManager.rollback(status);
    }
}
