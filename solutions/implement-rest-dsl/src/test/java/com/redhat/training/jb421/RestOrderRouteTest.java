package com.redhat.training.jb421;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.CamelSpringJUnit4ClassRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.AssertThrows;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.redhat.training.jb421.model.Order;
import com.redhat.training.jb421.model.OrderItem;

@RunWith(CamelSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/bundle-camel-context.xml" })
@UseAdviceWith(true)
public class RestOrderRouteTest {

	private static final String TEST_DATA_FOLDER = "/home/student/jb421/data/orderItemJSON/";

	@Autowired
	public CamelContext camelContext;

	private EntityManagerFactory emf;

	private EntityManager em;

	@PersistenceUnit
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}

	@Before
	public void setup() {
		em = emf.createEntityManager();
		clearData();
	}

	@Test
	@DirtiesContext
	public void testCreateOrder() throws Exception {
		camelContext.start();
		Thread.sleep(5000);

		// Create order with id 1
		callRestEndpoint("POST", null, 1);

		Order result = em.find(Order.class, 1);

		assertNotNull(result);
	}

	@Test
	@DirtiesContext
	public void testDelete() throws Exception {
		camelContext.start();
		Thread.sleep(5000);

		// Create order with id 1
		callRestEndpoint("POST", null, 1);

		// Delete order with id 1
		callRestEndpoint("DELETE", null, 1);

		Order result = em.find(Order.class, 1);

		assertNull(result);
	}

	@Test
	@DirtiesContext
	public void testUpdate() throws Exception {
		camelContext.start();
		Thread.sleep(5000);

		// Create order with id 1
		callRestEndpoint("POST", null, 1);

		// Update order with id 1
		File testFile = new File(TEST_DATA_FOLDER + "orderItem.json");
		callRestEndpoint("PUT", testFile, 1);

		Order result = em.find(Order.class, 1);

		assertNotNull(result);
		assertEquals(1, result.getOrderItems().size());
	}

	@Test
	@DirtiesContext
	public void testGet() throws Exception {
		camelContext.start();
		Thread.sleep(5000);

		// Create order with id 1
		callRestEndpoint("POST", null, 1);

		// Update order with id 1
		File testFile = new File(TEST_DATA_FOLDER + "orderItem.json");
		callRestEndpoint("PUT", testFile, 1);

		String order = getRESTData(1);
		assertNotNull(order);
	}

	@After
	public void cleanUp() {
		clearData();
	}

	private void callRestEndpoint(String httpMethod, File requestBody, Integer orderId) {
		try {
			URL url = new URL("http://localhost:8080/orders/" + orderId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod(httpMethod);
			conn.setRequestProperty("Content-Type", "application/json");

			OutputStream os = conn.getOutputStream();
			if (requestBody != null) {
				os.write(Files.readAllBytes(requestBody.toPath()));
			}
			os.flush();

			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("REST Endpoint returned HTTP code " + conn.getResponseCode() + ", message: "
						+ conn.getResponseMessage());
			}

			conn.disconnect();
		} catch (Exception e) {
			throw new RuntimeException("Error hitting REST Endpoint: " + e.getMessage());
		}
	}

	private String getRESTData(Integer orderId) throws Exception{
		StringBuilder result = new StringBuilder();
		URL url = new URL("http://localhost:8080/orders/" + orderId);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	private void clearData() {
		em.getTransaction().begin();
		Query ordersQuery = em.createNamedQuery("getAllOrders");
		List<Order> orders = ordersQuery.getResultList();
		for (Order o : orders) {
			for (OrderItem oi : o.getOrderItems()) {
				em.remove(oi);
			}
			em.remove(o);
		}
		em.flush();
		em.getTransaction().commit();
	}

}
