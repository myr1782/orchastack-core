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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.spi.PersistenceUnitInfo;

import orchastack.core.jpa.container.ManagedPersistenceUnitInfoFactory;
import orchastack.core.jpa.container.ParsedPersistenceUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("rawtypes")
public class ManagedPersistenceUnitInfoFactoryImpl implements
		ManagedPersistenceUnitInfoFactory {

	private ConcurrentMap<Bundle, Collection<PersistenceUnitInfo>> persistenceUnits = new ConcurrentHashMap<Bundle, Collection<PersistenceUnitInfo>>();

	public Collection<PersistenceUnitInfo> createManagedPersistenceUnitMetadata(
			BundleContext containerContext, Bundle persistenceBundle,
			ServiceReference providerReference,
			Collection<ParsedPersistenceUnit> persistenceMetadata) {

		Collection<PersistenceUnitInfo> managedUnits = new ArrayList<PersistenceUnitInfo>();

		for (ParsedPersistenceUnit unit : persistenceMetadata)
			managedUnits.add(new PersistenceUnitInfoImpl(persistenceBundle,
					unit, providerReference, false));

		Collection<?> existing = persistenceUnits.putIfAbsent(
				persistenceBundle, managedUnits);
		if (existing != null)
			throw new IllegalStateException(
					"previous.pus.have.not.been.destroyed "
							+ persistenceBundle.getSymbolicName());
		return Collections.unmodifiableCollection(managedUnits);
	}

	public void destroyPersistenceBundle(BundleContext containerContext,
			Bundle bundle) {
		Collection<PersistenceUnitInfo> mpus = persistenceUnits.remove(bundle);
		if (mpus == null)
			return; // already destroyed
		for (PersistenceUnitInfo impl : mpus) {
			PersistenceUnitInfoImpl impll = (PersistenceUnitInfoImpl) impl;
			impll.clearUp();
		}
	}

	public String getDefaultProviderClassName() {
		return "org.apache.openjpa.persistence.PersistenceProviderImpl";
	}
}
