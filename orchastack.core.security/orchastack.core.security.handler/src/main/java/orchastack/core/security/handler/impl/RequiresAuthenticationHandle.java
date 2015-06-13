package orchastack.core.security.handler.impl;

import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.security.handler.AuthzAnnotationType;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.annotations.HandlerDeclaration;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;

/**
 * if (!SecurityUtils.getSubject().isAuthenticated()) { <br>
 * throw new AuthorizationException(...); }
 */
@Handler(name = RequiresAuthenticationHandle.NAME, namespace = AuthzAnnotationType.NAMESPACE)
public class RequiresAuthenticationHandle extends PrimitiveHandler {
	final private static Logger log = Logger
			.getLogger(RequiresAuthenticationHandle.class.getName());

	/**
	 * The handler name.
	 */
	public static final String NAME = "requiresauthentication";

	@Override
	public void configure(Element meta, Dictionary configuration)
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
						"A RequiresAuthentication element must specified the method attribute");
			}
			MethodMetadata ma = this.getPojoMetadata().getMethod(method);
			if (ma == null) {
				throw new ConfigurationException(
						"A RequiresAuthentication method is not in the pojo class : "
								+ method);
			}

			SecuredMethod sm = new SecuredMethod(method,
					AuthzAnnotationType.RequiresAuthentication, null, null);
			this.getInstanceManager().register(ma, sm);
		}

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

}
