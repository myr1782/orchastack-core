package orchastack.core.mq.requestor.kafka;

import java.util.Properties;
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

@Component(managedservice = "orchastack.core.mq.params")
@Provides(specifications = { orchastack.core.mq.requestor.MqRequestorFactory.class }, strategy = "SERVICE")
@Instantiate
public class KafkaRequestorFactoryImpl extends BaseMqRequestorFactory {

	/**
	 * kafka property names for producer
	 */
	// kafka.serializer.DefaultEncoder
	final public static String MQ_PARAMS_KAFKA_SERIALIZER = "serializer.class";
	final public static String MQ_PARAMS_KAFKA_BROKERS = "metadata.broker.list";
	final public static String MQ_PARAMS_KAFKA_REQUEST_ACK = "request.required.acks";
	final public static String MQ_PARAMS_KAFKA_REQUEST_TIMEOUT = "request.timeout.ms";

	// sync async
	final public static String MQ_PARAMS_KAFKA_PRODUCER_TYPE = "producer.type";
	// if async, default 200
	final public static String MQ_PARAMS_KAFKA_BATCH_NUM = "batch.num.messages";
	final public static String MQ_PARAMS_KAFKA_COMPRESSION_CODEC = "compression.codec";

	/**
	 * kafka property names for consumer
	 * 
	 */
	final public static String MQ_PARAMS_KAFKA_ZOOKEEPER_URL = "zookeeper.connect";
	final public static String MQ_PARAMS_KAFKA_GROUPID = "group.id";
	final public static String MQ_PARAMS_KAFKA_ZOOKEEPER_TIMEOUT = "zookeeper.session.timeout.ms";
	final public static String MQ_PARAMS_KAFKA_ZOOKEEPER_SYNC_TIME = "zookeeper.sync.time.ms";
	final public static String MQ_PARAMS_KAFKA_SOCKET_TIMEOUT = "socket.timeout.ms";
	final public static String MQ_PARAMS_KAFKA_CONSUMER_TIMEOUT = "consumer.timeout.ms";

	final public static String MQ_PARAMS_KAFKA_CLIENTID = "client.id";
	final public static String MQ_PARAMS_KAFKA_DUAL_COMMIT = "dual.commit.enabled";
	/**
	 * tx related properties
	 */
	final public static String MQ_PARAMS_KAFKA_AUTOCOMMIT = "auto.commit.enable";
	final public static String MQ_PARAMS_KAFKA_AUTOCOMMIT_INTERVAL = "auto.commit.interval.ms";
	final public static String MQ_PARAMS_KAFKA_OFFSET_RESET_POLICY = "auto.offset.reset";

	private static Logger log = Logger
			.getLogger(KafkaRequestorFactoryImpl.class.getName());

	@Property
	private String userName;

	@Property
	private String password;

	@Property
	private String brokerURL;

	@Property
	private String zkURL;

	@Property
	private int networkCheckInterval = 6000;

	@Property
	private String requestTimeout = "2000";

	@Property
	private String consumerTimeout = "2000";

	@Requires
	private TransactionManager txMnager;

	public KafkaRequestorFactoryImpl() {
		// clear cache
		// cachedRequestors.clear();
	}

	@Validate
	public void init() {
		Properties props = new Properties();
		props.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_BROKERS, brokerURL);
		props.put(MqRequestorFactory.MQ_PARAMS_BROKER_URL, brokerURL);
		props.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_ZOOKEEPER_URL,
				zkURL);

		props.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_REQUEST_TIMEOUT,
				requestTimeout);
		props.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_CONSUMER_TIMEOUT,
				consumerTimeout);

		props.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_ZOOKEEPER_TIMEOUT,
				"400");
		props.put(
				KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_ZOOKEEPER_SYNC_TIME,
				"200");

		props.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_REQUEST_ACK, "1");

		super.setBrokerParams(props);

		log.log(Level.INFO,
				"kafka properties in BaseMqRequestorFactory is set as " + props);
	}

	@Invalidate
	public void closeall() {
		super.closeall();

		log.log(Level.INFO, "clean up is done!");
	}

	public void setBrokerParams(Properties params) {

		this.userName = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_USERNAME);

		this.password = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_PASSWORD);

		this.brokerURL = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_BROKER_URL);

		this.zkURL = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_KAFKA_ZOOKEEPER_URL);

		this.networkCheckInterval = (Integer) params
				.get(MqRequestorFactory.MQ_PARAMS_NETWORK_CHECK_INTERVAL);

		this.requestTimeout = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_KAFKA_REQUEST_TIMEOUT);

		this.consumerTimeout = (String) params
				.get(MqRequestorFactory.MQ_PARAMS_KAFKA_CONSUMER_TIMEOUT);

		init();
	}

	public MqRequestor createNewRequestor(RequestorKey rkey,
			boolean transactional) throws Exception {

		Transaction tx = null;
		if (transactional)
			tx = txMnager.getTransaction();

		this.m_props.remove(MqRequestorFactory.MQ_PARAMS_BROKER_URL);

		MqRequestor requestor = new KafkaRequestorImpl(rkey.queue,
				this.m_props, tx);

		// add to cache
		cachedRequestors.put(rkey, requestor);

		return requestor;
	}

}
