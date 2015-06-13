package orchastack.core.security.handler.impl;

import java.lang.reflect.Member;

import orchastack.core.security.handler.AuthzAnnotationType;

import org.apache.felix.ipojo.MethodInterceptor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

public class SecuredMethod implements MethodInterceptor {

	private String method;
	private String value = null;
	private String logical = "AND";

	private AuthzAnnotationType type;

	public SecuredMethod(String method, AuthzAnnotationType type, String value,
			String logical) {
		super();
		this.method = method;
		this.value = value;
		this.type = type;
		if (logical != null)
			this.logical = logical;
	}

	public void onEntry(Object pojo, Member method, Object[] args) {
		// TODO Auto-generated method stub
		switch (type) {
		case RequiresRoles:
			requiresRoles();
			break;
		case RequiresPermissions:
			requiresPermissions();
			break;
		case RequiresAuthentication:
			requiresAuthentication();
			break;
		case RequiresGuest:
			requiresGuest();
			break;
		case RequiresUser:
			requiresUser();
			break;
		}

	}

	public void onExit(Object pojo, Member method, Object returnedObj) {

	}

	public void onError(Object pojo, Member method, Throwable throwable) {
		// TODO Auto-generated method stub

	}

	public void onFinally(Object pojo, Member method) {
		// TODO Auto-generated method stub

	}

	private void requiresRoles() throws AuthorizationException {
		Subject subject = SecurityUtils.getSubject();
		String[] roles = this.value.split(",");
		Logical lz = Logical.valueOf(logical);
		if (lz == Logical.AND) {
			boolean hasRole = true;
			for (String role : roles) {
				hasRole &= subject.hasRole(role);
			}

			if (!hasRole)
				throw new AuthorizationException("Security Role " + value
						+ " -AND is required for method " + method);
		} else {
			boolean hasRole = false;
			for (String role : roles) {
				hasRole |= subject.hasRole(role);
			}
			if (!hasRole)
				throw new AuthorizationException("Security Role " + value
						+ " -OR is required for method " + method);
		}

	}

	private void requiresPermissions() throws AuthorizationException {
		Subject subject = SecurityUtils.getSubject();

		String[] permits = this.value.split(",");
		Logical lz = Logical.valueOf(logical);
		if (lz == Logical.AND) {
			boolean hasPermit = true;
			for (String p : permits) {
				hasPermit &= subject.isPermitted(p);
			}

			if (!hasPermit)
				throw new AuthorizationException("Security Permission " + value
						+ " -AND is required for method " + method);
		} else {
			boolean hasPermit = false;
			for (String p : permits) {
				hasPermit |= subject.isPermitted(p);
			}
			if (!hasPermit)
				throw new AuthorizationException("Security Permission " + value
						+ " -OR is required for method " + method);
		}

	}

	private void requiresAuthentication() throws AuthorizationException {
		Subject subject = SecurityUtils.getSubject();
		if (!subject.isAuthenticated())
			throw new AuthorizationException(
					"calling user must be authenticated for method " + method);
	}

	private void requiresUser() throws AuthorizationException {
		Subject subject = SecurityUtils.getSubject();
		PrincipalCollection principals = subject.getPrincipals();
		if (principals == null || principals.isEmpty())
			throw new AuthorizationException(
					"Anonymous calling is not allowed for method " + method);
	}

	private void requiresGuest() throws AuthorizationException {
		Subject subject = SecurityUtils.getSubject();
		PrincipalCollection principals = subject.getPrincipals();
		if (principals != null && !principals.isEmpty())
			throw new AuthorizationException(
					"Only guest user is allowed for method " + method);
	}
}
