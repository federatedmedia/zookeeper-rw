/**
 * Project: Zkrw
 * File: zkrwTest.java
 * Created on: Jun 17, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 */
package net.fmpub.zkrw;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.log4j.*;
import org.apache.zookeeper.*;
import org.junit.*;

/**
 * @author brogosky
 * 
 */
public class zkrwTest extends TestCase implements Watcher {

    private static Logger logger;
    
    Zkrw app;

    public static final String ZK_SERVERS = "127.0.0.1:2181";

    public static final String TEST_ZK_ROOT_NODE = "/DS-test"; // DO NOT DELETE
    public static final String TEST_ZK_NODE = TEST_ZK_ROOT_NODE + "/main";

    public static final String TEST_ZK_CHILDNAME = "/test-child";

    public static final String TEST_ZK_GRANDCHILDNAME = "/test-grandchild";

    public static final String TEST_ZK_CHILD1_NODE = TEST_ZK_NODE + TEST_ZK_CHILDNAME + "1";
    public static final String TEST_ZK_CHILD2_NODE = TEST_ZK_NODE + TEST_ZK_CHILDNAME + "2";
    public static final String TEST_ZK_GRANDCHILD_NODE = TEST_ZK_CHILD2_NODE
            + TEST_ZK_GRANDCHILDNAME;

    public static final String TEST_ZK_GRGRANDCHILDNAME = "/more/blah/test-great-grandchild";

    public static final String TEST_ZK_GRGRANDCHILD_NODE = TEST_ZK_CHILD2_NODE
            + TEST_ZK_GRANDCHILDNAME + TEST_ZK_GRGRANDCHILDNAME;

    /**
     * Names of methods to test in testMain(). The order is important.
     */
    public static final String[] METHOD_NAMES = "create|deleteAll|createIfNotExists|createIfNotExistsWithParents|createOrSet|createOrSetWithParents|get|getAll|getChildren|getChildrenOnly|getCTime|getMTime|getNumChildren|qAdd|qPoll|set|delete|exists|"
            .split("\\|");

    /**
     * Order is important. This is the order in which they must be deleted.
     */
    public static final String[] TEST_ZK_NODES = new String[] { TEST_ZK_GRANDCHILD_NODE,
            TEST_ZK_CHILD1_NODE, TEST_ZK_CHILD2_NODE, TEST_ZK_NODE };

    /**
     * Test method for {@link net.fmpub.zkrw.Zkrw#main(java.lang.String[])}.
     */
    @Test
    public void testMain() {
        System.out.println("testMain...");
        stopOption();
        for (String methodName : METHOD_NAMES) {
            System.out.println("Calling " + methodName);
            // "$ZK_SERVERS" "$ZK_SESSTIMEOUT_MS" "$ZK_NUM_RETRIES" "$ZK_RETRY_DELAY_MIN_MS" "$ZK_RETRY_DELAY_MAX_MS"
            Zkrw.main(new String[] { ZK_SERVERS, "","","","", methodName, TEST_ZK_NODE, "0" });
        }
        


    }
    
    /**
     * Test method for {@link net.fmpub.zkrw.Zkrw#main(java.lang.String[])}.
     */
    @Test
    public void testMain_empty() {
        System.out.println("testMain_empty...");
        stopOption();
        for (String methodName : METHOD_NAMES) {
            System.out.println("Calling " + methodName);
            // "$ZK_SERVERS" "$ZK_SESSTIMEOUT_MS" "$ZK_NUM_RETRIES" "$ZK_RETRY_DELAY_MIN_MS" "$ZK_RETRY_DELAY_MAX_MS"
            Zkrw.main(new String[] { ZK_SERVERS, "","","","", methodName, TEST_ZK_NODE, "" });
        }
        


    }
    
    
    @Test
    public void testRetriesAndTimeout() throws InterruptedException {
        
        System.out.println("testRetriesAndTimeout...");
        stopOption();
        
        // check works with a node, with 5 retries, low wait
        assertEquals(0,Zkrw.run(ZK_SERVERS, 3000, 5, 0, 5, "exists", TEST_ZK_NODE, "", ""));
        assertEquals(0,Zkrw.run(ZK_SERVERS, 3000, 5, 0, 5, "create", TEST_ZK_NODE, "new", ""));
        assertEquals(0,Zkrw.run(ZK_SERVERS, 3000, 5, 0, 5, "get", TEST_ZK_NODE, "", ""));
        
        // check it fails with a non-existent node, with 5 retries
        final String node = TEST_ZK_NODE + "/testTiming";
        
        
        final int retries = 5;
        
        final int minDur = 10;
        final int range = 1000;
        
        long startFailure = System.currentTimeMillis();
        assertEquals(1,Zkrw.run(ZK_SERVERS, 3000, retries, minDur, range, "get", node, "", ""));
        long failureRuntime = System.currentTimeMillis() - startFailure;
        assertTrue(failureRuntime > (minDur * retries) && failureRuntime < ( (minDur + range) * retries) );
        
        // check it passes with a non-existent node, with 15 retries
        
        TestRunThread testThread = new TestRunThread(retries,minDur,range,node);
        testThread.start();
        Thread.sleep(minDur+range);
        // create the node
        System.out.println("Attempting testRetriesAndTimeout to create " + node);
        assertEquals(0,Zkrw.run(ZK_SERVERS, 3000, 15, 0, 1000, "create", node, "new", ""));
        
        // wait for test thread to complete
        testThread.join();
        
        assertEquals(0,testThread.getReturnVal());
        assertTrue(testThread.getRuntime() > minDur && testThread.getRuntime() < ( (minDur + range) * retries) );
        
    }
    
