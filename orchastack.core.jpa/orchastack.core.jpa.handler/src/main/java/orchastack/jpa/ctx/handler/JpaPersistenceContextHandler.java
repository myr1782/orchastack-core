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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package orchastack.jpa.ctx.handler;

import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.annotations.Handler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

@Handler(name = "persistencecontext", namespace = JpaPersistenceContextHandler.NAMESPACE)
public class JpaPersistenceContextHandler extends PrimitiveHandler {

	final private static Logger log = Logger
			.getLogger(JpaPersistenceContextHandler.class.getName());

	/**
	 * Default timeout if not specified.
	 */
	public static final int DEFAULT_TIMEOUT = 3000;
	/**
	 * The handler namespace.
	 */
	public static final String NAMESPACE = "javax.persistence";

	/**
	 * Start method. Starts managed dependencies.
	 * 
	 * @see org.apache.felix.ipojo.Handler#start()
	 */
	public void start() {
	}

	/**
	 * Stop method. Stops managed dependencies.
	 * 
	 * @see org.apache.felix.ipojo.Handler#stop()
	 */
	public void stop() {
	}
	

	public void configure(Element meta, Dictionary dictionary)
			throws ConfigurationException {

		log.log(Level.INFO, "configuring iPOJO component!!!");

		PojoMetadata manipulation = getFactory().getPojoMetadata();
		Element[] deps = meta.getElements("persistencecontext", NAMESPACE);

		for (int i = 0; i < deps.length; i++) {
			if (!deps[i].containsAttribute("field")) {
				error("PersistenceContext must be attached to a field or the field is already used");
				return;
			}
			String field = deps[i].getAttribute("field");
			FieldMetadata fieldmeta = manipulation.getField(field);

			if (fieldmeta == null) {
				error("The field " + field + " does not exist in the class "
						+ getInstanceManager().getClassName());
				return;
			}

			String spec = fieldmeta.getFieldType();

			String unitName = "";

			if (deps[i].containsAttribute("unitname")) {
				unitName = deps[i].getAttribute("unitname");
			}
			PersistenceInterceptor icpt = new PersistenceInterceptor(
					getInstanceManager().getContext(), unitName, 6000);

			getInstanceManager().register(fieldmeta, icpt);
		}
	}

}
