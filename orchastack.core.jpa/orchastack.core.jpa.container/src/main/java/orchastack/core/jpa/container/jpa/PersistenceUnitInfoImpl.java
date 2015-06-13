package orchastack.core.jpa.container.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import orchastack.core.jpa.container.ParsedPersistenceUnit;
import orchastack.core.jpa.container.util.OsgiFrameworkUtil;
import orchastack.core.jpa.container.util.TempBundleDelegatingClassLoader;
import orchastack.core.jpa.container.weaving.TransformerRegistry;
import orchastack.core.jpa.container.weaving.TransformerRegistryFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("rawtypes")
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

	private final Bundle bundle;

	private final ParsedPersistenceUnit unit;

	private final ServiceReference providerRef;

	private final Boolean useDataSourceFactory;

	private ClassTransformer transformer;

	// initialize it lazily because we create a PersistenceUnitInfoImpl when the
	// bundle is INSTALLED state
	private final AtomicReference<ClassLoader> cl = new AtomicReference<ClassLoader>();

	/** Logger */
	private static final Logger _logger = Logger
			.getLogger(PersistenceUnitInfoImpl.class.getName());

	private static final String JDBC_PREFIX = "javax.persistence.jdbc.";

	public PersistenceUnitInfoImpl(Bundle b, ParsedPersistenceUnit parsedData,
			final ServiceReference providerRef,
			Boolean globalUsedatasourcefactory) {
		bundle = b;
		unit = parsedData;
		this.providerRef = providerRef;
		// Local override for global DataSourceFactory usage
		Boolean localUseDataSourceFactory = Boolean
				.parseBoolean(getInternalProperties().getProperty(
						ParsedPersistenceUnit.USE_DATA_SOURCE_FACTORY, "true"));

		this.useDataSourceFactory = globalUsedatasourcefactory
				&& localUseDataSourceFactory;
	}

	public synchronized void addTransformer(ClassTransformer arg0) {
		TransformerRegistry reg = TransformerRegistryFactory
				.getTransformerRegistry();
		if (reg != null) {
			reg.addTransformer(bundle, arg0, providerRef);
			transformer = arg0;
		}
	}

	public boolean internalExcludeUnlistedClasses() {
		Boolean result = (Boolean) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.EXCLUDE_UNLISTED_CLASSES);
		return (result == null) ? false : result;
	}

	public boolean excludeUnlistedClasses() {
		return true;
	}

	public ClassLoader getClassLoader() {
		if (cl.get() == null) {
			// use forced because for even for a resolved bundle we could
			// otherwise get null
			cl.compareAndSet(null, OsgiFrameworkUtil.getClassLoader(bundle));
		}

		return cl.get();
	}

	@SuppressWarnings("unchecked")
	public List<URL> getJarFileUrls() {
		List<String> jarFiles = (List<String>) unit.getPersistenceXmlMetadata()
				.get(ParsedPersistenceUnit.JAR_FILES);
		List<URL> urls = new ArrayList<URL>();
		if (jarFiles != null) {
			for (String jarFile : jarFiles) {
				URL url = bundle.getResource(jarFile);
				if (url == null) {
					_logger.log(Level.SEVERE, "pu.not.found, "
							+ getPersistenceUnitName());
				} else {
					urls.add(url);
				}
			}
		}
		return urls;
	}

	public DataSource getJtaDataSource() {
		String jndiString = (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.JTA_DATASOURCE);
		JndiDataSourceHelper toReturn = null;
		if (jndiString != null) {
			toReturn = new JndiDataSourceHelper(jndiString,
					getPersistenceUnitName(), bundle,
					getTransactionType() == PersistenceUnitTransactionType.JTA);
			return toReturn.getDs();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public List<String> getManagedClassNames() {
		List<String> classes = (List<String>) unit.getPersistenceXmlMetadata()
				.get(ParsedPersistenceUnit.MANAGED_CLASSES);
		if (classes == null)
			classes = new ArrayList<String>();
		if (!!!internalExcludeUnlistedClasses()) {
			JPAAnnotationScanner scanner = JPAAnnotationScanner.getInstance();
			if (scanner != null)
				classes.addAll(scanner.findJPAAnnotatedClasses(bundle));
		}

		return Collections.unmodifiableList(classes);
	}

	@SuppressWarnings("unchecked")
	public List<String> getMappingFileNames() {
		List<String> mappingFiles = (List<String>) unit
				.getPersistenceXmlMetadata().get(
						ParsedPersistenceUnit.MAPPING_FILES);
		if (mappingFiles == null)
			mappingFiles = new ArrayList<String>();

		return Collections.unmodifiableList(mappingFiles);
	}

	public ClassLoader getNewTempClassLoader() {
		ClassLoader cl = OsgiFrameworkUtil.getClassLoader(providerRef
				.getBundle());
		return new TempBundleDelegatingClassLoader(bundle, cl);
	}

	public DataSource getNonJtaDataSource() {

		String jndiString = (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.NON_JTA_DATASOURCE);
		JndiDataSourceHelper toReturn = null;
		if (jndiString != null) {
			toReturn = new JndiDataSourceHelper(jndiString,
					getPersistenceUnitName(), bundle, false);
			return toReturn.getDs();
		}

		return null;

	}

	public String getPersistenceProviderClassName() {
		return (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.PROVIDER_CLASSNAME);
	}

	public String getPersistenceUnitName() {
		return (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.UNIT_NAME);
	}

	public URL getPersistenceUnitRootUrl() {
		return bundle.getResource("/");
	}

	public String getPersistenceXMLSchemaVersion() {
		return (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.SCHEMA_VERSION);
	}

	private Properties getInternalProperties() {
		return (Properties) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.PROPERTIES);
	}

	public Properties getProperties() {
		Properties p = new Properties();
		p.putAll(getInternalProperties());

		String jdbcClass = p.getProperty("javax.persistence.jdbc.driver");
		if (useDataSourceFactory && jdbcClass != null) {
			p.setProperty(ParsedPersistenceUnit.DATA_SOURCE_FACTORY_CLASS_NAME,
					jdbcClass);
			p.remove("javax.persistence.jdbc.driver");
		}
		return p;
	}

	public SharedCacheMode getSharedCacheMode() {
		String s = (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.SHARED_CACHE_MODE);

		if (s == null)
			return SharedCacheMode.UNSPECIFIED;
		else
			return SharedCacheMode.valueOf(s);
	}

	public PersistenceUnitTransactionType getTransactionType() {

		String s = (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.TRANSACTION_TYPE);

		if (s == null)
			return PersistenceUnitTransactionType.JTA;
		else
			return PersistenceUnitTransactionType.valueOf(s);
	}

	public ValidationMode getValidationMode() {
		String s = (String) unit.getPersistenceXmlMetadata().get(
				ParsedPersistenceUnit.VALIDATION_MODE);

		if (s == null)
			return ValidationMode.AUTO;
		else
			return ValidationMode.valueOf(s);

	}

	public synchronized void clearUp() {
		if (transformer != null) {
			TransformerRegistry reg = TransformerRegistryFactory
					.getTransformerRegistry();
			reg.removeTransformer(bundle, transformer);
			transformer = null;
		}
	}

}