    private class TestRunThread extends Thread {

        int returnVal;
        
        int retries;
        int minDur;
        int range;
        String node;
        long runtime = 0;
       
        /**
         * @param retries
         * @param minDur
         * @param range
         * @param node
         */
        public TestRunThread(int retries, int minDur, int range, String node) {
            super();
            this.retries = retries;
            this.minDur = minDur;
            this.range = range;
            this.node = node;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            System.out.println("\nAttempting testRetriesAndTimeout threaded test of " + node);
            
            // check it eventually succeeds with a non-existent node, with  retries
            returnVal = (Zkrw.run(ZK_SERVERS, 3000, retries, minDur, range, "get", node, "", ""));
            
            runtime = System.currentTimeMillis() - start;
            System.out.println("\n Finished testRetriesAndTimeout threaded test with "+returnVal+" in " + runtime+ " ms");
            
            
            //success2 = (runtime > (minDur) && runtime < ( (minDur + range) * retries) );
        }
        
        public int getReturnVal() {
            return returnVal;
        }
        
        public long getRuntime() {
            return runtime;
        }
        
    }

    private void stopOption() {
        // fail("Forcing failure in stopOption()");

    }

    @Before
    public void setUp() {
        System.out.println("setUp...");
        
        PropertyConfigurator.configure("src/main/resources/log4j.test.properties");
        
        logger = Logger.getLogger(zkrwTest.class);
        logger.info("setUp()...");
        initApp();

        // for (String zknode : TEST_ZK_NODES) {
        try {

            // ZooKeeper zk = new ZooKeeper(ZK_SERVERS, 3000, this);

            // if (zk.exists(TEST_ZK_NODE, false) != null) {
            if (app.exists(TEST_ZK_NODE).equals(Zkrw.TRUE_RETURN_VAL)) {
                app.deleteAll(TEST_ZK_NODE);
            }
            

        } catch (KeeperException e) {
            System.err.println("setUp failed...");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("setUp interrupted...");
            e.printStackTrace();
        }
        /*
         * catch (IOException e1) { System.err.println("setUp I/O problem...");
         * e1.printStackTrace(); }
         */

    }

