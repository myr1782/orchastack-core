package orchastack.core.security.handler;

public enum AuthzAnnotationType {

	RequiresRoles,RequiresPermissions,RequiresAuthentication,RequiresGuest,RequiresUser;

	/**
	 * The handler namespace.
	 */
	public static final String NAMESPACE = "orchastack.core.security.authz";
	

	/**
	 * The handler value.
	 */
	public static final String VALUE = "value";
	
}
