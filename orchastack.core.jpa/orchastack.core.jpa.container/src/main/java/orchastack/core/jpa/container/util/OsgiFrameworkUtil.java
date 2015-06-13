/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package orchastack.core.jpa.container.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public final class OsgiFrameworkUtil {

	/**
	 * This method attempts to get the classloader for a bundle. It may return
	 * null if their is no such classloader, or if it cannot obtain the
	 * classloader for the bundle.
	 * 
	 * @param b
	 *            the bundle whose classloader is desired.
	 * @return the classloader if found, or null if for example the bundle is in
	 *         INSTALLED or UNINSTALLED state
	 */
	public static ClassLoader getClassLoader(Bundle b) {
		if (b != null && b.getState() != Bundle.UNINSTALLED
				&& b.getState() != Bundle.INSTALLED) {
			return defualtGetClassLoader(b);
		} else {
			return null;
		}
	}

	private static ClassLoader defualtGetClassLoader(final Bundle b) {
		ClassLoader cl = null;
		// so first off try to get the real classloader. We can do this by
		// loading a known class
		// such as the bundle activator. There is no guarantee this will work,
		// so we have a back door too.
		String activator = (String) b.getHeaders().get(
				Constants.BUNDLE_ACTIVATOR);
		if (activator != null) {
			try {
				Class<?> clazz = b.loadClass(activator);
				// so we have the class, but it could have been imported, so we
				// make sure the two bundles
				// are the same. A reference check should work here because
				// there will be one.
				Bundle activatorBundle = FrameworkUtil.getBundle(clazz);
				if (activatorBundle == b) {
					cl = clazz.getClassLoader();
				}
			} catch (ClassNotFoundException e) {
			}
		}

		if (cl == null) {
			// ok so we haven't found a class loader yet, so we need to create a
			// wapper class loader
			cl = AccessController
					.doPrivileged(new PrivilegedAction<ClassLoader>() {
						public ClassLoader run() {
							return new BundleToClassLoaderAdapter(b);
						}
					});
		}

		return cl;
	}

	/**
	 * Safely unregister the supplied ServiceRegistration, for when you don't
	 * care about the potential IllegalStateException and don't want it to run
	 * wild through your code
	 * 
	 * @param reg
	 *            The {@link ServiceRegistration}, may be null
	 */
	public static void safeUnregisterService(ServiceRegistration reg) {
		if (reg != null) {
			try {
				reg.unregister();
			} catch (IllegalStateException e) {
				// This can be safely ignored
			}
		}
	}
}