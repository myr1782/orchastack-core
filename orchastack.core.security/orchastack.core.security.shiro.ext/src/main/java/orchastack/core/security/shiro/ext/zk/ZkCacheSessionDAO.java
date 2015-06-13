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
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.security.shiro.ext.Serializer;

import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.MapCache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

public class ZkCacheSessionDAO extends CachingSessionDAO {

	private boolean useMemCache = false;

	public ZkCacheSessionDAO(boolean useMemCache) {
		this.useMemCache = useMemCache;
		if (useMemCache) {
			setCacheManager(new AbstractCacheManager() {
				@Override
				protected Cache<Serializable, Session> createCache(String name)
						throws CacheException {
					return new MapCache<Serializable, Session>(name,
							new ConcurrentHashMap<Serializable, Session>());
				}
			});
		}

		try {
			zooKeeper = ensureCacheManager();
			if (!this.checkBaseDirExists()) {
				zooKeeper.create(shiroSessionZKPath, null, null,
						CreateMode.PERSISTENT);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "visit zookeeper error.");
		}
	}

	Logger logger = Logger.getLogger(this.getClass().getName());

	private ZooKeeper zooKeeper;

	private String shiroSessionZKPath = "/shirosessions";

	private String sessionPrefix = "session-";

	public void setSessionPrefix(String sessionPrefix) {
		this.sessionPrefix = sessionPrefix;
	}

	private ZooKeeper ensureCacheManager() throws IOException {
		return new ZooKeeper("localhost:2181", 3000, null);
	}

	public void setShiroSessionZKPath(String shiroSessionZKPath) {
		this.shiroSessionZKPath = shiroSessionZKPath;
	}

	@Override
	protected void doUpdate(Session session) {
		if (session == null || session.getId() == null) {
			logger.log(Level.SEVERE, "session argument cannot be null.");
		}
		saveSession(session, "update");
	}

	@Override
	protected void doDelete(Session session) {
		if (session == null || session.getId() == null) {
			logger.log(Level.SEVERE, "session argument cannot be null.");
		}
		logger.log(Level.FINE, "delete session for id: {}", session.getId());
		try {
			zooKeeper.delete(getPath(session.getId()), -1);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "delete zookeeper data error.");
		}
	}

	@Override
	public Collection<Session> getActiveSessions() {
		Set<Session> sessions = new HashSet<Session>();
		// return something like session-9e3b5707-fa80-4d32-a6c9-f1c3685263a5
		List<String> ss = null;
		try {
			ss = zooKeeper.getChildren(shiroSessionZKPath, false);
		} catch (KeeperException e) {

		} catch (InterruptedException e) {

		}
		if (ss != null)
			for (String id : ss) {
				if (id.startsWith(sessionPrefix)) {
					String noPrefixId = id.replace(sessionPrefix, "");
					Session session = doReadSession(noPrefixId);
					if (session != null)
						sessions.add(session);
				}
			}
		logger.log(Level.FINE, "shiro getActiveSessions. size: {}",
				sessions.size());
		return sessions;
	}

	@Override
	protected Serializable doCreate(Session session) {
		Serializable sessionId = this.generateSessionId(session);
		this.assignSessionId(session, sessionId);
		saveSession(session, "create");
		return sessionId;
	}

	@Override
	protected Session doReadSession(Serializable id) {
		if (id == null) {
			logger.log(Level.SEVERE, "id is null!");
			return null;
		}
		logger.log(Level.FINE, "doReadSession for path: {}", getPath(id));

		Session session;
		try {
			byte[] byteData = zooKeeper.getData(getPath(id), false, null);
			if (byteData != null && byteData.length > 0) {

				session = (Session) Serializer.deserialize(byteData);

				if (useMemCache) {
					this.cache(session, id);
					logger.log(Level.FINE,
							"doReadSession for path: {}, add cached !",
							getPath(id));
				}
				return session;
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "read zookeeper data error.");
		}
		return null;

	}

	private String getPath(Serializable sessID) {
		return shiroSessionZKPath + '/' + sessionPrefix + sessID.toString();
	}

	private void saveSession(Session session, String act) {
		Serializable sessionId = session.getId();
		logger.log(Level.FINE, "save session for id: {}, act: {}",
				new Object[] { sessionId, act });

		try {
			String path = getPath(sessionId);
			byte[] data = Serializer.serialize(session);

			if (act == "update")
				zooKeeper.setData(path, data, -1);
			else
				zooKeeper.create(path, data, null, CreateMode.PERSISTENT);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error save session to zookeeper.", e);
		}
	}

	private boolean checkBaseDirExists() {
		try {
			return !(zooKeeper.exists(shiroSessionZKPath, false) == null);
		} catch (KeeperException e) {
			throw new CacheException("KeeperException: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new CacheException("InterruptedException: " + e.getMessage(),
					e);
		}
	}

}
