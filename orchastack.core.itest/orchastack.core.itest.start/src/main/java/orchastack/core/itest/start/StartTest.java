package orchastack.core.itest.start;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.itest.biz.UserJpaService;
import orchastack.core.itest.biz.UserCompoundService;
import orchastack.core.itest.entity.CloudUser1;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class StartTest implements BundleActivator {

	private static Logger log = Logger.getLogger(StartTest.class.getName());

	public void start(BundleContext context) throws Exception {
		Subject subject = null;
		try {

			subject = SecurityUtils.getSubject();
			UsernamePasswordToken token = new UsernamePasswordToken("admin",
					"active123");
			subject.login(token);
//			ServiceReference ref = context.getServiceReference(TestLogic.class
//					.getCanonicalName());
//
//			final TestLogic test = (TestLogic) context.getService(ref);
//			test.testLogic();
			
			ServiceReference ref = context.getServiceReference(UserCompoundService.class
					.getCanonicalName());

			final UserCompoundService test = (UserCompoundService) context.getService(ref);
			
			final CloudUser1 u = new CloudUser1(UUID.randomUUID().toString(),
					"xsfdsf", "xxxxxxxxx.xxxxxxx@163.com");

			test.saveOrUpdate(u);
			
		} catch (Exception e) {
			log.log(Level.SEVERE, "Service Error!", e);
		} finally {
			if (subject != null)
				subject.logout();
		}

	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
