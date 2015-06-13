package orchastack.core.mq.requestor;

public class RequestorKey {
	public String brokerURL;
	public Thread client;
	public String queue;
	public RequestorKey(String brokerURL, String queue, Thread client) {
		super();
		this.brokerURL = brokerURL;
		this.queue = queue;
		this.client = client;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((brokerURL == null) ? 0 : brokerURL.hashCode());
		result = prime * result
				+ ((client == null) ? 0 : client.hashCode());
		result = prime * result + ((queue == null) ? 0 : queue.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequestorKey other = (RequestorKey) obj;
		
		if (brokerURL == null) {
			if (other.brokerURL != null)
				return false;
		} else if (!brokerURL.equals(other.brokerURL))
			return false;
		if (client == null) {
			if (other.client != null)
				return false;
		} else if (!client.equals(other.client))
			return false;
		if (queue == null) {
			if (other.queue != null)
				return false;
		} else if (!queue.equals(other.queue))
			return false;
		return true;
	}
}
