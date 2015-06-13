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

import javax.inject.Inject;

import orchastack.core.itest.biz.MqBizService;

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
// @Ignore
public class KafkaMqTxTest {

	@Inject
	private LogService log;

	@Inject
	private MqBizService mqBiz;

	@Test
	public void testMQService() {
		try {
//			mqBiz.sendMessageWithin();
			mqBiz.receiveMessageWithin();
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "MQ tx biz test error", e);
			fail();
		}
	}

	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		// probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*");
		probe.setHeader(Constants.IMPORT_PACKAGE, "orchastack.core.itest.biz");
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
						.classifier("features"), "orchastack-service"),

		// useOwnExamBundlesStartLevel(60), junitBundles(),
		// provision(scanDir("target/")),
		// Test on both equinox and felix
		// equinox(),
		// felix()
		};
	}
}
