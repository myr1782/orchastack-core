package orchastack.core.itest.biz.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import orchastack.core.itest.biz.UserBizService;
import orchastack.core.itest.entity.CloudUser1;
import orchastack.core.security.annotation.RequiresRoles;
import orchastack.core.transaction.Transactional;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides(specifications = { orchastack.core.itest.biz.UserBizService.class })
@Instantiate
public class UserBizServiceImpl implements UserBizService {

	final private static Logger log = Logger.getLogger(UserBizServiceImpl.class
			.getName());

	@PersistenceContext(unitName = "orcha-entity")
	private EntityManager persist;

	@RequiresRoles(value = "cloudUsers")
	@Transactional(timeout = 4000, propagation = "requires")
	public CloudUser1 saveOrUpdate(CloudUser1 user) throws Exception {
		log.log(Level.INFO, "saveOrUpdate............" + persist);

		boolean ccc = persist.contains(user);
		log.log(Level.INFO, "saveOrUpdate...contains...." + ccc);

		CloudUser1 u = persist.merge(user);

//		u.setEmail("fsdfsdgdfsgsfgf");

		return u;

	}
}
