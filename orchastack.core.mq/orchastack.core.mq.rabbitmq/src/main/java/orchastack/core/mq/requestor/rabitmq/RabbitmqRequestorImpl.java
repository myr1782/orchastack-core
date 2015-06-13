package orchastack.core.mq.requestor.rabitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import orchastack.core.mq.requestor.DestinationFormatException;
import orchastack.core.mq.requestor.Message;
import orchastack.core.mq.requestor.MqRequestor;
import orchastack.core.mq.requestor.TransactionException;

public class RabbitmqRequestorImpl implements MqRequestor {

	private static Logger log = Logger.getLogger(RabbitmqRequestorImpl.class
			.getName());

	final private AtomicReference<Transaction> m_tx = new AtomicReference<Transaction>();
	private boolean m_transactional;

	private String m_destination;
	private String _destination;
	private String m_exchange;

	private QueueingConsumer m_consumer;

	private Channel m_channel;

	private boolean m_closed = false;

	RabbitmqRequestorImpl(String destination, QueueingConsumer consumer,
			Channel channel, Transaction tx) throws TransactionException,
			IOException, DestinationFormatException {
		super();
		this.m_destination = destination;
		this.m_consumer = consumer;
		this.m_channel = channel;
		if (tx != null) {
			this.m_tx.set(tx);
			this.m_transactional = true;

			// choose the transactional mode
			this.m_channel.txSelect();
		} else
			this.m_transactional = false;

		// register Synchronization to commit or roll back tx
		if (this.m_transactional) {
			try {
				m_tx.get().registerSynchronization(new Synchronization() {

					public void beforeCompletion() {

						
						boolean rollback = false;
						
						// send messages at last
						for (MessageWrapper w_msg : msgCache) {
							try {
								m_channel.basicPublish(m_exchange, null,
										w_msg.properties, w_msg.body);
							} catch (IOException e) {
								try {
									m_tx.get().setRollbackOnly();
									m_channel.txRollback();
								} catch (Exception e1) {
									log.log(Level.SEVERE,
											"Error rollback transaction", e1);
								}

								log.log(Level.WARNING,
										"Error send message, transtion is rolled back",
										e);

								break;
							}
						}
						
						// ack messages
						if (!receivedMessages.isEmpty())
							try {
								m_channel.basicAck(
										receivedMessages.get(
												receivedMessages.size() - 1)
												.getDeliveryTag(), false);
							} catch (IOException e) {
								try {
									m_tx.get().setRollbackOnly();
									m_channel.txRollback();
								} catch (Exception e1) {
									log.log(Level.SEVERE,
											"Error rollback transaction", e1);
								}

								log.log(Level.WARNING,
										"Error ack message, transtion is rolled back",
										e);
								rollback = true;
							}


						// commit
						try {
							m_channel.txCommit();
						} catch (IOException e) {
							try {
								m_tx.get().setRollbackOnly();
								m_channel.txRollback();
							} catch (Exception e1) {
								log.log(Level.SEVERE,
										"Error rollback transaction", e1);
							}

							log.log(Level.WARNING,
									"Error commit, transtion is rolled back", e);
						}
					}

					public void afterCompletion(int status) {
						
						msgCache.clear();
						
						// FIXME do we really need to close channel after tx
						// commit
						try {
							m_channel.close();
						} catch (IOException e) {
							log.log(Level.WARNING,
									"Error close channel after tx commit!", e);
						}

					}

				});
			} catch (Exception e) {
				throw new TransactionException(
						"Error to register Synchronization!", e);
			}
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

		// create queue or topic
		// FIXME what if already exist?
		if (type.equals("topic")) {
			// declare exchange
			this.m_channel.exchangeDeclare(this.m_destination, type, true);

			this._destination = channel.queueDeclare().getQueue();

			this.m_exchange = this.m_destination;

			this.m_channel.queueBind(this._destination, this.m_exchange,
					this.m_destination);

		} else if (type.equals("queue")) {
			this.m_channel.queueDeclare(_destination, true, false, false, null);

			this.m_exchange = _destination;
			this.m_channel.exchangeDeclare(this.m_destination, "direct", true);
			this.m_channel.queueBind(_destination, m_exchange,
					this.m_destination);

			// this.m_exchange = "";

		} else {
			log.log(Level.SEVERE,
					"destination format is not supported, legal format is : queue://test or topic://test!");
			throw new DestinationFormatException(
					"destination format is not supported");
		}

		// listen for shutdown event
		this.m_channel.addShutdownListener(new ShutdownListener() {
			public void shutdownCompleted(ShutdownSignalException cause) {
				m_closed = true;
			}
		});

		// fair deliver
		this.m_channel.basicQos(1);

		// start consumer
		if (this.m_transactional) {
			// choose transactional way
			// this.m_channel.txSelect();
			this.m_channel.basicConsume(this._destination, false,
					this.m_consumer);
		} else {
			this.m_channel.basicConsume(this._destination, true,
					this.m_consumer);
		}

		// no confirm, it's asynchronous
		// this.m_channel.confirmSelect();

	}

	public String getDestination() {
		return this.m_destination;
	}

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
			// declare exchange
			this.m_channel.exchangeDeclare(dest, type, true);
		} else if (type.equals("queue")) {
			this.m_channel.queueDeclare(dest, true, false, false, null);
			this.m_channel.exchangeDeclare(dest, "direct", true);
		} else {
			log.log(Level.SEVERE,
					"destination format is not supported, legal format is : queue://test or topic://test!");
			throw new DestinationFormatException(
					"destination format is not supported");
		}
	}

	private class MessageWrapper {

		AMQP.BasicProperties properties;
		byte[] body;

		public MessageWrapper(BasicProperties properties, byte[] body) {
			super();
			this.properties = properties;
			this.body = body;
		}
	}

	private LinkedList<MessageWrapper> msgCache = new LinkedList<MessageWrapper>();

	public Message send(Message msg) throws Exception {

		BasicProperties theProps = null;
		if (msg.getProperties() != null && !msg.getProperties().isEmpty()) {
			theProps = RabbitmqUtils.convertProperties(msg.getProperties());
		}

		MessageWrapper wmsg = new MessageWrapper(theProps, msg.getContent());

		String routing_key = this.m_destination;

		if (this.m_transactional)
			msgCache.add(wmsg);
		else {
			m_channel.basicPublish(this.m_exchange, routing_key, theProps,
					msg.getContent());
		}

		return null;
	}

	private LinkedList<Envelope> receivedMessages = new LinkedList<Envelope>();

	public Message receive(int timeout) throws Exception {

		Message msg = null;
		QueueingConsumer.Delivery delivery;

		delivery = m_consumer.nextDelivery(timeout);

		if (delivery != null) {
			Envelope env = delivery.getEnvelope();
			if (env != null && env.getDeliveryTag() == 0) {
				log.log(Level.SEVERE,
						"Message receive error with DeliveryTag = 0!");
			}

			if (this.m_transactional)
				receivedMessages.add(delivery.getEnvelope());

			byte[] content = delivery.getBody();

			HashMap props = RabbitmqUtils.convertProperties(delivery
					.getProperties());

			msg = new Message(content, props);
		}
		return msg;
	}

	public void close() throws IOException {

		// unbind if topic
		if (this.m_destination.startsWith("topic")) {
			this.m_channel.exchangeUnbind(this._destination, this.m_exchange, this.m_destination);
//			.queueBind(this._destination, this.m_exchange,
//					this.m_destination);
		}
		this.m_channel.close();

	}

	public boolean isAvailable() {
		return !m_closed;
	}

}
