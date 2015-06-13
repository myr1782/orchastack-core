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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public class BundleToClassLoaderAdapter extends ClassLoader {

	private final Bundle b;

	public BundleToClassLoaderAdapter(Bundle bundle) {
		b = bundle;
	}

	@Override
	public URL getResource(final String name) {
		return AccessController.doPrivileged(new PrivilegedAction<URL>() {
			public URL run() {
				return b.getResource(name);
			}
		});
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		URL url = getResource(name);

		InputStream result = null;

		if (url != null) {
			try {
				result = url.openStream();
			} catch (IOException e) {
			}
		}

		return result;
	}

	@Override
	public Enumeration<URL> getResources(final String name) throws IOException {
		Enumeration<URL> urls;
		try {
			urls = AccessController
					.doPrivileged(new PrivilegedExceptionAction<Enumeration<URL>>() {
						@SuppressWarnings("unchecked")
						public Enumeration<URL> run() throws IOException {
							return b.getResources(name);
						}
					});
		} catch (PrivilegedActionException e) {
			Exception cause = e.getException();

			if (cause instanceof IOException)
				throw (IOException) cause;
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;

			IOException ioe = new IOException(name);
			ioe.initCause(cause);
			throw ioe;
		}

		if (urls == null) {
			urls = Collections.enumeration(new ArrayList<URL>());
		}

		return urls;
	}

	/*
	 * Notes we overwrite loadClass rather than findClass because we don't want
	 * to delegate to the default classloader, only the bundle.
	 * 
	 * Also note that ClassLoader#loadClass(String) by javadoc on ClassLoader
	 * delegates to this method, so we don't need to overwrite it separately.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
	 */
	@Override
	public Class<?> loadClass(final String name, boolean resolve)
			throws ClassNotFoundException {
		try {
			Class<?> result = AccessController
					.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
						public Class<?> run() throws ClassNotFoundException {
							return b.loadClass(name);
						}
					});

			if (resolve)
				resolveClass(result);

			return result;
		} catch (PrivilegedActionException e) {
			Exception cause = e.getException();

			if (cause instanceof ClassNotFoundException)
				throw (ClassNotFoundException) cause;
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;

			throw new ClassNotFoundException(name, cause);
		}
	}
}