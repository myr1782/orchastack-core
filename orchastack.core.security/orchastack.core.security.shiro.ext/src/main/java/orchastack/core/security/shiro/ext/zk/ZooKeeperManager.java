/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package orchastack.core.security.shiro.ext.zk;

import java.io.IOException;
import java.io.InputStream;

import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.io.ResourceUtils;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marc Ende <me@e-beyond.de>
 * @Since 1.2.2
 */
public class ZooKeeperManager implements CacheManager, Initializable,
		Destroyable {
	public static final Logger log = LoggerFactory
			.getLogger(ZooKeeperManager.class);

	/**
	 * The EhCache cache manager used by this implementation to create caches.
	 */
	protected net.sf.ehcache.CacheManager manager;

	/**
	 * Indicates if the CacheManager instance was implicitly/automatically
	 * created by this instance, indicating that it should be automatically
	 * cleaned up as well on shutdown.
	 */
	private boolean cacheManagerImplicitlyCreated = false;

	/**
	 * Classpath file location of the ehcache CacheManager config file.
	 */
	private String cacheManagerConfigFile = "classpath:orchastack/core/security/manager/zk/ehcache.xml";

	/**
	 * Returns the wrapped Ehcache {@link net.sf.ehcache.CacheManager
	 * CacheManager} instance.
	 * 
	 * @return the wrapped Ehcache {@link net.sf.ehcache.CacheManager
	 *         CacheManager} instance.
	 */
	public net.sf.ehcache.CacheManager getCacheManager() {
		return manager;
	}

	/**
	 * Sets the wrapped Ehcache {@link net.sf.ehcache.CacheManager CacheManager}
	 * instance.
	 * 
	 * @param manager
	 *            the wrapped Ehcache {@link net.sf.ehcache.CacheManager
	 *            CacheManager} instance.
	 */
	public void setCacheManager(net.sf.ehcache.CacheManager manager) {
		this.manager = manager;
	}

	/**
	 * Returns the resource location of the config file used to initialize a new
	 * EhCache CacheManager instance. The string can be any resource path
	 * supported by the
	 * {@link org.apache.shiro.io.ResourceUtils#getInputStreamForPath(String)}
	 * call.
	 * <p/>
	 * This property is ignored if the CacheManager instance is injected
	 * directly - that is, it is only used to lazily create a CacheManager if
	 * one is not already provided.
	 * 
	 * @return the resource location of the config file used to initialize the
	 *         wrapped EhCache CacheManager instance.
	 */
	public String getCacheManagerConfigFile() {
		return this.cacheManagerConfigFile;
	}

	/**
	 * Sets the resource location of the config file used to initialize the
	 * wrapped EhCache CacheManager instance. The string can be any resource
	 * path supported by the
	 * {@link org.apache.shiro.io.ResourceUtils#getInputStreamForPath(String)}
	 * call.
	 * <p/>
	 * This property is ignored if the CacheManager instance is injected
	 * directly - that is, it is only used to lazily create a CacheManager if
	 * one is not already provided.
	 * 
	 * @param classpathLocation
	 *            resource location of the config file used to create the
	 *            wrapped EhCache CacheManager instance.
	 */
	public void setCacheManagerConfigFile(String classpathLocation) {
		this.cacheManagerConfigFile = classpathLocation;
	}

	/**
	 * Acquires the InputStream for the ehcache configuration file using
	 * {@link ResourceUtils#getInputStreamForPath(String)
	 * ResourceUtils.getInputStreamForPath} with the path returned from
	 * {@link #getCacheManagerConfigFile() getCacheManagerConfigFile()}.
	 * 
	 * @return the InputStream for the ehcache configuration file.
	 */
	protected InputStream getCacheManagerConfigFileInputStream() {
		String configFile = getCacheManagerConfigFile();
		try {
			return ResourceUtils.getInputStreamForPath(configFile);
		} catch (IOException e) {
			throw new ConfigurationException(
					"Unable to obtain input stream for cacheManagerConfigFile ["
							+ configFile + "]", e);
		}
	}

	private ZooKeeperCache zkCache = null;

	public <K, V> Cache<K, V> getCache(final String name) throws CacheException {

		if (log.isTraceEnabled()) {
			log.trace("Acquiring Zookeeper instance named [" + name + "]");
		}

		net.sf.ehcache.Ehcache cache = ensureEhCacheManager().getEhcache(name);
		if (cache == null) {
			if (log.isInfoEnabled()) {
				log.info(
						"Cache with name '{}' does not yet exist.  Creating now.",
						name);
			}
			this.manager.addCache(name);

			cache = manager.getCache(name);

			if (log.isInfoEnabled()) {
				log.info("Added EhCache named [" + name + "]");
			}
		} else {
			if (log.isInfoEnabled()) {
				log.info("Using existing EHCache named [" + cache.getName()
						+ "]");
			}
		}

		zkCache = new ZooKeeperCache<K, V>(zkConnectString, zkSessionTimeout,
				cache);
		return zkCache;

	}

	private String zkConnectString = "localhost:2181";
	private int zkSessionTimeout = 3000;

	public void setZkConnectString(String zkConnectString) {
		this.zkConnectString = zkConnectString;
	}

	public void setZkSessionTimeout(int zkSessionTimeout) {
		this.zkSessionTimeout = zkSessionTimeout;
	}

	private net.sf.ehcache.CacheManager ensureEhCacheManager() {
		try {
			if (this.manager == null) {
				if (log.isDebugEnabled()) {
					log.debug("cacheManager property not set.  Constructing CacheManager instance... ");
				}
				// using the CacheManager constructor, the resulting instance is
				// _not_ a VM singleton
				// (as would be the case by calling CacheManager.getInstance().
				// We do not use the getInstance here
				// because we need to know if we need to destroy the
				// CacheManager instance - using the static call,
				// we don't know which component is responsible for shutting it
				// down. By using a single EhCacheManager,
				// it will always know to shut down the instance if it was
				// responsible for creating it.
				this.manager = new net.sf.ehcache.CacheManager(
						getCacheManagerConfigFileInputStream());
				if (log.isTraceEnabled()) {
					log.trace("instantiated Ehcache CacheManager instance.");
				}
				cacheManagerImplicitlyCreated = true;
				if (log.isDebugEnabled()) {
					log.debug("implicit cacheManager created successfully.");
				}
			}
			return this.manager;
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void destroy() throws Exception {
		if (log.isTraceEnabled()) {
			log.trace("Called destroy()");
			if (manager != null)
				manager.shutdown();
			if (zkCache != null)
				zkCache.close();
		}
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public void init() throws ShiroException {
		if (log.isTraceEnabled()) {
			log.trace("Called init()");
		}

		ensureEhCacheManager();

		// To change body of implemented methods use File | Settings | File
		// Templates.
	}
}
