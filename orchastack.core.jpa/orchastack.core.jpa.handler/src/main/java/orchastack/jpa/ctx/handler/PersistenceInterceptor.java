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
package orchastack.jpa.ctx.handler;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.TransactionManager;

import orchastack.jpa.ctx.PersistenceContextFactory;

import org.apache.felix.ipojo.FieldInterceptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class PersistenceInterceptor implements FieldInterceptor {

	final static private Logger log = Logger
			.getLogger(PersistenceInterceptor.class.getName());

	/**
	 * The timeout.
	 */
	private long m_timeout;

	/**
	 * The handler managing this inject.
	 */
	private BundleContext m_context;

	private String m_unitName;

	public PersistenceInterceptor(BundleContext context, String unitName,
			long timeout) {
		this.m_context = context;
		this.m_timeout = timeout;
		this.m_unitName = unitName;
	}

	/**
	 * The code require a value of the monitored field. If providers are
	 * available, the method return service object(s) immediately. Else, the
	 * thread is blocked until an arrival. If no provider arrives during the
	 * among of time specified, the method throws a Runtime Exception.
	 * 
	 * @param caller
	 *            POJO instance asking for the service
	 * @param field
	 *            field name
	 * @param oldValue
	 *            previous value
	 * @return the object to inject.
	 * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object,
	 *      java.lang.String, java.lang.Object)
	 */
	public synchronized Object onGet(Object caller, String field,
			Object oldValue) {

		log.log(Level.INFO, "PersistenceInterceptor onGet .... ");
		// Begin to wait ...
		long enter = System.currentTimeMillis();
		boolean exhausted = false;
		synchronized (this) {
			while (!getServices() && !exhausted) {
				try {
					wait(1);
				} catch (InterruptedException e) {
					// We was interrupted ....
				} finally {
					long end = System.currentTimeMillis();
					exhausted = (end - enter) > m_timeout;
				}
			}
		}
		// Check
		if (exhausted) {
			return onTimeout();
		} else {
			try {
				// FIXME the call's thread
				log.log(Level.INFO, "onGet going to return .... ");
				return pcf.getPersistenceContext(Thread.currentThread(),
						txManager.getTransaction(), m_unitName);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error getting PersistenceContext", e);
			}
		}

		log.log(Level.INFO, "onGet return NULL!!! ");

		return null;

	}

	// @Requires(optional = false, timeout = 6000)
	protected PersistenceContextFactory pcf = null;

	// @Requires(optional = false, timeout = 6000)
	protected TransactionManager txManager = null;

	private boolean getServices() {

		ServiceReference refTx = m_context
				.getServiceReference(TransactionManager.class);

		ServiceReference refPcf = m_context
				.getServiceReference(PersistenceContextFactory.class);

		if (refTx != null && refPcf != null) {

			txManager = (TransactionManager) m_context.getService(refTx);
			pcf = (PersistenceContextFactory) m_context.getService(refPcf);

			return true;
		}

		return false;
	}

	/**
	 * The monitored field receives a value. Nothing to do.
	 * 
	 * @param arg0
	 *            POJO setting the value.
	 * @param arg1
	 *            field name
	 * @param arg2
	 *            received value
	 * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object,
	 *      java.lang.String, java.lang.Object)
	 */
	public void onSet(Object arg0, String arg1, Object arg2) {
		

	}

	/**
	 * Implements the timeout policy according to the specified configuration.
	 * 
	 * @return the object to return when the timeout occurs.
	 */
	Object onTimeout() {
		// Throws a runtime exception
		throw new RuntimeException("Persistence Context unavailable : timeout");

	}

	long getTimeout() {
		return m_timeout;
	}

}
