package orchastack.core.security.manager;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.Factory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ShiroManager implements BundleActivator {

	private static Logger log = Logger.getLogger(ShiroManager.class.getName());

	private SecurityManager securityManager;

	public void start(BundleContext context) throws Exception {
		// first look shiro.ini in kara.etc
		String path = System.getProperty("karaf.etc");
		if (path == null) {
			// you need to config in system property shiro.config
			path = System.getProperty("shiro.config.path");
		}
		if (path == null) {
			log.log(Level.SEVERE,
					"if kara is not used, you need to set system property: shiro.config.path");
		} else {

			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Factory<SecurityManager> factory=null;
			try {
				Thread.currentThread().setContextClassLoader(
						ShiroManager.class.getClassLoader());

				factory = new IniSecurityManagerFactory(
						"file://" + path + "/shiro.ini");
				securityManager = factory.getInstance();
				SecurityUtils.setSecurityManager(securityManager);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error initiating Shiro, trying again!");

				synchronized (this) {
					wait(6000);
				}

				try {
					securityManager = factory.getInstance();
					SecurityUtils.setSecurityManager(securityManager);
				} catch (Exception e1) {
					log.log(Level.SEVERE,
							"Error initiating Shiro on seccond trying!", e1);
				}
				
				log.log(Level.INFO,
						"Successfully initiated Shiro on seccond trying!");

			} finally {
				Thread.currentThread().setContextClassLoader(cl);
			}
		}
	}

	public void stop(BundleContext context) throws Exception {
		// no need to shutdown
	}

}
