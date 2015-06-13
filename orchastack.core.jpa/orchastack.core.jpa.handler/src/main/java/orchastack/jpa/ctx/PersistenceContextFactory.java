package orchastack.jpa.ctx;

import javax.persistence.EntityManager;
import javax.transaction.Transaction;
import javax.transaction.TransactionSynchronizationRegistry;

public interface PersistenceContextFactory {
	public EntityManager getPersistenceContext(Thread client,Transaction tx,String unitName) throws Exception;
}
