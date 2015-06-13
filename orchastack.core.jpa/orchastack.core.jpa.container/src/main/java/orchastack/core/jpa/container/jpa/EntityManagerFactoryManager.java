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
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package orchastack.core.jpa.container.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import orchastack.core.jpa.container.ParsedPersistenceUnit;
import orchastack.core.jpa.container.util.OsgiFrameworkUtil;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * This class manages the lifecycle of Persistence Units and their associated
 * {@link EntityManagerFactory} objects.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class EntityManagerFactoryManager {

	/** The container's {@link BundleContext} */
	private final BundleContext containerContext;
	/** The persistence bundle */
	private final Bundle bundle;
	/** The {@link PersistenceProvider} to use */
	private ServiceReference provider;
	/** The named persistence units to manage */
	private Map<String, PersistenceUnitInfo> persistenceUnits;
	/** The original parsed data */
	private Collection<ParsedPersistenceUnit> parsedData;
	/** A Map of created {@link EntityManagerFactory}s */
	private Map<String, EntityManagerFactory> emfs = null;
	/**
	 * The {@link ServiceRegistration} objects for the
	 * {@link EntityManagerFactory}s
	 */
	private ConcurrentMap<String, ServiceRegistration> registrations = null;

	/** Logger */
	private static final Logger _logger = Logger
			.getLogger(EntityManagerFactoryManager.class.getName());

	/**
	 * Create an {@link EntityManagerFactoryManager} for the supplied
	 * persistence bundle.
	 * 
	 * This constructor should only be used by a
	 * {@link PersistenceBundleManager} that is synchronized on itself, and the
	 * resulting manager should be immediately stored in the bundleToManager Map
	 * 
	 * @param containerCtx
	 * @param b
	 */
	public EntityManagerFactoryManager(BundleContext containerCtx, Bundle b) {
		containerContext = containerCtx;
		bundle = b;
	}

	private Map<String, PersistenceUnitInfo> getInfoMap(
			Collection<PersistenceUnitInfo> infos) {
		Map<String, PersistenceUnitInfo> map = Collections
				.synchronizedMap(new HashMap<String, PersistenceUnitInfo>());
		if (infos != null) {
			for (PersistenceUnitInfo info : infos) {
				map.put(info.getPersistenceUnitName(), info);
			}
		}
		return map;
	}

	/**
	 * Notify the {@link EntityManagerFactoryManager} that a provider is being
	 * removed from the service registry.
	 * 
	 * If the provider is used by this {@link EntityManagerFactoryManager} then
	 * the manager should destroy the dependent persistence units.
	 * 
	 * <b>This method should only be called when not holding any locks</b>
	 * 
	 * @param ref
	 *            The provider service reference
	 * @return true if the the provider is being used by this manager
	 */
	public synchronized boolean providerRemoved(ServiceReference ref) {
		boolean toReturn = false;
		if (provider != null) {
			toReturn = provider.equals(ref);
		}

		if (toReturn)
			destroy();

		return toReturn;
	}

	/**
	 * Notify the {@link EntityManagerFactoryManager} that the bundle it is
	 * managing has changed state
	 * 
	 * <b>This method should only be called when not holding any locks</b>
	 * 
	 * @throws InvalidPersistenceUnitException
	 *             if the manager is no longer valid and should be destroyed
	 */
	public synchronized void bundleStateChange()
			throws InvalidPersistenceUnitException {

		_logger.log(Level.FINE, " bundleStateChange, --" + bundle.getState());

		switch (bundle.getState()) {
		case Bundle.RESOLVED:
			// If we are Resolved as a result of having stopped
			// and missed the STOPPING event we need to unregister
			// unregisterEntityManagerFactories();
			_logger.log(Level.FINE, "bundle is RESOLVED,  do nothing----");
			break;
		// Starting and active both require EMFs to be registered
		case Bundle.STARTING:
			_logger.log(Level.FINE, "bundle is STARTING,  do nothing----");
			break;
		case Bundle.ACTIVE:
			_logger.log(Level.FINE,
					"bundle is ACTIVE,  registerEntityManagerFactories()-start");
			registerEntityManagerFactories();
			_logger.log(Level.INFO,
					"bundle is ACTIVE,  register EntityManagerFactories - finished");
			break;
		// Stopping means the EMFs should
		case Bundle.STOPPING:
			unregisterEntityManagerFactories();
			_logger.log(Level.INFO,
					"bundle is STOPPING,  unregister EntityManagerFactories ----");
			break;
		case Bundle.UNINSTALLED:
			// Destroy everything
			destroyEntityManagerFactories();
			_logger.log(Level.INFO,
					"bundle is INSTALLED,  destroy EntityManagerFactories ----");
		}
	}

	/**
	 * Unregister all {@link EntityManagerFactory} services
	 */
	private void unregisterEntityManagerFactories() {
		// If we have registrations then unregister them
		if (registrations != null) {
			for (Entry<String, ServiceRegistration> entry : registrations
					.entrySet()) {
				OsgiFrameworkUtil.safeUnregisterService(entry.getValue());
				// persistenceUnits.get(entry.getKey()).unregistered();
			}
			// remember to set registrations to be null
			registrations = null;
		}
	}

	private void unregisterEntityManagerFactory(String unit) {
		if (registrations != null) {
			OsgiFrameworkUtil.safeUnregisterService(registrations.remove(unit));
			// persistenceUnits.get(unit).unregistered();
		}
	}

	/**
	 * Register {@link EntityManagerFactory} services
	 * 
	 * @throws InvalidPersistenceUnitException
	 *             if this {@link EntityManagerFactory} is no longer valid and
	 *             should be destroyed
	 */
	private void registerEntityManagerFactories()
			throws InvalidPersistenceUnitException {
		// Only register if there is a provider and we are not
		// quiescing
		if (registrations == null) {
			registrations = new ConcurrentHashMap<String, ServiceRegistration>();
		}

		if (provider != null) {
					
			// Make sure the EntityManagerFactories are instantiated
			createEntityManagerFactories();
			
			_logger.log(Level.FINE,
					"createEntityManagerFactories --finished");
		
			String providerName = (String) provider
					.getProperty("javax.persistence.provider");
			if (providerName == null) {
				_logger.log(Level.WARNING,
						"no.provider.specified" + bundle.getSymbolicName()
								+ '/' + provider);
			}
			// Register each EMF
			for (Entry<String, EntityManagerFactory> entry : emfs.entrySet()) {

				Hashtable<String, Object> props = new Hashtable<String, Object>();
				String unitName = entry.getKey();

				if (registrations.containsKey(unitName)) {
					_logger.log(Level.FINE,
							"registerEntityManagerFactories ---- already  registered");
					continue;
				}

				props.put(ParsedPersistenceUnit.OSGI_UNIT_NAME, unitName);
				if (providerName != null)
					props.put(ParsedPersistenceUnit.OSGI_UNIT_PROVIDER,
							providerName);

				props.put(ParsedPersistenceUnit.OSGI_UNIT_VERSION,
						bundle.getVersion());
				props.put(
						ParsedPersistenceUnit.CONTAINER_MANAGED_PERSISTENCE_UNIT,
						Boolean.TRUE);
				props.put(ParsedPersistenceUnit.EMPTY_PERSISTENCE_UNIT_NAME,
						"".equals(unitName));
				try {
					registrations.put(
							unitName,
							bundle.getBundleContext().registerService(
									EntityManagerFactory.class
											.getCanonicalName(),
									entry.getValue(), props));
					// persistenceUnits.get(unitName).registered();
					_logger.log(Level.FINE, "registered persistence unit"
							+ unitName + bundle.getSymbolicName() + '/'
							+ bundle.getVersion());
				} catch (Exception e) {
					_logger.log(
							Level.SEVERE,
							"cannot.register.persistence.unit" + unitName
									+ bundle.getSymbolicName() + '/'
									+ bundle.getVersion());
					throw new InvalidPersistenceUnitException();
				}
			}
		}else {
			_logger.log(Level.SEVERE,
					"no provider found " + bundle.getSymbolicName()
							+ '/' + provider);
		}
	}

	/**
	 * Create {@link EntityManagerFactory} services for this peristence unit
	 * throws InvalidPersistenceUnitException if this
	 * {@link EntityManagerFactory} is no longer valid and should be destroyed
	 */
	private void createEntityManagerFactories()
			throws InvalidPersistenceUnitException {
		if (emfs == null) {
			emfs = new HashMap<String, EntityManagerFactory>();
		}
		// Only try if we have a provider and EMFs
		if (provider == null || !emfs.isEmpty()) {
			_logger.log(Level.FINE,
					"createEntityManagerFactories ----provider == null || !emfs.isEmpty()");
			
			return;
		}
		try {
			// Get hold of the provider
			PersistenceProvider providerService = (PersistenceProvider) containerContext
					.getService(provider);
			
			_logger.log(Level.FINE,
					"createEntityManagerFactories --got -PersistenceProvider ");
			
			if (providerService == null) {
				_logger.log(Level.WARNING, "persistence.provider.gone.awol"
						+ bundle.getSymbolicName() + '/' + bundle.getVersion());
				throw new InvalidPersistenceUnitException();
			}

			for (String unitName : persistenceUnits.keySet()) {
				PersistenceUnitInfo mpui = persistenceUnits.get(unitName);
				
				try {
					Map<String, Object> props = new HashMap<String, Object>();
					props.put(ParsedPersistenceUnit.USE_DATA_SOURCE_FACTORY,
							"false");

					_logger.log(Level.FINE,
							"createEntityManagerFactories -- starting createContainerEntityManagerFactory ");
					
					EntityManagerFactory emf = providerService
							.createContainerEntityManagerFactory(mpui, props);
					_logger.log(Level.FINE,
							"createEntityManagerFactories -- finished createContainerEntityManagerFactory ");
					
					emfs.put(unitName, emf);

				} catch (Exception e) {
					_logger.log(Level.WARNING,
							"Error creating EntityManagerFactory", e);
				}
			}
		} finally {
			// Remember to unget the provider
			containerContext.ungetService(provider);
		}
	}

	/**
	 * Manage the EntityManagerFactories for the following provider and
	 * {@link PersistenceUnitInfo}s
	 * 
	 * This method should only be called when not holding any locks
	 * 
	 * @param ref
	 *            The {@link PersistenceProvider} {@link ServiceReference}
	 * @param infos
	 *            The {@link PersistenceUnitInfo}s defined by our bundle
	 */
	public synchronized void manage(ServiceReference ref,
			Collection<PersistenceUnitInfo> infos) throws IllegalStateException {
		provider = ref;
		persistenceUnits = getInfoMap(infos);
	}

	/**
	 * Manage the EntityManagerFactories for the following provider, updated
	 * persistence xmls and {@link PersistenceUnitInfo}s
	 * 
	 * This method should only be called when not holding any locks
	 * 
	 * @param parsedUnits
	 *            The updated {@link ParsedPersistenceUnit}s for this bundle
	 * @param ref
	 *            The {@link PersistenceProvider} {@link ServiceReference}
	 * @param infos
	 *            The {@link PersistenceUnitInfo}s defined by our bundle
	 */
	public synchronized void manage(
			Collection<ParsedPersistenceUnit> parsedUnits,
			ServiceReference ref, Collection<PersistenceUnitInfo> infos)
			throws IllegalStateException {
		parsedData = parsedUnits;
		provider = ref;
		persistenceUnits = getInfoMap(infos);
	}

	/**
	 * Stop managing any {@link EntityManagerFactory}s
	 * 
	 * This method should only be called when not holding any locks
	 */
	public synchronized void destroy() {
		destroyEntityManagerFactories();

		provider = null;
		persistenceUnits = null;
	}

	/**
	 * S
	 */
	private void destroyEntityManagerFactories() {
		if (registrations != null)
			unregisterEntityManagerFactories();
		if (emfs != null) {
			for (Entry<String, EntityManagerFactory> entry : emfs.entrySet()) {
				try {
					entry.getValue().close();
				} catch (Exception e) {
					_logger.log(
							Level.SEVERE,
							"could.not.close.persistence.unit" + entry.getKey()
									+ bundle.getSymbolicName() + '/'
									+ bundle.getVersion(), e);
				}
				// FIXME destroy EntityManagerFactories services
			}
		}
		emfs = null;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public Collection<ParsedPersistenceUnit> getParsedPersistenceUnits() {
		return parsedData;
	}

}
