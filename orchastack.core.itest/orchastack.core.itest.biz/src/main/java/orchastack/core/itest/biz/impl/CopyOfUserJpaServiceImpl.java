package orchastack.core.itest.biz.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import orchastack.core.itest.biz.UserJpaService;
import orchastack.core.itest.entity.CloudUser1;
import orchastack.core.security.annotation.RequiresRoles;
import orchastack.core.transaction.Transactional;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides(specifications = { orchastack.core.itest.biz.UserJpaService.class })
@Instantiate
public class CopyOfUserJpaServiceImpl implements UserJpaService {

	final private static Logger log = Logger
			.getLogger(CopyOfUserJpaServiceImpl.class.getName());

	@PersistenceContext(unitName = "orcha-entity")
	private EntityManager persist;

	@RequiresRoles(value = "admins")
	@Transactional(timeout = 4000, propagation = "requires")
	public CloudUser1 saveWithTx(CloudUser1 user) throws Exception {
		log.log(Level.INFO, "saveWithTx............" + persist);

		user.setDeleted(true);
		CloudUser1 u = persist.merge(user);
		
		log.log(Level.INFO, "saveWithTx......contains......" + 	persist.contains(u));
		
		return u;
	}
}
