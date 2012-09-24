/**
 * Project: Zkrw
 * File: zkrw3Test.java
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
public class zkrw3Test extends TestCase {

    Zkrw app;
    public static final String ZK_SERVERS = "127.0.0.1:2181";

    public static final String TEST_ZK_OTHER_GRGRANDCHILDNAME = "/more/blah/blah/test-great-grandchild/1/2";
    public static final String TEST_ZK_OTHER_GRGRANDCHILD_NODE = zkrwTest.TEST_ZK_ROOT_NODE
            + TEST_ZK_OTHER_GRGRANDCHILDNAME;

    public static final String TEST_ZK_QU_NODE = zkrwTest.TEST_ZK_NODE + "/test-queue";

    private void stopOption() {
        // fail("Forcing failure in stopOption()");

    }

    @Before
    public void setUp() {
        System.out.println("setUp...");

        PropertyConfigurator.configure("src/main/resources/log4j.test.properties");

        try {
            initApp();
            if (app.exists(zkrwTest.TEST_ZK_NODE).equals(Zkrw.TRUE_RETURN_VAL)) {
                app.deleteAll(zkrwTest.TEST_ZK_NODE);
            }

        } catch (KeeperException e) {

            e.printStackTrace();
        } catch (InterruptedException e) {

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

    @After
    public void tearDown() {
        try {
            app.close();
        } catch (InterruptedException e) {
            System.err.println("Exception during tearDown()");
            e.printStackTrace();
        }
    }

    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#createIfNotExistsWithParents(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateIfNotExistsWithParents() {
        System.out.println("testCreateIfNotExistsWithParents...");
        stopOption();

        try {
            // create the base node
            String baseValue = "1";
            app.createOrSet(zkrwTest.TEST_ZK_NODE, baseValue);

            // test the method
            app.createIfNotExistsWithParents(zkrwTest.TEST_ZK_GRGRANDCHILD_NODE, "0");

            // check the base node has not been changed, but the others have
            // been created

            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_GRGRANDCHILD_NODE));
            assertEquals(baseValue, app.get(zkrwTest.TEST_ZK_NODE));
            assertEquals("0", app.get(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals("0", app.get(zkrwTest.TEST_ZK_GRANDCHILD_NODE));
            assertEquals("0", app.get(zkrwTest.TEST_ZK_GRGRANDCHILD_NODE));
            // app.syncPath(zkrwTest.TEST_ZK_NODE);

            app.createIfNotExistsWithParents(TEST_ZK_OTHER_GRGRANDCHILD_NODE, "2");
            assertEquals("2", app.get(TEST_ZK_OTHER_GRGRANDCHILD_NODE));

            // System.out.println(
            // "Pausing for testCreateIfNotExistsWithParents to finish.");
            // Thread.sleep(2000);
            //
        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void testQueuePollAndAdd() {
        System.out.println("testQueuePollAndAdd...");
        stopOption();
        try {

            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(TEST_ZK_QU_NODE));
            app.createIfNotExistsWithParents(TEST_ZK_QU_NODE, "0");
            boolean notFoundOK = false;
            try {
                app.queuePoll(TEST_ZK_QU_NODE);
            } catch (RuntimeException e) {
                notFoundOK = true;
            }
            assertTrue(notFoundOK);

            assertEquals(Zkrw.TRUE_RETURN_VAL, app.queueAdd(TEST_ZK_QU_NODE, "new1", 5));

            assertEquals(Zkrw.TRUE_RETURN_VAL, app.queueAdd(TEST_ZK_QU_NODE, "new2", 3));

            System.out.println(app.getChildren(TEST_ZK_QU_NODE));
            ;

            assertEquals("new2", app.queuePoll(TEST_ZK_QU_NODE, 10));
            assertEquals("new1", app.queuePoll(TEST_ZK_QU_NODE));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
        System.out.println("testQueuePollAndAdd -- done");
    }

    @Test
    public void testQueuePollAndAddFromMain() {
        System.out.println("testQueuePollAndAddFromMain...");
        stopOption();
        try {

            System.out.println("Nodes: " + app.getChildren(zkrwTest.TEST_ZK_ROOT_NODE));

            System.out.println("exists...");
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "exists", TEST_ZK_QU_NODE });

            System.out.println("createIfNotExistsWithParents...");
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "createIfNotExistsWithParents",
                    TEST_ZK_QU_NODE, "0" });
            System.out.println("Queue Children: " + app.getChildren(TEST_ZK_QU_NODE));

            // Zkrw.main(new String[]{ZK_SERVERS,"qPoll",TEST_ZK_QU_NODE});

            // "$ZK_SERVERS" "$ZK_SESSTIMEOUT_MS" "$ZK_NUM_RETRIES"
            // "$ZK_RETRY_DELAY_MIN_MS" "$ZK_RETRY_DELAY_MAX_MS"

            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "qAdd", TEST_ZK_QU_NODE, "new1",
                    "5" });
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "qAdd", TEST_ZK_QU_NODE, "new2",
                    "3" });

            System.out.println("Queue Children: " + app.getChildren(TEST_ZK_QU_NODE));

            assertEquals("new2", app.queuePoll(TEST_ZK_QU_NODE, 10));
            assertEquals("new1", app.queuePoll(TEST_ZK_QU_NODE));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
        System.out.println("testQueuePollAndAddFromMain -- done");
    }

    @Test
    public void testQueuePollAndAddFromMainEmptyNull() {
        System.out.println("testQueuePollAndAddFromMainEmptyNull...");
        stopOption();
        try {

            System.out.println("Nodes: " + app.getChildren(zkrwTest.TEST_ZK_ROOT_NODE));

            System.out.println("exists...");
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "exists", TEST_ZK_QU_NODE });

            System.out.println("createIfNotExistsWithParents...");
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "createIfNotExistsWithParents",
                    TEST_ZK_QU_NODE, "0" });
            System.out.println("Queue Children: " + app.getChildren(TEST_ZK_QU_NODE));

            // Zkrw.main(new String[]{ZK_SERVERS,"qPoll",TEST_ZK_QU_NODE});

            // "$ZK_SERVERS" "$ZK_SESSTIMEOUT_MS" "$ZK_NUM_RETRIES"
            // "$ZK_RETRY_DELAY_MIN_MS" "$ZK_RETRY_DELAY_MAX_MS"

            // no priority
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "qAdd", TEST_ZK_QU_NODE, "new2" });

            // empty priority
            Zkrw.main(new String[] { ZK_SERVERS, "", "", "", "", "qAdd", TEST_ZK_QU_NODE, "new3",
                    "" });

            System.out.println("Queue Children: " + app.getChildren(TEST_ZK_QU_NODE));

            assertEquals("new2", app.queuePoll(TEST_ZK_QU_NODE, 10));
            assertEquals("new3", app.queuePoll(TEST_ZK_QU_NODE));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
        System.out.println("testQueuePollAndAddFromMain -- done");
    }

    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#createIfNotExists(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateIfNotExists() {
        System.out.println("testCreateIfNotExists...");
        stopOption();
        try {

            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
            app.createIfNotExists(zkrwTest.TEST_ZK_NODE, "0");

            assertEquals("0", app.get(zkrwTest.TEST_ZK_NODE));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }
    
    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#createIfNotExists(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateIfNotExists_empty() {
        System.out.println("testCreateIfNotExists_empty...");
        stopOption();
        try {

            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
            app.createIfNotExists(zkrwTest.TEST_ZK_NODE, "");

            assertEquals("", app.get(zkrwTest.TEST_ZK_NODE));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#createOrSetWithParents(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateOrSetWithParents() {
        System.out.println("testCreateOrSetWithParents...");
        try {

            // / create the node
            app.createOrSet(zkrwTest.TEST_ZK_NODE, "1");
            assertEquals("1", app.get(zkrwTest.TEST_ZK_NODE));

            // check the child and grandchild nodes don't exist
            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));

            // test the method
            app.createOrSetWithParents(zkrwTest.TEST_ZK_GRANDCHILD_NODE, "0");

            // check the base node has been changed, and the others have
            // been
            // created
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));
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
    
    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#createOrSetWithParents(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateOrSetWithParents_empty() {
        System.out.println("testCreateOrSetWithParents_empty...");
        try {

            // / create the node
            app.createOrSet(zkrwTest.TEST_ZK_NODE, "1");
            assertEquals("1", app.get(zkrwTest.TEST_ZK_NODE));

            // check the child and grandchild nodes don't exist
            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));

            // test the method
            app.createOrSetWithParents(zkrwTest.TEST_ZK_GRANDCHILD_NODE, "");

            // check the base node has been changed, and the others have
            // been
            // created
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals(Zkrw.TRUE_RETURN_VAL, app.exists(zkrwTest.TEST_ZK_GRANDCHILD_NODE));
            assertEquals("", app.get(zkrwTest.TEST_ZK_NODE));
            assertEquals("", app.get(zkrwTest.TEST_ZK_CHILD2_NODE));
            assertEquals("", app.get(zkrwTest.TEST_ZK_GRANDCHILD_NODE));

            // System.out.println("Pausing for testCreateOrSetWithParents to finish.");
            // Thread.sleep(2000);

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

}
