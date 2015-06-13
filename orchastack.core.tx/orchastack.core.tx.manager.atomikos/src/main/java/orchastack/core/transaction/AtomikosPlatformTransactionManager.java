/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package orchastack.core.transaction;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 */
public class AtomikosPlatformTransactionManager extends UserTransactionManager
		implements PlatformTransactionManager {

	private static Logger log = Logger
			.getLogger(AtomikosPlatformTransactionManager.class.getName());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final PlatformTransactionManager platformTransactionManager;

	// private final Map<Transaction, SuspendedResourcesHolder>
	// suspendedResources = new ConcurrentHashMap<Transaction,
	// SuspendedResourcesHolder>();

	public AtomikosPlatformTransactionManager() throws XAException {
		try {
			init();
			setForceShutdown(true);
		} catch (SystemException e) {
			log.log(Level.SEVERE, "Error init TransactionManager!", e);
		}
		UserTransactionImp uTM = new UserTransactionImp();

		platformTransactionManager = new JtaTransactionManager(uTM, this);
		registerTransactionAssociationListener();
	}

	public AtomikosPlatformTransactionManager(
			int defaultTransactionTimeoutSeconds) throws XAException,
			SystemException {
		try {
			init();
			setForceShutdown(true);
		} catch (SystemException e) {
			log.log(Level.SEVERE, "Error init TransactionManager!", e);
		}
		UserTransactionImp uTM = new UserTransactionImp();

		uTM.setTransactionTimeout(defaultTransactionTimeoutSeconds);

		platformTransactionManager = new JtaTransactionManager(uTM, this);

		registerTransactionAssociationListener();
	}

	public TransactionStatus getTransaction(TransactionDefinition definition)
			throws TransactionException {
		log.log(Level.INFO, "AtomikosPlatformTransactionManager -- getTransaction()!");
		return platformTransactionManager.getTransaction(definition);
	}

	public void commit(TransactionStatus status) throws TransactionException {
		log.log(Level.INFO, "AtomikosPlatformTransactionManager -- commit()!");
		platformTransactionManager.commit(status);
	}

	public void rollback(TransactionStatus status) throws TransactionException {
		log.log(Level.INFO, "AtomikosPlatformTransactionManager -- rollback()!");
		platformTransactionManager.rollback(status);
	}

	protected void registerTransactionAssociationListener() {

		// TODO for Atomikos, assoicate Thread with TX is done by
		// CompositeTransactionManager automatically.
		// client code needs to do nothing.

	}

	public void stop() {
		close();
	}

	/**
	 * Holder for suspended resources. Used internally by <code>suspend</code>
	 * and <code>resume</code>.
	 */
	// private static class SuspendedResourcesHolder {
	//
	// private final Object suspendedResources;
	//
	// private final List<?> suspendedSynchronizations;
	//
	// private final String name;
	//
	// private final boolean readOnly;
	//
	// public SuspendedResourcesHolder(Object suspendedResources,
	// List<?> suspendedSynchronizations, String name, boolean readOnly) {
	//
	// this.suspendedResources = suspendedResources;
	// this.suspendedSynchronizations = suspendedSynchronizations;
	// this.name = name;
	// this.readOnly = readOnly;
	// }
	//
	// public Object getSuspendedResources() {
	// return suspendedResources;
	// }
	//
	// public List<?> getSuspendedSynchronizations() {
	// return suspendedSynchronizations;
	// }
	//
	// public String getName() {
	// return name;
	// }
	//
	// public boolean isReadOnly() {
	// return readOnly;
	// }
	// }

}
