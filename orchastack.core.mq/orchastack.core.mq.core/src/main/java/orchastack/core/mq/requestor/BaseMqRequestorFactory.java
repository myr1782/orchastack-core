package orchastack.core.mq.requestor;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseMqRequestorFactory implements MqRequestorFactory {

	private static Logger log = Logger.getLogger(BaseMqRequestorFactory.class
			.getName());

	protected ConcurrentHashMap<RequestorKey, MqRequestor> cachedRequestors = new ConcurrentHashMap<RequestorKey, MqRequestor>();

	protected Properties m_props;

	public BaseMqRequestorFactory() {

	}

	@Override
	public void setBrokerParams(Properties params) {
		this.m_props = params;
	}

	public MqRequestor getRequestor(String destination, Thread client,
			boolean transactional) throws Exception {
		MqRequestor requestor = null;

		if (m_props == null) {
			log.log(Level.WARNING,
					"Karaf Properties are not set, init() again!");
			init();
			if (m_props == null)
				return null;
		}
		String brokerURL = (String) m_props
				.get(MqRequestorFactory.MQ_PARAMS_BROKER_URL);

		if (brokerURL == null) {
			log.log(Level.WARNING, "Karaf brokerURL is not set!");
			return null;
		}

		RequestorKey rkey = new RequestorKey(brokerURL, destination, client);
		if (!cachedRequestors.containsKey(rkey)) {
			log.log(Level.INFO, "No MqRequestor before, will create new!");
			requestor = createNewRequestor(rkey, transactional);
		} else {
			// FIXME check if the requestor is available
			requestor = cachedRequestors.get(rkey);
			if (!requestor.isAvailable()) {
				log.log(Level.INFO, "MqRequestor closed, will create new!");
				cachedRequestors.remove(rkey);
				requestor = createNewRequestor(rkey, transactional);
			}
		}

		return requestor;
	}

	protected abstract void init() throws Exception;

	protected abstract MqRequestor createNewRequestor(RequestorKey rkey,
			boolean transactional) throws Exception;

	@Override
	public void closeall() {
		try {
			for (MqRequestor r : cachedRequestors.values()) {
				r.close();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error close MqRequestors!", e);
		}

	}

}
