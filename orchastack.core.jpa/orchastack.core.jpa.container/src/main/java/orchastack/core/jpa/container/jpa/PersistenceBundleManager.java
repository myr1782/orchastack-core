package orchastack.core.jpa.container.jpa;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import orchastack.core.jpa.container.ManagedPersistenceUnitInfoFactory;
import orchastack.core.jpa.container.ParsedPersistenceUnit;
import orchastack.core.jpa.container.PersistenceDescriptor;
import orchastack.core.jpa.container.tx.OSGiTransactionManager;
import orchastack.core.jpa.container.util.OsgiFrameworkUtil;
import orchastack.core.jpa.container.util.RecursiveBundleTracker;
import orchastack.core.jpa.container.util.VersionRange;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This class locates, parses and manages persistence units defined in OSGi
 * bundles. It also keeps track of PersistenceProvider services and delegates
 * the EMF creation to the matching PersistenceProvider
 */
@SuppressWarnings("rawtypes")
public class PersistenceBundleManager implements BundleTrackerCustomizer,
		ServiceTrackerCustomizer, BundleActivator {
	/** Logger */
	private static final Logger _logger = Logger
			.getLogger(PersistenceBundleManager.class.getName());

	/** The bundle context for this bundle */
	private BundleContext ctx = null;

	/**
	 * A map of providers to persistence bundles this is used to guarantee that
	 * when a provider service is removed we can access all of the bundles that
	 * might possibly be using it. The map should only ever be accessed when
	 * synchronized on {@code this}.
	 */
	private final Map<Bundle, EntityManagerFactoryManager> bundleToManagerMap = new HashMap<Bundle, EntityManagerFactoryManager>();
	/**
	 * The PersistenceProviders. The Set should only ever be accessed when
	 * synchronized on {@code this}. Use a Set for constant access and add
	 * times.
	 */
	private Set<ServiceReference> persistenceProviders = new HashSet<ServiceReference>();
	/**
	 * Managers that do not have a suitable provider yet should only ever be
	 * accessed when synchronized on {@code this} Use a set so we don't have to
	 * be careful about adding multiple times!
	 */
	private Collection<EntityManagerFactoryManager> managersAwaitingProviders = new ArrayList<EntityManagerFactoryManager>();
	/** Plug-point for persistence unit providers */
	private ManagedPersistenceUnitInfoFactory persistenceUnitFactory;
	/** Parser for persistence descriptors */
	private PersistenceDescriptorParser parser;
	/** Registration for the Parser */
	private ServiceRegistration parserReg;
	/** Configuration for this extender */
	private Properties config;
	private RecursiveBundleTracker tracker;
	private ServiceTracker serviceTracker;

	@SuppressWarnings("unchecked")
	private void open() {
		// Create the pluggable ManagedPersistenceUnitInfoFactory
		String className = config
				.getProperty(ManagedPersistenceUnitInfoFactory.DEFAULT_PU_INFO_FACTORY_KEY);

		if (className != null) {
			try {
				Class<? extends ManagedPersistenceUnitInfoFactory> clazz = (Class<? extends ManagedPersistenceUnitInfoFactory>) ctx
						.getBundle().loadClass(className);
				persistenceUnitFactory = clazz.newInstance();
			} catch (Exception e) {
				_logger.log(Level.SEVERE,
						"unable.to.create.mpuif " + className, e);
			}
		}

		if (persistenceUnitFactory == null)
			persistenceUnitFactory = new ManagedPersistenceUnitInfoFactoryImpl();
		serviceTracker.open();
		tracker.open();

		OSGiTransactionManager.init(ctx);
	}

	private void close() {
		if (tracker != null) {
			tracker.close();
		}

		if (serviceTracker != null) {
			serviceTracker.close();
		}

		OSGiTransactionManager.destroy();
	}

	public Object addingBundle(Bundle bundle, BundleEvent event) {
		// _logger.log(Level.INFO, "bundle gound " + bundle);
		return setupManager(bundle, null, true);

	}

	/**
	 * A provider is being added, add it to our Set
	 * 
	 * @param ref
	 */
	public Object addingService(ServiceReference ref) {

		Map<EntityManagerFactoryManager, ServiceReference> managersToManage = new HashMap<EntityManagerFactoryManager, ServiceReference>();
		synchronized (this) {

			_logger.log(Level.FINE, "Adding a provider: {}",
					new Object[] { ref });

			persistenceProviders.add(ref);

			Iterator<EntityManagerFactoryManager> it = managersAwaitingProviders
					.iterator();
			while (it.hasNext()) {
				EntityManagerFactoryManager mgr = it.next();
				ServiceReference reference = getProviderServiceReference(mgr
						.getParsedPersistenceUnits());
				if (reference != null) {
					managersToManage.put(mgr, reference);
					it.remove();
				}
			}
		}

		for (Entry<EntityManagerFactoryManager, ServiceReference> entry : managersToManage
				.entrySet()) {
			EntityManagerFactoryManager mgr = entry.getKey();
			ServiceReference reference = entry.getValue();
			Collection<PersistenceUnitInfo> infos = null;
			try {
				infos = persistenceUnitFactory
						.createManagedPersistenceUnitMetadata(ctx,
								mgr.getBundle(), reference,
								mgr.getParsedPersistenceUnits());

				mgr.manage(reference, infos);
				mgr.bundleStateChange();
			} catch (Exception e) {
				if (e instanceof InvalidPersistenceUnitException) {
					logInvalidPersistenceUnitException(mgr.getBundle(),
							(InvalidPersistenceUnitException) e);
				} else {
					_logger.log(Level.WARNING, "unable.to.manage.pu,"
							+ mgr.getBundle().getSymbolicName(), e);
				}
				mgr.destroy();
				if (infos != null)
					persistenceUnitFactory.destroyPersistenceBundle(ctx,
							mgr.getBundle());

				// Something better may have come along while we weren't
				// synchronized
				setupManager(mgr.getBundle(), mgr, false);
			}
		}
		return ref;
	}

	/**
	 * A provider is being removed, remove it from the set, and notify all
	 * managers that it has been removed
	 * 
	 * @param ref
	 */
	public void removedService(ServiceReference ref, Object o) {

		_logger.log(Level.FINE, "Removing a provider:" + ref);

		Map<Bundle, EntityManagerFactoryManager> mgrs;
		synchronized (this) {
			persistenceProviders.remove(ref);
			mgrs = new HashMap<Bundle, EntityManagerFactoryManager>(
					bundleToManagerMap);
		}
		// If the entry is removed then make sure we notify the
		// persistenceUnitFactory
		for (Entry<Bundle, EntityManagerFactoryManager> entry : mgrs.entrySet()) {
			EntityManagerFactoryManager mgr = entry.getValue();
			if (mgr.providerRemoved(ref)) {
				Bundle bundle = entry.getKey();
				persistenceUnitFactory.destroyPersistenceBundle(ctx, bundle);
				// Allow the manager to re-initialize with a new provider
				// No change to the units
				setupManager(bundle, mgr, false);
			}
		}
	}

	/**
	 * Add config properties, making sure to read in the properties file and
	 * override the supplied properties
	 * 
	 * @param props
	 */
	private void initConfig() {
		config = new Properties();
		URL u = ctx.getBundle().getResource(
				ManagedPersistenceUnitInfoFactory.JPA_CONTAINER_PROPERTIES);

		if (u != null) {
			_logger.log(Level.INFO, "aries.jpa.config.file.found");
			try {
				config.load(u.openStream());
			} catch (IOException e) {
				_logger.log(Level.SEVERE, "aries.jpa.config.file.read.error", e);
			}
		} else {
			_logger.log(Level.WARNING, "aries.jpa.config.file.not.found");
		}
	}

	public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {

		EntityManagerFactoryManager mgr = (EntityManagerFactoryManager) object;
		// If the bundle was updated we need to destroy it and re-initialize
		// the EntityManagerFactoryManager
		// If the bundle becomes unresolved we need to destroy persistenceUnits,
		// since they
		// keep a reference to bundle classloader which has wiring m_isDisposed
		// set to true
		// this occurs when Karaf BundleWatcher is used.

		_logger.log(Level.FINE,
				"Bundle " + bundle + " state changed - " + event.getType());

		if (event != null
				&& (event.getType() == BundleEvent.UPDATED || event.getType() == BundleEvent.UNRESOLVED)) {
			mgr.destroy();
			persistenceUnitFactory.destroyPersistenceBundle(ctx, bundle);
			if (event.getType() == BundleEvent.UPDATED) {
				// Don't add to the managersAwaitingProviders, the setupManager
				// will do it
				setupManager(bundle, mgr, true);
			}
		} else {
			try {
				boolean reassign;
				synchronized (this) {
					reassign = managersAwaitingProviders.contains(mgr);
				}
				if (reassign) {
					setupManager(bundle, mgr, false);
				} else {
					mgr.bundleStateChange();
				}
			} catch (InvalidPersistenceUnitException e) {
				logInvalidPersistenceUnitException(bundle, e);
				mgr.destroy();
				persistenceUnitFactory.destroyPersistenceBundle(ctx, bundle);

				// Try re-initializing the manager immediately, this wasn't an
				// update so the units don't need to be re-parsed
				setupManager(bundle, mgr, false);
			}
		}
	}

	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		EntityManagerFactoryManager mgr = (EntityManagerFactoryManager) object;
		mgr.destroy();
		if (!managersAwaitingProviders.contains(mgr)) {
			persistenceUnitFactory.destroyPersistenceBundle(ctx, bundle);
		}
		// Remember to tidy up the map
		synchronized (this) {
			bundleToManagerMap.remove(bundle);
		}
	}

	private Collection<ParsedPersistenceUnit> parseBundle(Bundle b) {

		Collection<ParsedPersistenceUnit> pUnits = new ArrayList<ParsedPersistenceUnit>();

		Collection<PersistenceDescriptor> persistenceXmls = PersistenceBundleHelper
				.findPersistenceXmlFiles(b);

		// If we have no persistence units then our job is done
		if (!!!persistenceXmls.isEmpty()) {

			_logger.log(Level.FINE,
					"Located Persistence descriptors: {} in bundle {}",
					new Object[] { persistenceXmls,
							b.getSymbolicName() + "_" + b.getVersion() });

			if (b.getState() == Bundle.ACTIVE) {
				_logger.log(Level.WARNING, "jpa.bundle.active",
						b.getSymbolicName());
			}

			// Parse each descriptor
			for (PersistenceDescriptor descriptor : persistenceXmls) {
				try {
					pUnits.addAll(parser.parse(b, descriptor));
				} catch (PersistenceDescriptorParserException e) {
					_logger.log(Level.SEVERE,
							"persistence.description.parse.error, "
									+ descriptor.getLocation(), e);
				}
			}
		}
		return pUnits;
	}

	/**
	 * Set up an {@link EntityManagerFactoryManager} for the supplied bundle
	 * 
	 * @param bundle
	 *            The bundle
	 * @param mgr
	 *            The previously existing {@link EntityManagerFactoryManager} or
	 *            {@code null} if none existed
	 * @return The manager to use, or null if no persistence units can be
	 *         managed for this bundle
	 */
	private EntityManagerFactoryManager setupManager(Bundle bundle,
			EntityManagerFactoryManager mgr, boolean reParse) {

		Collection<ParsedPersistenceUnit> pUnits = (mgr == null || reParse) ? parseBundle(bundle)
				: mgr.getParsedPersistenceUnits();

		// If we have any persistence units then find a provider to use
		if (!!!pUnits.isEmpty()) {
			_logger.log(Level.INFO, "Located Persistence units: " + pUnits);

			ServiceReference ref = getProviderServiceReference(pUnits);
			// If we found a provider then create the ManagedPersistenceUnitInfo
			// objects
			Collection<PersistenceUnitInfo> infos = null;
			if (ref != null) {
				infos = persistenceUnitFactory
						.createManagedPersistenceUnitMetadata(ctx, bundle, ref,
								pUnits);
			}

			if (mgr == null) {
				mgr = new EntityManagerFactoryManager(ctx, bundle);
			}
			mgr.manage(pUnits, ref, infos);

			// Register the manager (this may re-add, but who cares)
			synchronized (this) {
				bundleToManagerMap.put(bundle, mgr);
				// If the provider is gone then we need to wait
				if (ref == null) {
					managersAwaitingProviders.add(mgr);
				}
			}

			// prod the manager to get it into the right state
			try {
				mgr.bundleStateChange();
			} catch (InvalidPersistenceUnitException e) {
				logInvalidPersistenceUnitException(bundle, e);
				mgr.destroy();
				if (infos != null)
					persistenceUnitFactory
							.destroyPersistenceBundle(ctx, bundle);
				// Put the manager into the list of managers waiting for a new
				// provider, one that might work!
				synchronized (this) {
					managersAwaitingProviders.add(mgr);
				}
			}
		}
		return mgr;
	}

	/**
	 * Get a persistence provider from the service registry described by the
	 * persistence units defined
	 * 
	 * @param parsedPersistenceUnits
	 * @return A service reference or null if no suitable reference is available
	 */
	private synchronized ServiceReference getProviderServiceReference(
			Collection<ParsedPersistenceUnit> parsedPersistenceUnits) {
		Set<String> ppClassNames = new HashSet<String>();
		List<VersionRange> versionRanges = new ArrayList<VersionRange>();
		// Fill the set of class names and version Filters
		for (ParsedPersistenceUnit unit : parsedPersistenceUnits) {
			Map<String, Object> metadata = unit.getPersistenceXmlMetadata();
			String provider = (String) metadata
					.get(ParsedPersistenceUnit.PROVIDER_CLASSNAME);
			// get providers specified in the persistence units
			if (provider != null && !!!provider.equals("")) {
				ppClassNames.add(provider);

				Properties props = (Properties) metadata
						.get(ParsedPersistenceUnit.PROPERTIES);

				if (props != null
						&& props.containsKey(ParsedPersistenceUnit.JPA_PROVIDER_VERSION)) {

					String versionRangeString = props
							.getProperty(
									ParsedPersistenceUnit.JPA_PROVIDER_VERSION,
									"0.0.0");
					try {
						versionRanges.add(VersionRange
								.parseVersionRange(versionRangeString));
					} catch (IllegalArgumentException e) {
						_logger.log(Level.SEVERE,
								"version.range.parse.failure "
										+ versionRangeString,
								metadata.get(ParsedPersistenceUnit.UNIT_NAME));
					}
				}
			}
		}
		// If we have too many provider class names or incompatible version
		// ranges specified then blow up

		VersionRange range = null;
		if (!!!versionRanges.isEmpty()) {
			try {
				range = combineVersionRanges(versionRanges);
			} catch (InvalidRangeCombination e) {
				Bundle bundle = parsedPersistenceUnits.iterator().next()
						.getDefiningBundle();
				_logger.log(Level.SEVERE, "invalid.provider.version.ranges "
						+ bundle.getSymbolicName(), e);
				return null;
			}
		}

		if (ppClassNames.size() > 1) {
			Bundle bundle = parsedPersistenceUnits.iterator().next()
					.getDefiningBundle();
			_logger.log(Level.SEVERE,
					"multiple.persistence.providers.specified",
					bundle.getSymbolicName());
			return null;
		} else {
			Bundle bundle = parsedPersistenceUnits.iterator().next()
					.getDefiningBundle();
			// Get the best provider for the given filters
			String provider = (ppClassNames.isEmpty()) ? persistenceUnitFactory
					.getDefaultProviderClassName() : ppClassNames.iterator()
					.next();
			return getBestProvider(provider, range, bundle);
		}
	}

	/**
	 * Turn a Collection of version ranges into a single range including common
	 * overlap
	 * 
	 * @param versionRanges
	 * @return
	 * @throws InvalidRangeCombination
	 */
	private VersionRange combineVersionRanges(List<VersionRange> versionRanges)
			throws InvalidRangeCombination {

		Version minVersion = new Version(0, 0, 0);
		Version maxVersion = null;
		boolean minExclusive = false;
		boolean maxExclusive = false;

		for (VersionRange range : versionRanges) {
			int minComparison = 0;
			try {
				minComparison = minVersion.compareTo(range.getMinimumVersion());
			} catch (Exception t) {
				// this is to catch a NoSuchMethodException that occurs due to a
				// building against a newer version of OSGi than
				// the one run with, need to call using Object version of method
				try {
					Method m = minVersion.getClass().getMethod("compareTo",
							Object.class);
					Object ret = m
							.invoke(minVersion, range.getMinimumVersion());
					minComparison = ((Integer) ret).intValue();
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(
							"PersistenceBundleManager.combineVersionRanges reflection compareTo failed",
							e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(
							"PersistenceBundleManager.combineVersionRanges reflection compareTo failed",
							e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(
							"PersistenceBundleManager.combineVersionRanges reflection compareTo failed",
							e);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(
							"PersistenceBundleManager.combineVersionRanges reflection compareTo failed",
							e);
				}
			}
			// If minVersion is smaller then we have a new, larger, minimum
			if (minComparison < 0) {
				minVersion = range.getMinimumVersion();
				minExclusive = range.isMinimumExclusive();
			}
			// Only update if it is the same version but more restrictive
			else if (minComparison == 0 && range.isMaximumExclusive())
				minExclusive = true;

			if (range.isMaximumUnbounded())
				continue;
			else if (maxVersion == null) {
				maxVersion = range.getMaximumVersion();
				maxExclusive = range.isMaximumExclusive();
			} else {
				int maxComparison = maxVersion.compareTo(range
						.getMaximumVersion());

				// We have a new, lower maximum
				if (maxComparison > 0) {
					maxVersion = range.getMaximumVersion();
					maxExclusive = range.isMaximumExclusive();
					// If the maximum is the same then make sure we set the
					// exclusivity properly
				} else if (maxComparison == 0 && range.isMaximumExclusive())
					maxExclusive = true;
			}
		}

		// Now check that we have valid values
		int check = (maxVersion == null) ? -1 : minVersion
				.compareTo(maxVersion);
		// If min is greater than max, or min is equal to max and one of the
		// exclusive
		// flags is set then we have a problem!
		if (check > 0 || (check == 0 && (minExclusive || maxExclusive))) {
			throw new InvalidRangeCombination(minVersion, minExclusive,
					maxVersion, maxExclusive);
		}

		// Turn the Versions into a version range string
		StringBuilder rangeString = new StringBuilder();
		rangeString.append(minVersion);

		if (maxVersion != null) {
			rangeString.insert(0, minExclusive ? "(" : "[");
			rangeString.append(",");
			rangeString.append(maxVersion);
			rangeString.append(maxExclusive ? ")" : "]");
		}
		// Turn that string back into a VersionRange
		return VersionRange.parseVersionRange(rangeString.toString());
	}

	/**
	 * Locate the best provider for the given criteria
	 * 
	 * @param providerClass
	 * @param matchingCriteria
	 * @param bundle
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private synchronized ServiceReference getBestProvider(String providerClass,
			VersionRange matchingCriteria, Bundle bundle) {
		if (!!!persistenceProviders.isEmpty()) {
			if ((providerClass != null && !!!"".equals(providerClass))
					|| matchingCriteria != null) {
				List<ServiceReference> refs = new ArrayList<ServiceReference>();
				for (ServiceReference reference : persistenceProviders) {

					if (providerClass != null
							&& !!!providerClass.equals(reference
									.getProperty("javax.persistence.provider")))
						continue;

					if (matchingCriteria == null
							|| matchingCriteria.matches(reference.getBundle()
									.getVersion()))
						refs.add(reference);
				}

				if (!!!refs.isEmpty()) {
					// Return the "best" provider, i.e. the highest version
					return Collections.max(refs,
							new ProviderServiceComparator());
				} else {
					_logger.log(Level.WARNING, "no.suitable.jpa.providers "
							+ providerClass + ", criteria " + matchingCriteria);
				}
			} else {
				// Return the "best" provider, i.e. the service OSGi would pick
				return (ServiceReference) Collections.max(persistenceProviders);
			}
		} else {
			_logger.log(Level.WARNING, "no.jpa.providers");
		}
		return null;
	}

	/**
	 * Sort the providers so that the highest version, highest ranked service is
	 * at the top
	 */
	private static class ProviderServiceComparator implements
			Comparator<ServiceReference> {
		public int compare(ServiceReference object1, ServiceReference object2) {
			Version v1 = object1.getBundle().getVersion();
			Version v2 = object2.getBundle().getVersion();
			int res = v1.compareTo(v2);
			if (res == 0) {
				Integer rank1 = (Integer) object1
						.getProperty(Constants.SERVICE_RANKING);
				Integer rank2 = (Integer) object2
						.getProperty(Constants.SERVICE_RANKING);
				if (rank1 != null && rank2 != null)
					res = rank1.compareTo(rank2);
			}
			return res;
		}
	}

	/**
	 * Log a warning to indicate that the Persistence units state will be
	 * destroyed
	 * 
	 * @param bundle
	 * @param e
	 */
	private void logInvalidPersistenceUnitException(Bundle bundle,
			InvalidPersistenceUnitException e) {
		_logger.log(Level.WARNING,
				"pu.has.becomd.invalid" + bundle.getSymbolicName(), e);
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// Just remove and re-add as the properties have changed
		removedService(reference, service);
		addingService(reference);
	}

	@SuppressWarnings("unchecked")
	public void start(BundleContext context) throws Exception {

		ctx = context;

		initConfig();
		initParser();

		serviceTracker = new ServiceTracker(ctx,
				PersistenceProvider.class.getName(), this);

		tracker = new RecursiveBundleTracker(ctx, Bundle.INSTALLED
				| Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE
				| Bundle.STOPPING, this);

		open();
	}

	private void initParser() {
		parser = new PersistenceDescriptorParser();
		parserReg = ctx.registerService(
				PersistenceDescriptorParser.class.getName(), parser, null);
	}

	public void stop(BundleContext context) throws Exception {
		close();
		OsgiFrameworkUtil.safeUnregisterService(parserReg);
	}

	public BundleContext getCtx() {
		return ctx;
	}

	public void quiesceBundle(Bundle bundleToQuiesce) {

		boolean thisBundle = bundleToQuiesce.equals(ctx.getBundle());

		Collection<EntityManagerFactoryManager> toDestroyNow = new ArrayList<EntityManagerFactoryManager>();
		final Collection<EntityManagerFactoryManager> quiesceNow = new ArrayList<EntityManagerFactoryManager>();
		synchronized (this) {
			if (thisBundle) {
				toDestroyNow.addAll(managersAwaitingProviders);
				managersAwaitingProviders.clear();
				quiesceNow.addAll(bundleToManagerMap.values());
				bundleToManagerMap.clear();
				quiesceNow.removeAll(toDestroyNow);
			} else {
				EntityManagerFactoryManager emfm = bundleToManagerMap
						.get(bundleToQuiesce);

				if (emfm != null) {
					if (managersAwaitingProviders.remove(emfm))
						toDestroyNow.add(emfm);
					else
						quiesceNow.add(emfm);
				}
			}
		}

		for (EntityManagerFactoryManager emfm : toDestroyNow)
			emfm.destroy();

	}
}
