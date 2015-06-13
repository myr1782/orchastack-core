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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;

import org.apache.aries.util.AriesFrameworkUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 */
public class TransactionManagerService {

	public static final String TRANSACTION_TIMEOUT = "aries.transaction.timeout";
	public static final String RECOVERABLE = "aries.transaction.recoverable";

	public static final int DEFAULT_TRANSACTION_TIMEOUT = 600; // 600 seconds ->
																// 10 minutes
	public static final boolean DEFAULT_RECOVERABLE = false; // not recoverable
																// by default

	private static final String PLATFORM_TRANSACTION_MANAGER_CLASS = "org.springframework.transaction.PlatformTransactionManager";

	private final String pid;
	private final Dictionary properties;
	private final BundleContext bundleContext;
	private boolean useSpring;
	private TransactionManager transactionManager;
	// private TransactionLog transactionLog;
	private ServiceRegistration serviceRegistration;

	public TransactionManagerService(String pid, Dictionary properties,
			BundleContext bundleContext) throws ConfigurationException {
		this.pid = pid;
		this.properties = properties;
		this.bundleContext = bundleContext;
		// Transaction timeout
		int transactionTimeout = getInt(TRANSACTION_TIMEOUT,
				DEFAULT_TRANSACTION_TIMEOUT);
		if (transactionTimeout <= 0) {
			throw new ConfigurationException(TRANSACTION_TIMEOUT,
					NLS.MESSAGES.getMessage("tx.timeout.greaterthan.zero"));
		}

		// Create transaction manager

		try {
			transactionManager = new SpringTransactionManagerCreator()
					.create(transactionTimeout);
			useSpring = false;
		} catch (NoClassDefFoundError e) {

		} catch (Exception e) {
			throw new RuntimeException(
					NLS.MESSAGES.getMessage("tx.recovery.error"), e);
		}
	}

	public void start() throws Exception {
		List<String> clazzes = new ArrayList<String>();
		clazzes.add(TransactionManager.class.getName());
		clazzes.add(TransactionSynchronizationRegistry.class.getName());
		clazzes.add(UserTransaction.class.getName());
		// clazzes.add(RecoverableTransactionManager.class.getName());
		if (useSpring) {
			clazzes.add(PLATFORM_TRANSACTION_MANAGER_CLASS);
		}
		serviceRegistration = bundleContext.registerService(
				clazzes.toArray(new String[clazzes.size()]),
				transactionManager, new Properties());
	}

	public void close() throws Exception {
		AriesFrameworkUtil.safeUnregisterService(serviceRegistration);
	}

	private String getString(String property, String dflt)
			throws ConfigurationException {
		String value = (String) properties.get(property);
		if (value != null) {
			return value;
		}
		return dflt;
	}

	private int getInt(String property, int dflt) throws ConfigurationException {
		String value = (String) properties.get(property);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				throw new ConfigurationException(property,
						NLS.MESSAGES.getMessage("prop.value.not.int", property,
								value), e);
			}
		}
		return dflt;
	}

	private boolean getBool(String property, boolean dflt)
			throws ConfigurationException {
		String value = (String) properties.get(property);
		if (value != null) {
			try {
				return Boolean.parseBoolean(value);
			} catch (Exception e) {
				throw new ConfigurationException(property,
						NLS.MESSAGES.getMessage("prop.value.not.boolean",
								property, value), e);
			}
		}
		return dflt;
	}

	/**
	 * We use an inner static class to decouple this class from the spring-tx
	 * classes in order to not have NoClassDefFoundError if those are not
	 * present.
	 */
	public static class SpringTransactionManagerCreator {

		public TransactionManager create(int defaultTransactionTimeoutSeconds)
				throws XAException, SystemException {
			return new AtomikosPlatformTransactionManager(
					defaultTransactionTimeoutSeconds);
		}

	}
}
