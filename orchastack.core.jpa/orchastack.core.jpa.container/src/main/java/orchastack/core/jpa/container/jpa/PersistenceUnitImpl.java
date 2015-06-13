package orchastack.core.jpa.container.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import orchastack.core.jpa.container.ParsedPersistenceUnit;

import org.osgi.framework.Bundle;

/**
 * An implementation of PersistenceUnit for parsed persistence unit metadata
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PersistenceUnitImpl implements ParsedPersistenceUnit {
	/** A map to hold the metadata from the xml */
	private final Map<String, Object> metadata = new HashMap<String, Object>();
	/** The bundle defining this persistence unit */
	private final Bundle bundle;

	/**
	 * Create a new persistence unit with the given name, transaction type,
	 * location and defining bundle
	 * 
	 * @param name
	 *            may be null
	 * @param transactionType
	 *            may be null
	 * @param location
	 * @param version
	 *            The version of the JPA schema used in persistence.xml
	 */
	public PersistenceUnitImpl(Bundle b, String name, String transactionType,
			String version) {
		this.bundle = b;
		metadata.put(SCHEMA_VERSION, version);
		metadata.put(PROPERTIES, new Properties());

		if (name == null)
			name = "";

		metadata.put(UNIT_NAME, name);
		if (transactionType != null)
			metadata.put(TRANSACTION_TYPE, transactionType);

	}

	public Bundle getDefiningBundle() {
		return bundle;
	}

	public Map<String, Object> getPersistenceXmlMetadata() {
		Map<String, Object> data = new HashMap<String, Object>(metadata);
		if (data.containsKey(MAPPING_FILES))
			data.put(MAPPING_FILES,
					((ArrayList) metadata.get(MAPPING_FILES)).clone());
		if (data.containsKey(JAR_FILES))
			data.put(JAR_FILES, ((ArrayList) metadata.get(JAR_FILES)).clone());
		if (data.containsKey(MANAGED_CLASSES))
			data.put(MANAGED_CLASSES,
					((ArrayList) metadata.get(MANAGED_CLASSES)).clone());
		if (data.containsKey(PROPERTIES))
			data.put(PROPERTIES,
					((Properties) metadata.get(PROPERTIES)).clone());

		return data;
	}

	/**
	 * @param provider
	 */
	public void setProviderClassName(String provider) {
		metadata.put(PROVIDER_CLASSNAME, provider);
	}

	/**
	 * @param jtaDataSource
	 */
	public void setJtaDataSource(String jtaDataSource) {
		metadata.put(JTA_DATASOURCE, jtaDataSource);
	}

	/**
	 * @param nonJtaDataSource
	 */
	public void setNonJtaDataSource(String nonJtaDataSource) {
		metadata.put(NON_JTA_DATASOURCE, nonJtaDataSource);
	}

	/**
	 * @param mappingFileName
	 */
	public void addMappingFileName(String mappingFileName) {
		List<String> files = (List<String>) metadata.get(MAPPING_FILES);
		if (files == null) {
			files = new ArrayList<String>();
			metadata.put(MAPPING_FILES, files);
		}
		files.add(mappingFileName);
	}

	/**
	 * @param jarFile
	 */
	public void addJarFileName(String jarFile) {
		List<String> jars = (List<String>) metadata.get(JAR_FILES);
		if (jars == null) {
			jars = new ArrayList<String>();
			metadata.put(JAR_FILES, jars);
		}

		jars.add(jarFile);
	}

	/**
	 * @param className
	 */
	public void addClassName(String className) {
		List<String> classes = (List<String>) metadata.get(MANAGED_CLASSES);
		if (classes == null) {
			classes = new ArrayList<String>();
			metadata.put(MANAGED_CLASSES, classes);
		}
		classes.add(className);
	}

	/**
	 * @param exclude
	 */
	public void setExcludeUnlisted(boolean exclude) {
		metadata.put(EXCLUDE_UNLISTED_CLASSES, exclude);
	}

	/**
	 * @param name
	 * @param value
	 */
	public void addProperty(String name, String value) {
		Properties props = (Properties) metadata.get(PROPERTIES);
		props.setProperty(name, value);
	}

	/**
	 * @param sharedCacheMode
	 */
	public void setSharedCacheMode(String sharedCacheMode) {
		metadata.put(SHARED_CACHE_MODE, sharedCacheMode);
	}

	/**
	 * @param validationMode
	 */
	public void setValidationMode(String validationMode) {
		metadata.put(VALIDATION_MODE, validationMode);
	}

	public String toString() {
		return "Persistence unit " + metadata.get(UNIT_NAME) + " in bundle "
				+ bundle.getSymbolicName() + "_" + bundle.getVersion();
	}
}
