package orchastack.core.itest.biz.impl;

import java.util.HashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.junit.Assert;

import orchastack.core.itest.biz.MqBizService;
import orchastack.core.mq.requestor.Message;
import orchastack.core.mq.requestor.MqRequestor;
import orchastack.core.mq.requestor.MqRequestorFactory;
import orchastack.core.transaction.Transactional;

@Component
@Provides(specifications = { orchastack.core.itest.biz.MqBizService.class })
@Instantiate
public class MqBizServiceImpl implements MqBizService {

	@Requires
	private MqRequestorFactory mqFactory;

	@Transactional(timeout = 4000, propagation = "requires")
	public void sendMessageWithin() throws Exception {
		HashMap<String, byte[]> props = new HashMap<String, byte[]>();

		props.put(Message.MESSAGE_RABBITMQ_USERID, "guest".getBytes());
		props.put(Message.MESSAGE_CALLER_USERID, "mathews".getBytes());

		props.put("test-props", "hhh".getBytes());

		Message msg = new Message("hello".getBytes(), props);

		MqRequestor r = mqFactory.getRequestor("queue://test",
				Thread.currentThread(), true);

		r.send(msg);

		// throw new RuntimeException();

	}

	@Transactional(timeout = 4000, propagation = "requires")
	public void receiveMessageWithin() throws Exception {
		MqRequestor r = mqFactory.getRequestor("queue://test",
				Thread.currentThread(), true);
		r.receive(3000);

		throw new RuntimeException();
	}

}
