package orchastack.core.mq.requestor.rabitmq;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import orchastack.core.mq.requestor.BaseMqRequestorFactory;
import orchastack.core.mq.requestor.MqRequestor;
import orchastack.core.mq.requestor.MqRequestorFactory;
import orchastack.core.mq.requestor.RequestorKey;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

@Component(managedservice = "orchastack.core.mq.params")
@Provides(specifications = { orchastack.core.mq.requestor.MqRequestorFactory.class }, strategy = "SERVICE")
@Instantiate
public class RabbitmqRequestorFactoryImpl extends BaseMqRequestorFactory  {

	private static Logger log = Logger
			.getLogger(RabbitmqRequestorFactoryImpl.class.getName());

	@Property
	private String userName;

	@Property
	private String password;

	@Property
	private String virtualHost;

	@Property
	private String brokerURL;

	@Property
	private int networkCheckInterval = 6000;

	@Requires
	private TransactionManager txMnager;

	public RabbitmqRequestorFactoryImpl() {
		// clear cache
		// cachedRequestors.clear();
	}

	private static ConcurrentHashMap<String, Connection> cachedConnections = new ConcurrentHashMap<String, Connection>();

	@Validate
	public void init() {
		initConnection(brokerURL, virtualHost, userName, password,
				networkCheckInterval);
	}

	public void initConnection(final String brokerURL, String virtualHost,
			String userName, String password, int networkCheckInterval) {
		try {
			// when re-connect, no clear cache
			if (!cachedConnections.containsKey(brokerURL)) {
				ConnectionFactory factory = new ConnectionFactory();
				factory.setUsername(userName);
				factory.setPassword(password);
				factory.setVirtualHost(virtualHost);
				String[] urls = brokerURL.split(",");
				String[] hosts = urls[0].split(":");
				if (hosts.length != 2) {
					log.log(Level.SEVERE,
							"Critical - broker URL format: hostname:port");
				}
				factory.setHost(hosts[0]);
				factory.setPort(Integer.parseInt(hosts[1]));

				factory.setAutomaticRecoveryEnabled(true);
				// connection that will recover automatically
				factory.setNetworkRecoveryInterval(networkCheckInterval);

				Connection m_connection = factory.newConnection();

				m_connection.addShutdownListener(new ShutdownListener() {
					public void shutdownCompleted(ShutdownSignalException cause) {
						Connection conn = (Connection) cause.getReference();
						int port = conn.getPort();
						String host = conn.getAddress().getHostName();
						cachedConnections.remove(brokerURL);
					}
				});

				cachedConnections.put(brokerURL, m_connection);
			}

		} catch (Exception e) {
			log.log(Level.WARNING, "Error init JmsRequestorFactory!", e);
		}

		log.log(Level.INFO, "RaabitMQ Connection is created for " + brokerURL);
	}

	@Invalidate
	public void closeAll() {
		try {
			super.closeall();
			for (Connection con : cachedConnections.values()) {
				con.close();
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error close RaabitMQ Connections!", e);
		}
	}

	public void setBrokerParams(Properties params) {

		super.setBrokerParams(params);
		
		String userName = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_USERNAME);

		String password = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_PASSWORD);

		String virtualHost = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_RABBITMQ_VIRTUALHOST);

		String brokerURL = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_BROKER_URL);

		int networkCheckInterval = (Integer) params
				.get(MqRequestorFactory.MQ_PARAMS_NETWORK_CHECK_INTERVAL);

		initConnection(brokerURL, virtualHost, userName, password,
				networkCheckInterval);

	}

	public MqRequestor createNewRequestor(RequestorKey rkey,
			boolean transactional) throws Exception {

		if (!cachedConnections.containsKey(rkey.brokerURL)) {
			log.log(Level.WARNING,
					"RabbitMQ connection is not properly set, please set by config orchastack.core.mq.params or method -setBrokerParams()");
			return null;
		}

		Transaction tx = null;
		if (transactional)
			tx = txMnager.getTransaction();

		Channel channel = cachedConnections.get(rkey.brokerURL).createChannel();
		QueueingConsumer consumer = new QueueingConsumer(channel);
		MqRequestor requestor = new RabbitmqRequestorImpl(rkey.queue, consumer,
				channel, tx);

		// add to cache
		cachedRequestors.put(rkey, requestor);

		return requestor;
	}

}
