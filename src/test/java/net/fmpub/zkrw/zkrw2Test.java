/**
 * Project: Zkrw
 * File: zkrw2Test.java
 * Created on: Jun 21, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 */
package net.fmpub.zkrw;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.KeeperException;
import org.junit.*;

/**
 * @author brogosky Note: I am splitting the unit tests for Zkrw into 3 classes,
 *         since I had a strange problem with my local Zookeeper instance due to
 *         resetting the nodes repeatedly.
 */
public class zkrw2Test extends TestCase {

	Zkrw app;

	int setupFailureCount = 0;

	private boolean stopOption() {
		if (setupFailureCount > 0) {
			System.out.println("Forcing skip of tests in stopOption()");
			
			return true;
		}
		return false;

	}

	@Before
	public void setUp() {
		System.out.println("setUp...");

		PropertyConfigurator.configure("src/main/resources/log4j.test.properties");
		
		initApp();
		boolean success = false;

		setupFailureCount = 0;
		
		while (!success && setupFailureCount < 5) {
			try {

				if (app.exists(zkrwTest.TEST_ZK_NODE).equals(Zkrw.TRUE_RETURN_VAL)) {
					app.deleteAll(zkrwTest.TEST_ZK_NODE);
					
				}
				success = true;
			} catch (KeeperException e) {

				if(e.code() == KeeperException.Code.CONNECTIONLOSS) {
					System.err.println("Proceeding after connection loss...\n");
				}
				else {
					e.printStackTrace();
				}
			} catch (InterruptedException e) {

				e.printStackTrace();
			}

			if (!success) {
				try {
					setupFailureCount++;
					System.out
							.println(" retrying after a pause.");
					//app.syncPath(zkrwTest.TEST_ZK_NODE);
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if (!success) {
			System.err
					.println("Skipping test because of repeated Zookeeper failures.");
		}
		else {
			System.out
			.println("Success after repeated Zookeeper failures.");
			setupFailureCount = 0;
		}

	}

	@After 
	public void tearDown() {
		try {
			app.close();
		} catch (InterruptedException e) {
			System.err.println("Exception during tearDown()");
			e.printStackTrace();
		}
	}
	
	public Zkrw initApp() {
		try {
			if (app == null) {
				app = new Zkrw(zkrwTest.ZK_SERVERS);
			}
			return app;
		} catch (IOException e) {

			e.printStackTrace();
		}
		fail();
		return null;
	}

	/**
	 * Test method for
	 * {@link net.fmpub.zkrw.Zkrw#createOrSetWithParents(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testCreateOrSetWithParents() {
		System.out.println("testCreateOrSetWithParents...");
		if (!stopOption()) {

			try {

				// / create the node
				app.createOrSet(zkrwTest.TEST_ZK_NODE, "1");
				assertEquals("1", app.get(zkrwTest.TEST_ZK_NODE));

				// check the child and grandchild nodes don't exist
				assertEquals(Zkrw.FALSE_RETURN_VAL,
						app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
				assertEquals(Zkrw.FALSE_RETURN_VAL,
						app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));

				// test the method
				app.createOrSetWithParents(zkrwTest.TEST_ZK_GRANDCHILD_NODE,
						"0");

				// check the base node has been changed, and the others have
				// been
				// created
				assertEquals(Zkrw.TRUE_RETURN_VAL,
						app.exists(zkrwTest.TEST_ZK_NODE));
				assertEquals(Zkrw.TRUE_RETURN_VAL,
						app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
				assertEquals(Zkrw.TRUE_RETURN_VAL,
						app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));
				assertEquals("0", app.get(zkrwTest.TEST_ZK_NODE));
				assertEquals("0", app.get(zkrwTest.TEST_ZK_CHILD2_NODE));
				assertEquals("0", app.get(zkrwTest.TEST_ZK_GRANDCHILD_NODE));

				// System.out.println("Pausing for testCreateOrSetWithParents to finish.");
				// Thread.sleep(2000);

			} catch (Exception e) {

				e.printStackTrace();
				fail();
			}
		}

	}
	
	/**
	 * Test method for {@link net.fmpub.zkrw.Zkrw#delete(java.lang.String)}.
	 */
	@Test
	public void testDelete_Exists() {
		System.out.println("testDelete_Exists...");
		if(!stopOption()) {
			try {
	
				assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
				app.create(zkrwTest.TEST_ZK_NODE, "0");
				assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
				app.delete(zkrwTest.TEST_ZK_NODE);
				assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
	
			} catch (Exception e) {
	
				e.printStackTrace();
				fail();
			}
		}
	}

}
