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

import orchastack.core.security.shiro.ext.Serializer;

import org.apache.shiro.cache.CacheException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ZooKeeperCache<K, V> extends EhCache<K, V> implements Watcher {
	public static final Logger log = Logger.getLogger(ZooKeeperCache.class
			.getName());
	public static final String BASE_PATH = "/shiro";

	private ZooKeeper zooKeeper;

	public ZooKeeperCache(String zkConnectString, int zkSessionTimeout,
			net.sf.ehcache.Ehcache cache) {
		super(cache);
		log.log(Level.FINE, "Instantiate ZooKeeperCache: {}", zooKeeper);

		log.log(Level.INFO, "ZooKeeper is not yet available creating new one.");

		try {
			zooKeeper = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
		} catch (IOException e0) {
			log.log(Level.SEVERE,
					"error connect to zookeeper, probably zookeeper servers are down!",
					e0);
			if (zooKeeper != null) {
				try {
					zooKeeper.close();
				} catch (InterruptedException e) {
					log.log(Level.SEVERE, "error close zookeeper client!", e);
				}
			}

			this.zooKeeper = null;

		}

	}

	public void close() {
		try {
			if (zooKeeper != null)
				zooKeeper.close();
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "error close zookeeper!", e);
		}
	}

	public void process(WatchedEvent event) {
		if (zooKeeper != null)
			try {
				String path = event.getPath();
				if (path != null && path.startsWith(BASE_PATH)) {
					String sid = path.substring(path.indexOf("/") + 1);
					if (sid != null) {
						if (event.getType() == EventType.NodeDataChanged
								|| event.getType() == EventType.NodeCreated) {
							byte[] result = zooKeeper.getData(path, false,
									new Stat());
							V val = (V) Serializer.deserialize(result);
							ZooKeeperCache.super.put((K) sid, val);
						}
						if (event.getType() == EventType.NodeDeleted) {
							ZooKeeperCache.super.remove((K) sid);
						}
					}
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "error process zookeeper event!", e);
			}

	}

	public V get(final K key) throws CacheException {
		log.log(Level.FINER, "Called: get({})", key);
		try {
			// try local ehcache first
			V obj = super.get(key);
			if (obj != null)
				return obj;

			if (zooKeeper != null) {
				Stat stat = new Stat();
				if (checkExists(key)) {

					byte[] result = zooKeeper.getData(BASE_PATH + "/" + key,
							null, stat);
					log.log(Level.FINE, "Found: {}", result);
					return (V) Serializer.deserialize(result);
				} else {
					log.log(Level.FINE, "Didn't found PATH: {}", key);
					return null;
				}
			}
		} catch (KeeperException e) {
			throw new CacheException("KeeperException", e);
		} catch (InterruptedException e) {
			throw new CacheException("InterruptedException", e);
		} catch (Exception e) {
			throw new CacheException("SerilizationError", e);
		}

		return null;
	}

	public V put(final K key, final V value) throws CacheException {
		// put into local ehcahe first

		super.put(key, value);

		log.log(Level.FINER, "Called: put({},{})", new Object[] { key, value });

		if (zooKeeper != null)
			try {
				if (!checkBaseDirExists()) {
					log.log(Level.FINE,
							"Didn't found BASE_PATH ({}) creating it.",
							BASE_PATH);
					zooKeeper.create(BASE_PATH, null,
							ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				if (!checkExists(key)) {
					log.log(Level.FINE,
							"Didn't found SESSION_PATH ({}) creating it with {}",
							new Object[] { key, value });
					zooKeeper.create(BASE_PATH + "/" + key,
							Serializer.serialize((Serializable) value),
							ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				} else {
					log.log(Level.FINE,
							"Found SESSION_PATH ({}) updating it with {}.",
							new Object[] { key, value });
					zooKeeper.setData(BASE_PATH + "/" + key,
							Serializer.serialize((Serializable) value), -1);
				}

			} catch (KeeperException e) {
				throw new CacheException("KeeperException", e);
			} catch (InterruptedException e) {
				throw new CacheException("InterruptedException", e);
			} catch (Exception e) {
				throw new CacheException("SerilizationError", e);
			}
		return null;
	}

	public V remove(final K key) throws CacheException {
		// remove local ehcache first
		super.remove(key);

		log.log(Level.FINER, "Called: remove({})", key);

		if (zooKeeper != null)
			if (checkExists(key)) {
				try {
					zooKeeper.delete(BASE_PATH + "/" + key, -1);
				} catch (InterruptedException e) {
					throw new CacheException("InterruptedException", e);
				} catch (KeeperException e) {
					throw new CacheException("KeeperException", e);
				}
			}
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	public void clear() throws CacheException {
		super.clear();

		log.log(Level.FINER, "Called: clear()");
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	public int size() {

		log.log(Level.FINER, "Called: size()");
		return super.size(); // To change body of implemented methods use File |
								// Settings |
		// File Templates.
	}

	public Set<K> keys() {
		log.log(Level.FINER, "Called: keys()");
		return super.keys(); // To change body of implemented methods use File |
		// Settings | File Templates.
	}

	public Collection<V> values() {
		log.log(Level.FINER, "Called: values()");
		return super.values(); // To change body of implemented methods use File
								// |
		// Settings | File Templates.
	}

	private boolean checkExists(K sessionId) {
		try {
			if (zooKeeper != null)
				return !(zooKeeper.exists(BASE_PATH + "/" + sessionId, false) == null);
		} catch (KeeperException e) {
			throw new CacheException("KeeperException: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new CacheException("InterruptedException: " + e.getMessage(),
					e);
		}
		return false;
	}

	private boolean checkBaseDirExists() {
		try {
			if (zooKeeper != null)
				return !(zooKeeper.exists(BASE_PATH, false) == null);
		} catch (KeeperException e) {
			throw new CacheException("KeeperException: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new CacheException("InterruptedException: " + e.getMessage(),
					e);
		}
		return false;
	}
}
