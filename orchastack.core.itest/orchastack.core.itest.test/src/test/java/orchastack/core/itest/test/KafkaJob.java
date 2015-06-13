package orchastack.core.itest.test;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.mq.requestor.Message;
import orchastack.core.mq.requestor.MqRequestor;
import orchastack.core.mq.requestor.MqRequestorFactory;

import org.junit.Assert;
import org.osgi.service.log.LogService;

public class KafkaJob implements Runnable {

	private static Logger log = Logger.getLogger(KafkaJob.class.getName());

	private MqRequestorFactory mqFactory;

	public KafkaJob(MqRequestorFactory mqFactory) {
		super();
		this.mqFactory = mqFactory;
	}

	public void run() {
		try {
			HashMap<String, byte[]> props = new HashMap<String, byte[]>();

			props.put(Message.MESSAGE_RABBITMQ_USERID, "guest".getBytes());
			props.put(Message.MESSAGE_CALLER_USERID, "mathews".getBytes());

			props.put("test-props", "hhh".getBytes());

			Message msg = new Message("hello".getBytes(), props);

			MqRequestor r = mqFactory.getRequestor("topic://test",
					Thread.currentThread(), false);

			Assert.assertNotNull(r);

			r.send(msg);

			Message m = r.receive(5000);

			Assert.assertNotNull(m);

			m = r.receive(5000);

			Assert.assertNotNull(m);

			Assert.assertEquals("hello", new String(m.getContent()));

			HashMap<String, byte[]> ppp = m.getProperties();

			Assert.assertNotNull(ppp);
			Assert.assertEquals("guest",
					new String(ppp.get(Message.MESSAGE_RABBITMQ_USERID)));
			Assert.assertEquals("mathews",
					new String(ppp.get(Message.MESSAGE_CALLER_USERID)));
			Assert.assertEquals("hhh", new String(ppp.get("test-props")));
			r.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Service Error!", e);
		}

	}

}
