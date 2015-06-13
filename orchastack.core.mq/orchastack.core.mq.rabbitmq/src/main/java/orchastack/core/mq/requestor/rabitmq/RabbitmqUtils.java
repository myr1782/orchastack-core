package orchastack.core.mq.requestor.rabitmq;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import orchastack.core.mq.requestor.Message;
import orchastack.core.mq.requestor.TimeStampUtil;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.MessageProperties;

public class RabbitmqUtils {

	static public HashMap convertProperties(BasicProperties props)
			throws UnsupportedEncodingException {
		HashMap prop = new HashMap();

		String appProp = props.getAppId();
		if (appProp != null)
			prop.put(Message.MESSAGE_APPID,
					appProp.getBytes(Message.MESSAGE_ENCODING));

		String corrProp = props.getCorrelationId();
		if (corrProp != null)
			prop.put(Message.MESSAGE_CORRELATION_ID,
					corrProp.getBytes(Message.MESSAGE_ENCODING));

		String expProp = props.getExpiration();
		if (expProp != null)
			prop.put(Message.MESSAGE_EXPIRATION,
					expProp.getBytes(Message.MESSAGE_ENCODING));

		String idProp = props.getMessageId();
		if (idProp != null)
			prop.put(Message.MESSAGE_ID,
					idProp.getBytes(Message.MESSAGE_ENCODING));

		String replyProp = props.getReplyTo();
		if (replyProp != null)
			prop.put(Message.MESSAGE_REPLYTO,
					replyProp.getBytes(Message.MESSAGE_ENCODING));

		String userProp = props.getUserId();
		if (userProp != null)
			prop.put(Message.MESSAGE_RABBITMQ_USERID,
					userProp.getBytes(Message.MESSAGE_ENCODING));

		Integer priorityProp = props.getPriority();
		if (priorityProp != null)
			prop.put(Message.MESSAGE_PRIORITY, priorityProp.toString()
					.getBytes(Message.MESSAGE_ENCODING));

		Date timeProp = props.getTimestamp();
		if (timeProp != null)
			prop.put(Message.MESSAGE_TIMESTAMP, TimeStampUtil.DATE_FORMAT
					.format(timeProp).getBytes(Message.MESSAGE_ENCODING));
		
		if (props.getHeaders() != null)
			prop.putAll(props.getHeaders());

		return prop;
	}

	static public BasicProperties convertProperties(
			HashMap<String, byte[]> props) throws UnsupportedEncodingException {

		Builder builder = new AMQP.BasicProperties.Builder();
		builder.deliveryMode(MessageProperties.PERSISTENT_TEXT_PLAIN
				.getDeliveryMode());
		builder.priority(MessageProperties.PERSISTENT_TEXT_PLAIN.getPriority());

		builder.contentEncoding(Message.MESSAGE_ENCODING);
		builder.contentType("text/plain");
		builder.deliveryMode(2);

		Map<String, Object> headers = new HashMap<String, Object>();

		if (props != null)
			for (String key : props.keySet()) {
				if (key.equals(Message.MESSAGE_ID)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.messageId(new String(v,
								Message.MESSAGE_ENCODING));
				} else if (key.equals(Message.MESSAGE_TIMESTAMP)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.timestamp(TimeStampUtil.parseDate(new String(v,
								Message.MESSAGE_ENCODING)));
				} else if (key.equals(Message.MESSAGE_PRIORITY)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.priority(Integer.valueOf(new String(v,
								Message.MESSAGE_ENCODING)));
				} else if (key.equals(Message.MESSAGE_REPLYTO)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.replyTo(new String(v, Message.MESSAGE_ENCODING));
				} else if (key.equals(Message.MESSAGE_EXPIRATION)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.expiration(new String(v,
								Message.MESSAGE_ENCODING));
				} else if (key.equals(Message.MESSAGE_RABBITMQ_USERID)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.userId(new String(v, Message.MESSAGE_ENCODING));
				} else if (key.equals(Message.MESSAGE_APPID)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.appId(new String(v, Message.MESSAGE_ENCODING));
				} else if (key.equals(Message.MESSAGE_CORRELATION_ID)) {
					byte[] v = props.get(key);
					if (v != null)
						builder.correlationId(new String(v,
								Message.MESSAGE_ENCODING));
				} else if (key.equals(Message.MESSAGE_RABBITMQ_ROUTING_KEY)) {
					// escape routing key
				} else
					headers.put(key, props.get(key));

			}

		// Add the headers to the builder.
		builder.headers(headers);

		BasicProperties theProps = builder.build();

		return theProps;
	}
}
