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
package orchastack.core.jpa.container.tx;

import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Used to avoid a dependency on javax.transaction.TransactionManager
 */
public class OSGiTransactionManager {

	private static final AtomicReference<OSGiTransactionManager> INSTANCE = new AtomicReference<OSGiTransactionManager>();
	private static final AtomicReference<BundleContext> context = new AtomicReference<BundleContext>();

	private static ServiceReference reference = null;
	private final AtomicReference<TransactionManager> txManager = new AtomicReference<TransactionManager>();

	@SuppressWarnings("unchecked")
	public static void init(BundleContext ctx) {
		try {
			Class<TransactionManager> txMgrClass = (Class<TransactionManager>) Class
					.forName("javax.transaction.TransactionManager", false,
							OSGiTransactionManager.class.getClassLoader());

			OSGiTransactionManager otm = new OSGiTransactionManager(txMgrClass,
					ctx);
			if (!!!INSTANCE.compareAndSet(null, otm))
				otm.destroy();
		} catch (ClassNotFoundException cnfe) {
			// No op
		}
	}

	public static OSGiTransactionManager get() {
		return INSTANCE.get();
	}

	private OSGiTransactionManager(Class<TransactionManager> txMgrClass,
			BundleContext ctx) {

		context.set(ctx);
		ServiceReference ref = ctx
				.getServiceReference(txMgrClass);
		if (ref != null) {
			reference = ref;
			TransactionManager txMgt = (TransactionManager)ctx.getService(reference);
			if (txMgt != null)
				txManager.set(txMgt);
		}

		// found TxManager
	}

	private TransactionManager getTransactionManager() {
		TransactionManager txMgr = txManager.get();

		if (txMgr == null)
			throw new IllegalStateException("unable.to.get.tx.mgr");

		return txMgr;
	}

	public static void destroy() {
		// unget TxManager
		context.get().ungetService(reference);
	}

	public int getStatus() throws SystemException {
		return getTransactionManager().getStatus();
	}

	public void resume(Transaction arg0) throws IllegalStateException,
			InvalidTransactionException, SystemException {
		getTransactionManager().resume(arg0);
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		getTransactionManager().setRollbackOnly();
	}

	public Transaction getTransaction() throws SystemException {
		// TODO Auto-generated method stub
		return getTransactionManager().getTransaction();
	}
}
