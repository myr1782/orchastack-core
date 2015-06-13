package orchastack.core.security.shiro.ext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serializer {
	public static byte[] serialize(Object msg) throws Exception {
		if (msg == null)
			return null;

		ByteArrayOutputStream bytmesg = new ByteArrayOutputStream();

		ObjectOutputStream out = new ObjectOutputStream(bytmesg);
		out.writeObject(msg);
		out.close();

		return bytmesg.toByteArray();
	}

	public static Object deserialize(byte[] msgByte) throws Exception {

		if (msgByte == null)
			return null;

		ByteArrayInputStream bytmesg = new ByteArrayInputStream(msgByte);

		ObjectInputStream in = new ObjectInputStream(bytmesg);
		Object msg = in.readObject();
		in.close();

		return msg;
	}
}
