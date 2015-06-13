package orchastack.core.itest.biz.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.itest.biz.UserBizService;
import orchastack.core.itest.biz.UserCompoundService;
import orchastack.core.itest.biz.UserJpaService;
import orchastack.core.itest.entity.CloudUser1;
import orchastack.core.security.annotation.RequiresRoles;
import orchastack.core.transaction.Transactional;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides(specifications = { orchastack.core.itest.biz.UserCompoundService.class })
@Instantiate
public class UserCompoundServiceImpl implements UserCompoundService {

	final private static Logger log = Logger
			.getLogger(UserCompoundServiceImpl.class.getName());

	@Requires
	private UserJpaService userJpaService;

	@Requires
	private UserBizService userBizService;

	@RequiresRoles(value = "admins")
	@Transactional(timeout = 6000, propagation = "requires")
	public CloudUser1 saveOrUpdate(CloudUser1 user) throws Exception {

		CloudUser1 u = userJpaService.saveWithTx(user);

		for (int i = 0; i < 5; i++) {
			u.setEmail("111@111.111-" + i);
			log.log(Level.INFO, "User - " + u);
			u = userBizService.saveOrUpdate(u);

			log.log(Level.INFO, "User - " + u);
		}
		return u;

	}
}
