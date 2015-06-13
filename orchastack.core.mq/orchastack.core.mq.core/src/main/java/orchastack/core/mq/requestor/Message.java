package orchastack.core.mq.requestor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * we only support byte array content, and properties will be string~byte array
 * pair too. It's application's responsibility to serialize and deserialize
 * objects
 * 
 * @author active
 * 
 */
public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	final public static String MESSAGE_ENCODING = "UTF-8";
	final public static String MESSAGE_ID = "message_id";
	final public static String MESSAGE_TIMESTAMP = "message_timestamp";
	final public static String MESSAGE_EXPIRATION = "message_expiration";
	final public static String MESSAGE_PRIORITY = "message_priority";
	final public static String MESSAGE_DESTINATION = "message_destination";
	final public static String MESSAGE_REPLYTO = "message_replyto";
	final public static String MESSAGE_CALLER_USERID = "message_caller_userid";
	final public static String MESSAGE_APPID = "message_appid";
	final public static String MESSAGE_CORRELATION_ID = "message_correlation_id";

	final public static String MESSAGE_RABBITMQ_ROUTING_KEY = "message_rabbitmq_routing_key";
	final public static String MESSAGE_RABBITMQ_USERID = "message_userid";

	final public static String MESSAGE_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	private byte[] content;
	private HashMap<String, byte[]> properties = new HashMap<String, byte[]>();

	public Message(byte[] content) {
		super();
		this.content = content;
	}

	public Message(byte[] content, HashMap<String, byte[]> properties) {
		super();
		this.content = content;
		this.properties = properties;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public HashMap<String, byte[]> getProperties() {
		return properties;
	}

	public void addProperty(String key, byte[] value) {
		this.properties.put(key, value);
	}

	public void setProperties(HashMap<String, byte[]> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return "Message [content=" + Arrays.toString(content) + ", properties="
				+ properties + "]";
	}

//	private void writeObject(ObjectOutputStream os) throws IOException,
//			ClassNotFoundException {
//
//	}
//
//	private void readObject(ObjectInputStream is) throws IOException,
//			ClassNotFoundException {
//
//	}

}
