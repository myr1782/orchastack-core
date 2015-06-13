package orchastack.core.executor.impl;

import java.util.Dictionary;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import orchastack.core.executor.ExecutorService;
import orchastack.core.executor.Job;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Updated;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.cm.ConfigurationException;

@Component(managedservice = "orchastack.core.executor")
@Provides(specifications = { ExecutorService.class })
@Instantiate
public class ExecutorServiceImpl implements ExecutorService {

	private static Logger log = Logger.getLogger(ExecutorServiceImpl.class
			.getName());

	final public String DEFAULT_CORE_POOL_SIZE = "defaultCorePoolSize";
	@Property
	private int defaultCorePoolSize = 20;

	final public String DEFAULT_MAXIMUM_POOL_SIZE = "defaultMaximumPoolSize";
	@Property
	private int defaultMaximumPoolSize = 300;

	final public String DEFAULT_KEEP_ALIVE_TIME = "defaultKeepAliveTime";
	/**
	 * keep the thread alive for ten minutes
	 */
	@Property
	private long defaultKeepAliveTime = 600;

	final public String DEFAULT_BLOCKING_DEPTH = "defaultBlockingDepth";

	@Property
	private int defaultBlockingDepth = 100;

	static RejectedExecutionHandler defaultRejectHandler = new RejectedExecutionHandler() {
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			// TODO log error here

		}
	};

	public ExecutorServiceImpl() {
		if (pool == null)
			pool = new ThreadPoolExecutor(this.defaultCorePoolSize,
					this.defaultMaximumPoolSize, this.defaultKeepAliveTime,
					TimeUnit.SECONDS, this.defaultBlockingQueue,
					ExecutorServiceImpl.defaultRejectHandler);
	}

	@Validate
	public void start() {
		log.log(Level.INFO, "ExecutorService started");
	}

	@Invalidate
	public void stop() {
		log.log(Level.INFO, "ExecutorService stopped");
	}

	private LinkedBlockingQueue defaultBlockingQueue = new LinkedBlockingQueue(
			this.defaultBlockingDepth);

	private static ThreadPoolExecutor pool;

	public void execute(final Job job) {
		ExecutorServiceImpl.pool.execute(new Runnable() {
			public void run() {
				try {
					job.run();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Updated
	public void updated(Dictionary properties) throws ConfigurationException {
		String defaultcorepoolsize = (String) properties
				.get(DEFAULT_CORE_POOL_SIZE);
		if (defaultcorepoolsize != null) {
			defaultCorePoolSize = Integer.parseInt(defaultcorepoolsize);
		}
		String defaultmaximumpoolsize = (String) properties
				.get(DEFAULT_MAXIMUM_POOL_SIZE);
		if (defaultmaximumpoolsize != null) {
			defaultMaximumPoolSize = Integer.parseInt(defaultmaximumpoolsize);
		}
		String defaultkeepalivetime = (String) properties
				.get(DEFAULT_KEEP_ALIVE_TIME);
		if (defaultkeepalivetime != null) {
			defaultKeepAliveTime = Integer.parseInt(defaultkeepalivetime);
		}

		String defaultblockingdepth = (String) properties
				.get(DEFAULT_BLOCKING_DEPTH);
		if (defaultblockingdepth != null) {
			defaultBlockingDepth = Integer.parseInt(defaultblockingdepth);
		}

		pool.shutdown();
		pool = null;

		defaultBlockingQueue = new LinkedBlockingQueue(
				this.defaultBlockingDepth);

		pool = new ThreadPoolExecutor(this.defaultCorePoolSize,
				this.defaultMaximumPoolSize, this.defaultKeepAliveTime,
				TimeUnit.SECONDS, this.defaultBlockingQueue,
				ExecutorServiceImpl.defaultRejectHandler);

	}

}
