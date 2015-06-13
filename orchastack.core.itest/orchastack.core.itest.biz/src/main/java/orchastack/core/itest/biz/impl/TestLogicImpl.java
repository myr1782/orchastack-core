package orchastack.core.itest.biz.impl;

import java.util.UUID;

import orchastack.core.itest.biz.TestLogic;
import orchastack.core.itest.biz.UserBizService;
import orchastack.core.itest.biz.UserJpaService;
import orchastack.core.itest.entity.CloudUser1;
import orchastack.core.security.annotation.Logical;
import orchastack.core.security.annotation.RequiresRoles;
import orchastack.core.transaction.Transactional;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@Component(managedservice = "orchastack.core.itest.params")
@Instantiate
@Provides
public class TestLogicImpl implements TestLogic {

	@Property
	private String hhhhProps;

	private BundleContext m_context;

	public TestLogicImpl(BundleContext context) {
		m_context = context;
	}

	@Override
	@RequiresRoles(value = "admins,users",logical=Logical.OR)
	@Transactional
	public void testLogic() throws Exception {
		ServiceReference ref = m_context
				.getServiceReference(UserJpaService.class.getCanonicalName());

		final UserJpaService userJpa = (UserJpaService) m_context
				.getService(ref);

		ref = m_context.getServiceReference(UserBizService.class
				.getCanonicalName());

		final UserBizService userBiz = (UserBizService) m_context
				.getService(ref);

		final CloudUser1 u = new CloudUser1(UUID.randomUUID().toString(),
				"xsfdsf", "xxxxxxxxx.xxxxxxx@163.com");

		CloudUser1 atu = userJpa.saveWithTx(u);

		atu.setEmail(hhhhProps);
		userBiz.saveOrUpdate(atu);

		// final AtomicReference<CloudUser> atu = new
		// AtomicReference<CloudUser>();
		// if (userJpa != null)
		// new Thread() {
		//
		// @Override
		// public void run() {
		// try {
		// atu.set(userJpa.saveWithTx(u));
		// } catch (Exception e) {
		// log.log(Level.SEVERE, "Service Error!", e);
		// }
		// }
		//
		// }.start();
		//
		// if (userBiz != null && atu != null) {
		//
		// new Thread() {
		//
		// @Override
		// public void run() {
		// try {
		//
		// long syst = System.currentTimeMillis();
		// while (atu.get() == null) {
		//
		// if (System.currentTimeMillis() - syst > 500000)
		// break;
		// synchronized (this) {
		// wait(10);
		// }
		// }
		// atu.get().setEmail("fsdfsdgdfsgsfgf");
		// userBiz.saveOrUpdate(atu.get());
		// } catch (Exception e) {
		// log.log(Level.SEVERE, "Service Error!", e);
		// }
		// }
		//
		// }.start();
		//
		// }

	}

}