    public Zkrw initApp() {
        try {
            // if (app == null) {
            app = new Zkrw(ZK_SERVERS);
            // }
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
     * Test method for {@link net.fmpub.zkrw.Zkrw#get(java.lang.String)}.
     */
    @Test
    public void testGet() {
        System.out.println("testGet...");
        stopOption();

        boolean failsWhenMissing = false;
        try {
            app.get(TEST_ZK_NODE);
        } catch (KeeperException e) {
            failsWhenMissing = true;
        } catch (InterruptedException e) {

            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(failsWhenMissing);

        try {
            app.create(TEST_ZK_NODE, "1");
            assertEquals("1", app.get(TEST_ZK_NODE));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    /**
     * Test method for {@link net.fmpub.zkrw.Zkrw#getAll(java.lang.String)}.
     */
    @Test
    public void testGetAll() {
        System.out.println("testGetAll...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");
            app.create(TEST_ZK_CHILD1_NODE, "1");
            app.create(TEST_ZK_CHILD2_NODE, "2");
            app.create(TEST_ZK_GRANDCHILD_NODE, "3");
            String output = app.getAll(TEST_ZK_NODE);
            System.out.println(output);
            assertTrue(output.contains(TEST_ZK_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && output.contains(TEST_ZK_CHILD1_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && output.contains(TEST_ZK_CHILD2_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && output.contains(TEST_ZK_GRANDCHILD_NODE + Zkrw.DEFAULT_LIST_DELIMITER));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test method for {@link net.fmpub.zkrw.Zkrw#getChildren(java.lang.String)}
     * .
     */
    @Test
    public void testGetChildren() {
        System.out.println("testGetChildren...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");
            app.create(TEST_ZK_CHILD1_NODE, "1");
            app.create(TEST_ZK_CHILD2_NODE, "2");
            app.create(TEST_ZK_GRANDCHILD_NODE, "3");
            String output = app.getChildren(TEST_ZK_NODE);
            System.out.println(output);
            assertTrue(output.contains(TEST_ZK_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && output.contains(TEST_ZK_CHILD1_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && output.contains(TEST_ZK_CHILD2_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && !output.contains(TEST_ZK_GRANDCHILD_NODE + Zkrw.DEFAULT_LIST_DELIMITER));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetChildrenOnly() {
        System.out.println("testGetChildrenOnly...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");
            app.create(TEST_ZK_CHILD1_NODE, "1");
            app.create(TEST_ZK_CHILD2_NODE, "2");
            app.create(TEST_ZK_GRANDCHILD_NODE, "3");
            String output = app.getChildrenOnly(TEST_ZK_NODE);
            System.out.println(output);
            assertTrue(output.contains(TEST_ZK_CHILD1_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && output.contains(TEST_ZK_CHILD2_NODE + Zkrw.DEFAULT_LIST_DELIMITER)
                    && !output.contains(TEST_ZK_GRANDCHILD_NODE + Zkrw.DEFAULT_LIST_DELIMITER));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void testGetNumChildren() {
        System.out.println("testGetNumChildren...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");
            app.create(TEST_ZK_CHILD1_NODE, "1");
            app.create(TEST_ZK_CHILD2_NODE, "2");
            app.create(TEST_ZK_GRANDCHILD_NODE, "3");
            String output = app.getNumChildren(TEST_ZK_NODE);
            System.out.println(output);
            assertEquals("2",output);

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetCTime() {
        System.out.println("testGetCTime...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");

            long now = System.currentTimeMillis();
            String output = app.getCTime(TEST_ZK_NODE);
            System.out.println(output + " vs. expected: " + now);
            // equal to within less than one second
            assertEquals((float) now, Float.valueOf(output), 100);

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void testGetMTime() {
        System.out.println("testGetMTime...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");

            long now = System.currentTimeMillis();
            String output = app.getMTime(TEST_ZK_NODE);
            System.out.println(output + " vs. expected: " + now);
            // equal to within less than one second
            assertEquals((float) now, Float.valueOf(output), 100);

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }
    
    /**
     * Test method for {@link net.fmpub.zkrw.Zkrw#deleteAll(java.lang.String)}.
     */
    @Test
    public void testDeleteAll() {
        System.out.println("testDeleteAll...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");
            app.create(TEST_ZK_CHILD1_NODE, "1");
            app.create(TEST_ZK_CHILD2_NODE, "2");
            app.create(TEST_ZK_GRANDCHILD_NODE, "3");
            app.deleteAll(TEST_ZK_NODE);
            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(TEST_ZK_NODE));

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#set(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testCreate_Set_Get() {
        System.out.println("testCreate_Set_Get...");
        stopOption();
        try {

            app.create(TEST_ZK_NODE, "0");
            app.set(TEST_ZK_NODE, "1");
            assertEquals("1", app.get(TEST_ZK_NODE));
            
            // try blank
            app.set(TEST_ZK_NODE, "");
            assertEquals("", app.get(TEST_ZK_NODE));
            

        } catch (Exception e) {

            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test method for
     * {@link net.fmpub.zkrw.Zkrw#createOrSet(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateOrSet() {
        System.out.println("testCreateOrSet...");
        stopOption();
        try {

            assertEquals(Zkrw.FALSE_RETURN_VAL, app.exists(TEST_ZK_NODE));
            app.createOrSet(TEST_ZK_NODE, "0");

            assertEquals("0", app.get(TEST_ZK_NODE));

            app.createOrSet(TEST_ZK_NODE, "1");

            assertEquals("1", app.get(TEST_ZK_NODE));
            
            app.createOrSet(TEST_ZK_NODE, "");
            assertEquals("", app.get(TEST_ZK_NODE));
            

        } catch (Exception e) {

            e.printStackTrace();
            // fail();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent)
     */
    @Override
    public void process(WatchedEvent event) {
        // no op

    }

}
