package orchastack.core.itest.test;

import static orchastack.core.itest.RegressionConfiguration.regressionDefaults;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Ignore;

import orchastack.core.mq.requestor.Message;
import orchastack.core.mq.requestor.MqRequestor;
import orchastack.core.mq.requestor.MqRequestorFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

@RunWith(PaxExam.class)
@Ignore
public class RabbitMqTest {

	// @Inject
	// private FeaturesService featuresService;
	//
	// @Test
	// public void testXXX() throws Exception {
	// // assertTrue(featuresService.isInstalled(featuresService
	// // .getFeature("scheduler")));
	// // assertTrue(featuresService.isInstalled(featuresService
	// // .getFeature("wrapper")));
	// assertTrue(true);
	// }

	// @Inject
	// private BillingService billingService;

	@Inject
	private LogService log;

	@Inject
	private MqRequestorFactory mqFactory;

	private class TestThread extends Thread {
		@Override
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

				Assert.assertEquals("hello", new String(m.getContent()));

				HashMap<String, byte[]> ppp = m.getProperties();

				Assert.assertNotNull(ppp);
				Assert.assertEquals("guest",
						new String(ppp.get(Message.MESSAGE_RABBITMQ_USERID)));
				Assert.assertEquals("mathews",
						new String(ppp.get(Message.MESSAGE_CALLER_USERID)));
				Assert.assertEquals("hhh", new String(ppp.get("test-props")));
				synchronized (this) {
					wait(120000);
				}
				r.close();
			} catch (Exception e) {
				log.log(LogService.LOG_ERROR, "Service Error!", e);
			}
		}
	}

	@Test
	public void testMQService() {
		new TestThread().start();
		new TestThread().start();

		synchronized (this) {
			try {
				wait(180000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		// probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*");
		probe.setHeader(Constants.IMPORT_PACKAGE,
				"orchastack.core.mq.requestor");
		probe.setHeader(Constants.EXPORT_PACKAGE, "orchastack.core.itest.test");

		return probe;
	}

	@Configuration
	public Option[] config() {
		return new Option[] {
				// karafDistributionConfiguration()
				// .frameworkUrl(
				// maven().groupId("org.apache.karaf")
				// .artifactId("apache-karaf").type("zip")
				// .version("3.0.0"))
				// .karafVersion("3.0.0").name("karaf")
				// .unpackDirectory(new File("target/paxexam/"))
				// .useDeployFolder(false),

				// bootDelegationPackages("sun.*", "com.sun.*"),
				regressionDefaults("target/paxexam/unpack2/"),

				// regressionDefaults(),
				keepRuntimeFolder(),

				// workingDirectory("target/"),
				systemTimeout(600000),

				// vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8889"),
				// mavenBundle().groupId("com.activenetwork.soi.bundles")
				// .artifactId("com.activenetwork.soi.bundles.drools")
				// .version("5.4.0.Final").startLevel(70).start(),

				// systemProperty("file.encoding").value("UTF-8"),

				// debugConfiguration("8889", true),
				// configureConsole().ignoreLocalConsole(),
				logLevel(LogLevelOption.LogLevel.INFO),

				replaceConfigurationFile(
						"etc/org.apache.karaf.management.cfg",
						new File(
								"src/test/resources/org.apache.karaf.management.cfg")),

				replaceConfigurationFile(
						"etc/orchastack.core.mq.params.cfg",
						new File(
								"src/test/resources/orchastack.core.mq.params.cfg")),

				replaceConfigurationFile("etc/overrides.properties", new File(
						"src/test/resources/overrides.properties")),

				editConfigurationFilePut(
						"etc/org.apache.karaf.features.cfg",
						"featuresRepositories",
						"mvn:org.apache.karaf.features/standard/3.0.2/xml/features,"
								+ "mvn:org.apache.karaf.features/enterprise/3.0.2/xml/features,"
								// +
								// "mvn:org.ops4j.pax.web/pax-web-features/3.0.2/xml/features,"
								+ "mvn:org.apache.karaf.features/spring/3.0.2/xml/features,"
								+ "mvn:orchastack.core.features/orchastack/0.0.1/xml/features"),

				features(maven().groupId("orchastack.core.features")
						.artifactId("orchastack").type("xml").version("0.0.1")
						.classifier("features"),
						"orchastack-rabbitmq-requestor"),

		// useOwnExamBundlesStartLevel(60), junitBundles(),
		// provision(scanDir("target/")),
		// Test on both equinox and felix
		// equinox(),
		// felix()
		};
	}
}
