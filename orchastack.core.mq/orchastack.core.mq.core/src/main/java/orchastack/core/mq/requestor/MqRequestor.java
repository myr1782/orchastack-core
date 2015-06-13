package orchastack.core.mq.requestor;

/**
 * 
 * @author active
 * 
 *         This is a wrapped class around MQ Session, which is a light-weighted
 *         object, not to be shared between threads and transactions.
 * 
 * @param <T>
 *            type of message
 */
public interface MqRequestor {

	/**
	 * get the destination for this MQ Requestor,destination will be like
	 * queue://testQueue or topic://testTopic
	 * 
	 * @return destination
	 */
	public String getDestination();

	/**
	 * create the Queue or Topic, if not durable, will be deleted before session
	 * closes. <br>
	 * Destination will be like queue://testQueue or topic://testTopic
	 * 
	 * @param destination
	 * @param durable
	 */
	public void createDestination(String destination, boolean durable) throws Exception;

	/**
	 * just send a message to MQ broker
	 * 
	 * @param msg
	 * @return a renewed @Message contains message ID, etc. properties from MQ
	 *         broker
	 */
	public Message send(Message msg) throws Exception;

	/**
	 * get a message from MQ broker, will wait for timeout period for a message,
	 * if timeout, no message
	 * 
	 * @param timeout
	 * @return
	 */
	public Message receive(int timeout) throws Exception;


	/**
	 * underlined MQ Session will be closed, if transaction is involved, session
	 * will be closed automatically.
	 */
	public void close() throws Exception;

	/**
	 * perhaps connection is intermittent, who knows?
	 * 
	 * @return
	 */
	public boolean isAvailable();

}
