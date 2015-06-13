package orchastack.jpa.ctx.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionRequiredException;

import orchastack.jpa.ctx.PersistenceContextFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@Component(name = "persistenceContextFactoryBean")
@Instantiate
@Provides(specifications = { orchastack.jpa.ctx.PersistenceContextFactory.class }, strategy = "SINGLETON")
final public class PersistenceContextFactoryImpl implements
		PersistenceContextFactory {

	final private static Logger log = Logger
			.getLogger(PersistenceContextFactoryImpl.class.getName());

	private static ConcurrentHashMap<Thread, EntityManager> jpaContext = new ConcurrentHashMap<Thread, EntityManager>();

	@Requires(filter = "(!(org.apache.aries.jpa.proxy.factory=true))", optional = false)
	private EntityManagerFactory defaultEmfactory = null;

	private static ConcurrentHashMap<String, EntityManagerFactory> emfactoryMap = new ConcurrentHashMap<String, EntityManagerFactory>();

	private BundleContext context;

	public PersistenceContextFactoryImpl(BundleContext context) {
		super();
		this.context = context;
	}

	private EntityManagerFactory getEntityManagerFactory(String unitName)
			throws Exception {

		if (emfactoryMap.containsKey(unitName)) {
			return emfactoryMap.get(unitName);
		} else if (context != null) {

			String filter = "(osgi.unit.name=" + unitName + ")";
			Collection refs = context.getServiceReferences(
					EntityManagerFactory.class, filter);
			if (refs != null && !refs.isEmpty()) {
				Iterator i = refs.iterator();
				ServiceReference ref = (ServiceReference) i.next();
				if (ref != null) {
					EntityManagerFactory emf = (EntityManagerFactory) context
							.getService(ref);
					emfactoryMap.put(unitName, emf);
					if (defaultEmfactory == null)
						defaultEmfactory = emf;
					return emf;
				}
			}

		}

		log.log(Level.WARNING, "Fail to get EntityManagerFactory - " + unitName);

		return null;

	}

	public EntityManager getPersistenceContext(Thread client, Transaction tx,
			String unitName) throws Exception {

		final AtomicReference<EntityManager> pcc = new AtomicReference<EntityManager>();

		if (jpaContext.containsKey(client)) {
			pcc.set(jpaContext.get(client));
		} else {
			if (unitName == null || unitName.equals(""))
				pcc.set(defaultEmfactory.createEntityManager());
			else {
				EntityManagerFactory emf = getEntityManagerFactory(unitName);
				if (emf != null)
					pcc.set(emf.createEntityManager());
			}

			if (pcc.get() != null)
				jpaContext.put(client, pcc.get());
			else
				log.log(Level.WARNING, "Fail to create EntityManager for "
						+ client);

			if (tx != null) {
				if (tx.getStatus() != Status.STATUS_ACTIVE) {
					throw new TransactionRequiredException(
							"Transaction Required!");
				} else {
					// tx.registerSynchronization(new Synchronization() {
					// @Override
					// public void beforeCompletion() {
					// // pcc.get().flush();
					// // pcc.get().close();
					// }
					//
					// @Override
					// public void afterCompletion(int status) {
					// }
					// });
				}
			}
		}

		return pcc.get();
	}
}
