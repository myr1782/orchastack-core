package orchastack.core.security.handler.impl;

import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.security.handler.AuthzAnnotationType;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;

/**
 * Subject currentUser = SecurityUtils.getSubject(); if
 * (!subject.isPermitted("account:create")) { throw new
 * AuthorizationException(...); }
 */
@Handler(name = RequiresPermissionsHandler.NAME, namespace = AuthzAnnotationType.NAMESPACE)
public class RequiresPermissionsHandler extends PrimitiveHandler {
	final private static Logger log = Logger
			.getLogger(RequiresPermissionsHandler.class.getName());

	/**
	 * The handler name.
	 */
	public static final String NAME = "requirespermissions";

	/**
	 * Start method. Starts managed dependencies.
	 * 
	 * @see org.apache.felix.ipojo.Handler#start()
	 */
	public void start() {
	}

	/**
	 * Stop method. Stops managed dependencies.
	 * 
	 * @see org.apache.felix.ipojo.Handler#stop()
	 */
	public void stop() {
	}

	public void configure(Element meta, Dictionary dictionary)
			throws ConfigurationException {

		log.log(Level.FINE, "configuring iPOJO component!!!");

		Element[] elements = meta.getElements(NAME,
				AuthzAnnotationType.NAMESPACE);
		for (int i = 0; i < elements.length; i++) {
			String method = elements[i].getAttribute("method");

			if (method == null) {
				method = elements[i].getAttribute("name");
			}

			if (method == null) {
				throw new ConfigurationException(
						"A RequiresPermissions element must specified the method attribute");
			}
			MethodMetadata ma = this.getPojoMetadata().getMethod(method);
			if (ma == null) {
				throw new ConfigurationException(
						"A RequiresPermissions method is not in the pojo class : "
								+ method);
			}
			String value = elements[i].getAttribute(AuthzAnnotationType.VALUE);

			if (value == null) {
				throw new ConfigurationException(
						"A RequiresPermissions element must specified the value attribute");
			}
			String logical = elements[i].getAttribute("logical");

			SecuredMethod sm = new SecuredMethod(method,
					AuthzAnnotationType.RequiresPermissions, value, logical);
			this.getInstanceManager().register(ma, sm);
		}

	}

}
