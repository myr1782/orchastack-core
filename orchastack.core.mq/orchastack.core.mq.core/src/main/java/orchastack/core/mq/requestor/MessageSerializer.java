package orchastack.core.mq.requestor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessageSerializer {

	public static byte[] serialize(Message msg) throws Exception {
		if (msg == null)
			return null;
		
		ByteArrayOutputStream bytmesg = new ByteArrayOutputStream();

		ObjectOutputStream out = new ObjectOutputStream(bytmesg);
		out.writeObject(msg);
		out.close();

		return bytmesg.toByteArray();
	}

	public static Message deserialize(byte[] msgByte) throws Exception {
		
		if (msgByte == null)
			return null;
		
		ByteArrayInputStream bytmesg = new ByteArrayInputStream(msgByte);

		ObjectInputStream in = new ObjectInputStream(bytmesg);
		Message msg = (Message) in.readObject();
		in.close();

		return msg;
	}
}
