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

package orchastack.core.transaction.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Callback;

@Handler(name = TransactionHandler.NAME, namespace = TransactionHandler.NAMESPACE)
public class TransactionHandler extends PrimitiveHandler implements
		Synchronization {

	public static final String NAMESPACE = "orchastack.core.transaction";

	public static final String NAME = "transactional";

	private static final String METHOD_ATTRIBUTE = "method";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String PROPAGATION_ATTRIBUTE = "propagation";

	private static final String EXCEPTIONONROLLBACK_ATTRIBUTE = "exceptiononrollback";

	private static final String NOROLLBACKFOR_ATTRIBUTE = "norollbackfor";

	public static final int DEFAULT_PROPAGATION = TransactionalMethod.REQUIRES;

	@Requires
	private TransactionManager m_transactionManager; // Service Dependency

	private List<TransactionalMethod> m_methods = new ArrayList<TransactionalMethod>();

	private Callback m_onRollback;

	private Callback m_onCommit;

	private List<Transaction> m_transactions = new ArrayList<Transaction>();

	public void configure(Element arg0, Dictionary arg1)
			throws ConfigurationException {
		Element[] elements = arg0.getElements(NAME, NAMESPACE);
		for (int i = 0; i < elements.length; i++) {
			String method = elements[i].getAttribute(METHOD_ATTRIBUTE);
			String to = elements[i].getAttribute(TIMEOUT_ATTRIBUTE);
			String propa = elements[i].getAttribute(PROPAGATION_ATTRIBUTE);
			String nrbf = elements[i].getAttribute(NOROLLBACKFOR_ATTRIBUTE);
			String eorb = elements[i]
					.getAttribute(EXCEPTIONONROLLBACK_ATTRIBUTE);

			if (method == null) {
				method = elements[i].getAttribute("name");
			}

			if (method == null) {
				throw new ConfigurationException(
						"A transactional element must specified the method attribute");
			}
			MethodMetadata meta = this.getPojoMetadata().getMethod(method);
			if (meta == null) {
				throw new ConfigurationException(
						"A transactional method is not in the pojo class : "
								+ method);
			}

			int timeout = 0;
			if (to != null) {
				timeout = new Integer(to).intValue();
			}

			int propagation = DEFAULT_PROPAGATION;
			if (propa != null) {
				propagation = parsePropagation(propa);
			}

			List<String> exceptions = new ArrayList<String>();
			if (nrbf != null) {
				exceptions = (List<String>) ParseUtils.parseArraysAsList(nrbf);
			}

			boolean exceptionOnRollback = false;
			if (eorb != null) {
				exceptionOnRollback = new Boolean(eorb).booleanValue();
			}

			TransactionalMethod tm = new TransactionalMethod(method,
					propagation, timeout, exceptions, exceptionOnRollback, this);
			m_methods.add(tm);
			this.getInstanceManager().register(meta, tm);
		}

	}

	private int parsePropagation(String propa) throws ConfigurationException {
		if (propa.equalsIgnoreCase("requires")) {
			return TransactionalMethod.REQUIRES;

		} else if (propa.equalsIgnoreCase("mandatory")) {
			return TransactionalMethod.MANDATORY;

		} else if (propa.equalsIgnoreCase("notsupported")) {
			return TransactionalMethod.NOT_SUPPORTED;

		} else if (propa.equalsIgnoreCase("supported")) {
			return TransactionalMethod.SUPPORTED;

		} else if (propa.equalsIgnoreCase("never")) {
			return TransactionalMethod.NEVER;

		} else if (propa.equalsIgnoreCase("requiresnew")) {
			return TransactionalMethod.REQUIRES_NEW;
		}

		throw new ConfigurationException("Unknown propgation policy : " + propa);
	}

	public void start() {
		// Set transaction managers.
		for (TransactionalMethod method : m_methods) {
			method.setTransactionManager(m_transactionManager);
		}
	}

	public void stop() {
		// Nothing to do.
	}

	@Bind
	public synchronized void bind(TransactionManager tm) {
		for (TransactionalMethod method : m_methods) {
			method.setTransactionManager(tm);
		}
	}

	@Unbind
	public synchronized void unbind(TransactionManager tm) {
		for (TransactionalMethod method : m_methods) {
			method.setTransactionManager(null);
		}
	}

	public void transactionRolledback(Transaction t) {
		if (m_onRollback != null) {
			try {
				m_onRollback.call(new Object[] { t });
			} catch (NoSuchMethodException e1) {
				error("Cannot invoke the onRollback method, method not found",
						e1);
			} catch (IllegalAccessException e1) {
				error("Cannot invoke the onRollback method,cannot access the method",
						e1);
			} catch (InvocationTargetException e1) {
				error("Cannot invoke the onRollback method,the method thrown an exception",
						e1.getTargetException());
			}
		}
	}

	public void transactionCommitted(Transaction t) {
		if (m_onRollback != null) {
			try {
				m_onCommit.call(new Object[] { t });
			} catch (NoSuchMethodException e1) {
				error("Cannot invoke the onCommit callback, method not found",
						e1);
			} catch (IllegalAccessException e1) {
				error("Cannot invoke the onCommit callback,cannot access the method",
						e1);
			} catch (InvocationTargetException e1) {
				error("Cannot invoke the onCommit callback,the method thrown an exception",
						e1.getTargetException());
			}
		}

	}

	public void stateChanged(int newState) {
		if (newState == ComponentInstance.INVALID) {
			// rollback all owned transactions.
			for (int i = 0; i < m_methods.size(); i++) {
				m_methods.get(i).rollbackOwnedTransactions();
			}

			for (int i = 0; i < m_transactions.size(); i++) {
				try {
					m_transactions.get(i).setRollbackOnly();
				} catch (Exception e) {
					error("Cannot set rollback only on a transaction : "
							+ e.getMessage());
				}
			}
		}
	}

	public synchronized Object onGet(Object pojo, String fieldName, Object value) {
		try {
			if (m_transactionManager != null) {
				return m_transactionManager.getTransaction();
			} else {
				return null;
			}
		} catch (SystemException e) {
			error("Cannot get the current transaction, internal error", e);
			return null;
		}
	}

	public void afterCompletion(int arg0) {
		try {
			if (m_transactionManager.getTransaction() != null) {
				m_transactions.remove(m_transactionManager.getTransaction());
				if (arg0 == Status.STATUS_ROLLEDBACK) {
					transactionRolledback(m_transactionManager.getTransaction());
				} else if (arg0 == Status.STATUS_COMMITTED) {
					transactionCommitted(m_transactionManager.getTransaction());
				}
			}
		} catch (SystemException e) {
			error("Cannot remove the transaction from the transaction list : "
					+ e.getMessage());
		}
	}

	public void beforeCompletion() {

	}

	public void addTransaction(Transaction transaction) {
		if (m_transactions.contains(transaction)) {
			return;
		}
		try {
			transaction.registerSynchronization(this);
			m_transactions.add(transaction);
		} catch (Exception e) {
			error("Cannot add the transaction to the transaction list : "
					+ e.getMessage());
		}
	}

	public List<Transaction> getTransactions() {
		return m_transactions;
	}

}
