package orchastack.core.jpa.container;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;

/**
 * This interface provides access to the information defined by a persistence
 * unit in a persistence descriptor.
 * 
 * Implementations of this interface will be returned by calls to
 * {@link PersistenceDescriptorParser}.
 */
public interface ParsedPersistenceUnit {
	/*
	 * Keys for use in the PersistenceXml Map Stored values are Strings unless
	 * otherwise specified, and all values other than the schema version and
	 * unit name may be null. A null value indicates that the element/attribute
	 * was not present in the xml.
	 */

	/** The service property key mapped to the persistence unit name */
	public static final String OSGI_UNIT_NAME = "osgi.unit.name";
	/** The version of the persistence bundle. */
	public static final String OSGI_UNIT_VERSION = "osgi.unit.version";
	/**
	 * The service property key mapped to the {@link PersistenceProvider}
	 * implementation class name
	 */
	public static final String OSGI_UNIT_PROVIDER = "osgi.unit.provider";
	/**
	 * The service property key mapped to a Boolean indicating whether this
	 * persistence unit is container managed
	 */
	public static final String CONTAINER_MANAGED_PERSISTENCE_UNIT = "jpa.container.managed";
	/**
	 * The service property key mapped to a Boolean indicating whether this
	 * persistence unit has the default (empty string) unit name This allows
	 * clients to filter for empty string persistence unit names.
	 */
	public static final String EMPTY_PERSISTENCE_UNIT_NAME = "jpa.default.unit.name";

	/**
	 * This property determines whether the Aries JPA container should monitor
	 * for DataSourceFactories and only register the EMF when the DataSource is
	 * available
	 */
	public static final String USE_DATA_SOURCE_FACTORY = "jpa.use.data.source.factory";

	/**
	 * This property name is used to store the JDBC driver class name when using
	 * DataSourceFactory integration
	 */
	public static final String DATA_SOURCE_FACTORY_CLASS_NAME = "jpa.data.source.factory.class";

	/** The version of the JPA schema being used */
	public static final String SCHEMA_VERSION = "jpa.schema.version";
	/** The name of the persistence unit */
	public static final String UNIT_NAME = "jpa.unit.name";
	/** The Transaction type of the persistence unit */
	public static final String TRANSACTION_TYPE = "jpa.transaction.type";
	/** A {@link List} of {@link String} mapping file names */
	public static final String MAPPING_FILES = "jpa.mapping.files";
	/** A {@link List} of {@link String} jar file names */
	public static final String JAR_FILES = "jpa.jar.files";
	/** A {@link List} of {@link String} managed class names */
	public static final String MANAGED_CLASSES = "jpa.managed.classes";
	/**
	 * A {@link Properties} object containing the properties from the
	 * persistence unit
	 */
	public static final String PROPERTIES = "jpa.properties";
	/** The provider class name */
	public static final String PROVIDER_CLASSNAME = "jpa.provider";
	/** The jta-datasource name */
	public static final String JTA_DATASOURCE = "jpa.jta.datasource";
	/** The non-jta-datasource name */
	public static final String NON_JTA_DATASOURCE = "jpa.non.jta.datasource";
	/** A {@link Boolean} indicating whether unlisted classes should be excluded */
	public static final String EXCLUDE_UNLISTED_CLASSES = "jpa.exclude.unlisted";

	/* JPA 2 extensions */

	/**
	 * The caching type of the persistence unit. This will only be available for
	 * JPA2 persistence units.
	 */
	public static final String SHARED_CACHE_MODE = "jpa2.shared.cache.mode";
	/**
	 * The validation mode of the persistence unit. This will only be available
	 * for JPA2 persistence units.
	 */
	public static final String VALIDATION_MODE = "jpa2.validation.mode";

	/* End of Map keys */

	/**
	 * This property is used in the JPA properties to indicate a provider
	 * version range
	 */
	public static final String JPA_PROVIDER_VERSION = "jpa.provider.version";

	/**
	 * Return the persistence bundle that defines this persistence unit
	 * 
	 * @return the defining bundle
	 */
	public Bundle getDefiningBundle();

	/**
	 * Returns a deep copy of the persistence metadata, modifications to the
	 * returned {@link Map} will not be reflected in future calls.
	 * 
	 * @return the metadata
	 */
	public Map<String, Object> getPersistenceXmlMetadata();
}
