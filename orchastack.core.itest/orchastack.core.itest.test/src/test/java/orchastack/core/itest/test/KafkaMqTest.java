package orchastack.core.itest.test;

import static orchastack.core.itest.RegressionConfiguration.regressionDefaults;
import static org.junit.Assert.*;
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
import java.util.LinkedList;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Ignore;

import orchastack.core.itest.MultithreadTestUtil;
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
public class KafkaMqTest {

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

	@Test
	public void testMQService() {
		KafkaJob tr1, tr2;

		tr1 = new KafkaJob(mqFactory);
		tr2 = new KafkaJob(mqFactory);
		LinkedList trs = new LinkedList();
		trs.add(tr1);
		trs.add(tr2);

		try {
			MultithreadTestUtil.assertConcurrent("test threaded MqRequestor",
					trs, 180);
		} catch (java.lang.AssertionError e) {
			fail("test in thread fail assertions!");
		} catch (InterruptedException e) {
			log.log(LogService.LOG_ERROR, "test error", e);
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
						.classifier("features"), "orchastack-kafka-requestor"),

		// useOwnExamBundlesStartLevel(60), junitBundles(),
		// provision(scanDir("target/")),
		// Test on both equinox and felix
		// equinox(),
		// felix()
		};
	}
}
