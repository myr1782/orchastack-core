package orchastack.core.jpa.container.jpa;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import orchastack.core.jpa.container.tx.XADatasourceEnlistingWrapper;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class JndiDataSourceHelper {
	/** Logger */
	private static final Logger _logger = Logger
			.getLogger(JndiDataSourceHelper.class.getName());

	private AtomicReference<DataSource> ds = new AtomicReference<DataSource>();

	private final String jndiName;
	private final String unitName;
	private final Bundle persistenceBundle;
	private final boolean jta;

	public JndiDataSourceHelper(String jndi, String unit,
			Bundle persistenceBundle, boolean jta) {
		jndiName = jndi;
		unitName = unit;
		this.persistenceBundle = persistenceBundle;
		this.jta = jta;
	}

	public DataSource getDs() {
		if (ds.get() == null) {
			try {

				BundleContext bCtx = persistenceBundle.getBundleContext();
				if (bCtx == null)
					throw new IllegalStateException(
							"persistence.bundle.not.active, "
									+ persistenceBundle.getSymbolicName());
				Object o = null;
				if (jndiName.startsWith("osgi:service")) {

					_logger.log(Level.FINE,
							"looking for OSGI Datasource service---");

					String[] nms = new String[3];
					int firstSlash = jndiName.indexOf("/", 0);
					int secondSlash = jndiName.indexOf("/", firstSlash + 1);
					nms[1] = jndiName.substring(firstSlash + 1, secondSlash);
					nms[2] = jndiName.substring(secondSlash + 1,
							jndiName.length());

					_logger.log(Level.FINE, "Datasource interface - " + nms[1]
							+ ", filter - " + nms[2]);

					if (nms[1] != null && !nms[1].equals("")) {
						ServiceReference ref = getServiceReference(bCtx,
								nms[1], nms[2], 6000);

						_logger.log(Level.FINE, "got Datasource reference - " + ref);

						if (ref != null) {
							o = bCtx.getService(ref);
						} else {
							String message = "service reference not.found for data source "
									+ jndiName;
							_logger.log(Level.SEVERE, message);
						}
					}
				} else {
					Hashtable<String, Object> props = new Hashtable<String, Object>();
					props.put("osgi.service.jndi.bundleContext", bCtx);

					InitialContext ctx = new InitialContext(props);

					o = ctx.lookup(jndiName);
				}

				if (o == null) {
					String message = "no.data.source.found --- " + jndiName
							+ persistenceBundle.getSymbolicName();
					_logger.log(Level.SEVERE, message);
					throw new NullPointerException(message);
				} else if (o instanceof XADataSource) {
					if (jta) {
						ds.compareAndSet(null,
								wrapXADataSource((XADataSource) o));
					} else {
						if (o instanceof DataSource)
							ds.compareAndSet(null, (DataSource) o);
						else
							throw new IllegalArgumentException(
									"xa.datasource.non.tx,"
											+ persistenceBundle
													.getSymbolicName() + ","
											+ jndiName);
					}
				} else if (o instanceof DataSource) {
					ds.compareAndSet(null, (DataSource) o);
				} else {
					throw new IllegalArgumentException("not.a.datasource, "
							+ persistenceBundle.getSymbolicName() + ", "
							+ jndiName);
				}
			} catch (NamingException e) {
				String message = "no.data.source.found," + jndiName
						+ persistenceBundle.getSymbolicName();
				_logger.log(Level.SEVERE, message, e);
				throw new RuntimeException(message, e);
			} catch (InvalidSyntaxException e) {
				_logger.log(Level.SEVERE,
						"syntax error to query OSGI service!", e);
				throw new RuntimeException(
						"syntax error to query OSGI service!", e);
			}
		}
		return ds.get();
	}

	private ServiceReference getServiceReference(BundleContext context,
			String clazz, String filter, int timeout)
			throws InvalidSyntaxException {
		ServiceReference ref = null;
		// Begin to wait ...
		long enter = System.currentTimeMillis();
		boolean exhausted = false;
		synchronized (this) {
			while (ref == null && !exhausted) {
				if (filter == null || filter.trim().equals("")) {
					_logger.log(Level.FINE,
							"looking for OSGI Datasource service without filter--");
					ref = context.getServiceReference(clazz);
				} else {
					ServiceReference[] refs = context.getServiceReferences(
							clazz, filter);
					if (refs != null && refs.length > 0) {
						ref = refs[0];
					}
				}
				try {
					wait(50);
				} catch (InterruptedException e) {
					// We was interrupted ....
				} finally {
					long end = System.currentTimeMillis();
					exhausted = (end - enter) > timeout;
					_logger.log(Level.FINE, "looking for Datasource -- "
							+ (end - enter));
				}
			}
		}

		return ref;
	}

	protected DataSource wrapXADataSource(XADataSource xaDs)
			throws IllegalStateException {
		boolean b;
		try {
			Class.forName("javax.transaction.TransactionManager");
			b = true;
		} catch (ClassNotFoundException cnfe) {
			b = false;
		}

		if (!b)
			throw new IllegalStateException("no.xa.wrapping");

		return new XADatasourceEnlistingWrapper(xaDs);
	}
}
