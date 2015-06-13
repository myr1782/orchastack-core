package orchastack.core.mq.requestor;

import java.util.Properties;

public interface MqRequestorFactory {

	// config property names
	final public static String MQ_HORNETQ = "activeMQ";
	final public static String MQ_RABBITMQ = "rabbitMQ";
	final public static String MQ_KAFKA = "kafka";

	final public static String MQ_PARAMS_USERNAME = "userName";
	final public static String MQ_PARAMS_PASSWORD = "password";

	final public static String MQ_PARAMS_BROKER_URL = "brokerURL";

	final public static String MQ_PARAMS_RABBITMQ_VIRTUALHOST = "virtualHost";

	final public static String MQ_PARAMS_NETWORK_CHECK_INTERVAL = "networkCheckInterval";

	final public static String MQ_PARAMS_KAFKA_ZOOKEEPER_URL = "zookeeperURL";
	
	final public static String MQ_PARAMS_KAFKA_REQUEST_TIMEOUT = "requestTimeout";
	final public static String MQ_PARAMS_KAFKA_CONSUMER_TIMEOUT = "consumerTimeout";

	public void setBrokerParams(Properties params);

	public MqRequestor getRequestor(String destination, Thread client,
			boolean transactional) throws Exception;

	public void closeall();

}
