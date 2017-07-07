package com.redhat.training.routes;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/camel-context.xml" })
@UseAdviceWith(true)
public class JmsRouteTest {

	@Autowired
	private CamelContext camelContext;

	@Produce(uri = "file:"+JmsRouteBuilder.DIRECTORY)
	private ProducerTemplate fileTemplate;

	private JdbcTemplate jdbc;

	@Before
	public void setUp() {
		DataSource ds = camelContext.getRegistry().lookupByNameAndType("atomikosDataSource", DataSource.class);
		jdbc = new JdbcTemplate(ds);
		System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "*");
		CamelSpringTestSupport.deleteDirectory(JmsRouteBuilder.DIRECTORY);

	}

	@After
	public void tearDown() {
		if (jdbc != null) {
			jdbc.execute("drop table order_");
		}

	}

	@Test
	@DirtiesContext
	public void testDelivery() throws Exception {
		NotifyBuilder builder = new NotifyBuilder(camelContext).whenDone(1).create();
		camelContext.start();
		fileTemplate.sendBody(VALID_ORDER);
		Thread.sleep(2000);
//		Assert.assertTrue(builder.matches(5, TimeUnit.SECONDS));
//		int rows = jdbc.queryForObject("select count(1) from order_", Integer.class);
//		Assert.assertEquals(0, rows);
		Assert.fail("Test not implemented");
	}

	@Test
	@DirtiesContext
	public void testRedelivery() throws Exception {
		NotifyBuilder builder = new NotifyBuilder(camelContext).whenDone(1).create();
		ModelCamelContext context = camelContext.adapt(ModelCamelContext.class);
		context.getRouteDefinition("MessageToDB").adviceWith(context, new RouteBuilder() {
			public void configure() {
//				interceptSendToEndpoint("jms:*").choice().when(header("JMSRedelivered").isEqualTo("false"))
//						.throwException(new ConnectException("Cannot connect to the database"));
			}

		});
		camelContext.start();
		fileTemplate.sendBody(INVALID_ORDER);
		Thread.sleep(2000);
//		int rows = jdbc.queryForObject("select count(1) from order_", Integer.class);
//		Assert.assertTrue(builder.matches(5, TimeUnit.SECONDS));
//		Assert.assertEquals(1, rows);
		Assert.fail("Test not implemented");

	}

	private static final String VALID_ORDER = "<order>" + "<name>Mr. John Doe</name>" + "<discount>10</discount>"
			+ "</order>";
	private static final String INVALID_ORDER = "<order>" + "<name>Mr. John Doe</name>" + "<discount>150</discount>"
			+ "</order>";

}
