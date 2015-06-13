package orchastack.core.mq.requestor.kafka;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.I0Itec.zkclient.ZkClient;

import com.google.common.io.BaseEncoding;

import kafka.producer.KeyedMessage;
import kafka.admin.AdminUtils;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import orchastack.core.mq.requestor.DestinationFormatException;
import orchastack.core.mq.requestor.Message;
import orchastack.core.mq.requestor.MessageSerializer;
import orchastack.core.mq.requestor.MqRequestor;
import orchastack.core.mq.requestor.TransactionException;

public class KafkaRequestorImpl implements MqRequestor {

	private static Logger log = Logger.getLogger(KafkaRequestorImpl.class
			.getName());

	final private AtomicReference<Transaction> m_tx = new AtomicReference<Transaction>();
	private boolean m_transactional;

	private String m_destination;
	private String _destination;

	private boolean m_closed = false;

	private Producer<Integer, String> producer;
	private ConsumerConnector consumer;

	private Properties m_kafkaParams;

	KafkaRequestorImpl(String destination, Properties props, Transaction tx)
			throws TransactionException, IOException,
			DestinationFormatException, IllegalStateException,
			RollbackException, SystemException {

		m_kafkaParams = new Properties();
		m_kafkaParams.putAll(props);

		this.m_destination = destination;

		if (tx != null) {
			this.m_tx.set(tx);
			this.m_transactional = true;
			this.m_kafkaParams.put(
					KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_AUTOCOMMIT,
					"false");

			// for transanction safety
			m_kafkaParams
					.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_OFFSET_RESET_POLICY,
							"smallest");

		} else {
			this.m_transactional = false;
			this.m_kafkaParams.put(
					KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_AUTOCOMMIT,
					"true");
			this.m_kafkaParams
					.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_AUTOCOMMIT_INTERVAL,
							"1000");

		}
		// parse the destination string
		String[] dd = this.m_destination.split("://");
		if (dd[0] == null || dd[1] == null) {
			log.log(Level.SEVERE,
					"destination format error, legal format is : queue://test or topic://test!");
			throw new DestinationFormatException("destination format error");
		}

		String type = dd[0];
		this._destination = dd[1];

		// set queue or topic semantics for kafka
		if (type.equals("topic")) {
			String uid = UUID.randomUUID().toString().replace("-", "");
			m_kafkaParams.put(
					KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_GROUPID,
					this._destination + uid);
		} else if (type.equals("queue")) {
			m_kafkaParams.put(
					KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_GROUPID,
					this._destination);
		} else {
			log.log(Level.SEVERE,
					"destination format is not supported, legal format is : queue://test or topic://test!");
			throw new DestinationFormatException(
					"destination format is not supported");
		}

		m_kafkaParams.put(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_SERIALIZER,
				"kafka.serializer.StringEncoder");
		m_kafkaParams.put(
				KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_REQUEST_TIMEOUT,
				"2000");
		// extra settings for transaction
		if (this.m_transactional) {
			// choose transactional way

		} else {

		}

		// create consumer and producer
		producer = new Producer<Integer, String>(new ProducerConfig(
				this.m_kafkaParams));

		consumer = kafka.consumer.Consumer
				.createJavaConsumerConnector(new ConsumerConfig(
						this.m_kafkaParams));

		//start consumer first, if publish/subscribe mode, this is a must
		initConsumerIterator();
		
		// no confirm, it's asynchronous
		// this.m_channel.confirmSelect();
		if (this.m_transactional)
			m_tx.get().registerSynchronization(new Synchronization() {

				@Override
				public void beforeCompletion() {

					try {
						// send message at last
						for (KeyedMessage k_msg : msgCache) {
							producer.send(k_msg);
						}

						// commit consumer offset
						consumer.commitOffsets(true);
					} catch (Throwable e) {
						try {
							m_tx.get().setRollbackOnly();
						} catch (Exception e1) {
							log.log(Level.SEVERE,
									"Error rollback transaction!", e1);
						}
					}
				}

				@Override
				public void afterCompletion(int status) {
					msgCache.clear();
				}
			});

	}

	public String getDestination() {
		return this.m_destination;
	}

	private ZkClient zkClient;

	/**
	 * kafka topic is automatically created when send message to topic.<br>
	 * Anyway you may create a topic
	 */
	public void createDestination(String destination, boolean durable)
			throws Exception {

		String[] dd = destination.split("://");
		if (dd[0] == null || dd[1] == null) {
			log.log(Level.SEVERE,
					"destination format error, legal format is : queue://test or topic://test!");
			throw new DestinationFormatException("destination format error");
		}

		String type = dd[0];
		String dest = dd[1];

		if (type.equals("topic")) {

		} else if (type.equals("queue")) {

		} else {
			log.log(Level.SEVERE,
					"destination format is not supported, legal format is : queue://test or topic://test!");
			throw new DestinationFormatException(
					"destination format is not supported");
		}

		this._destination = dest;

		zkClient = new ZkClient(
				m_kafkaParams
						.getProperty(KafkaRequestorFactoryImpl.MQ_PARAMS_KAFKA_ZOOKEEPER_URL));
		AdminUtils.createTopic(zkClient, _destination, 1, 1, null);

	}

	final private LinkedList<KeyedMessage> msgCache = new LinkedList<KeyedMessage>();

	public Message send(Message msg) throws Exception {

		String payload = BaseEncoding.base64().encode(
				MessageSerializer.serialize(msg));

		KeyedMessage k_msg = new KeyedMessage<Integer, String>(_destination,
				payload);
		if (this.m_transactional)
			msgCache.add(k_msg);
		else {
			producer.send(k_msg);
			log.log(Level.INFO, "send message directly ok, msg - " + payload);
		}
		return null;
	}

	private ConsumerIterator<byte[], byte[]> m_consumerIt = null;

	private void initConsumerIterator() {
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(_destination, new Integer(1));

		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicCountMap);
		KafkaStream<byte[], byte[]> stream = consumerMap.get(_destination).get(
				0);
		this.m_consumerIt = stream.iterator();
	}

	public Message receive(int timeout) throws Exception {

		Message msg = null;
		if (this.m_consumerIt == null)
			initConsumerIterator();

		try {
			if (m_consumerIt.hasNext()) {
				byte[] mm = m_consumerIt.next().message();
				msg = MessageSerializer.deserialize(BaseEncoding.base64()
						.decode(new String(mm)));
				log.log(Level.INFO, "received message - " + new String(mm));
			}
		} catch (ConsumerTimeoutException e) {
			log.log(Level.INFO, "no message within timeout!");
		}
		return msg;
	}

	public void close() throws IOException {
		msgCache.clear();
		consumer.shutdown();
		producer.close();
		if (zkClient != null)
			zkClient.close();
	}

	public boolean isAvailable() {
		return !m_closed;
	}

}
