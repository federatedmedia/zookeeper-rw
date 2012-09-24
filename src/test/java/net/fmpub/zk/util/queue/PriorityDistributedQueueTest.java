/**
 * Project: Zkrw
 * File: PriorityDistributedQueueTest.java
 * Created on: Jul 8, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 */
package net.fmpub.zk.util.queue;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import net.fmpub.zkrw.Zkrw;

import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.*;
import org.junit.*;


/**
 * @author brogosky
 *
 */
public class PriorityDistributedQueueTest implements Watcher {

    static {
        PropertyConfigurator.configure("src/main/resources/log4j.test.properties");
    }
    
	public static final String ZK_SERVERS = "127.0.0.1:2181";
	
	SimplePriorityKeyHandler keyHandler;
	PriorityDistributedQueue<String, Integer> queueStrInt;
	
	Integer priorityNum = 1;
	
	// assumes the priority part is length 3
	String childNamePrefix = SimplePriorityKeyHandler.PREFIX + "00" + priorityNum + SimplePriorityKeyHandler.CHILD_NAME_DELIM;
	String childName = childNamePrefix + "0000001";
	
	public static final String TEST_ZK_ROOT_NODE = "/DS-test"; // DO NOT DELETE
	public static final String TEST_ZK_Q_NODE = TEST_ZK_ROOT_NODE + "/test-queue"; // DELETE EACH TIME
	
	ZooKeeper zk;
	Zkrw zkApp;
	
	@Before
	public void init()  {
		
	    
	    
		try {
			zk = new ZooKeeper(ZK_SERVERS, 3000, this);
			
			// delete queue node and all children
			zkApp = new Zkrw(ZK_SERVERS);
			if(zkApp.exists(TEST_ZK_Q_NODE).equals(Zkrw.TRUE_RETURN_VAL)) {
				System.out.println("Deleting queue node: " + TEST_ZK_Q_NODE);
				zkApp.deleteAll(TEST_ZK_Q_NODE);
			}
			zkApp.create(TEST_ZK_Q_NODE, "time "+System.currentTimeMillis());
			
			keyHandler = new SimplePriorityKeyHandler();
			queueStrInt = new PriorityDistributedQueue<String, Integer>(zk,TEST_ZK_Q_NODE,keyHandler);
			
		} catch (KeeperException ke) {
			handleKeeperException(ke);
		}
		catch (Exception e) {
			
			e.printStackTrace();
			fail("failed in init()");
		} 
		
	}
	
	@After
	public void tearDown() {
		try {
			zk.close();
			zkApp.close();
		} catch (NullPointerException npe) {
			System.out.println("Ignoring NullPointerException: " + npe.getMessage());
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void handleKeeperException(KeeperException e) {
		if(e.code() == KeeperException.Code.CONNECTIONLOSS) {
			System.err.println("Proceeding after connection loss...\n");
		}
		else {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testPoll() {
		try {
			assertNull(null, queueStrInt.poll());
		} catch (KeeperException e) {
			
			handleKeeperException(e);
			
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testPollInt() {
		try {
			assertNull(queueStrInt.poll());
			
			String val = "offer-value-5";
			
			Runnable runOffer = new OfferRunnable<String, Integer>(queueStrInt,val,1,1);
			
			// throws exception if item does not exist in the queue.
			System.out.println("Starting poll(int) attempt...");
			
			Thread offerThread = new Thread(runOffer);
			offerThread.run();
			
			assertEquals(val,new String( queueStrInt.poll(3000)));
			
			System.out.println("zk output: " + zkApp.getChildren(TEST_ZK_Q_NODE));
			
			// poll() removes the item...
			assertNull(queueStrInt.poll());
			
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testPeek() {
		try {
			assertNull(null, queueStrInt.peek());
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testOffer() {
		try {
			assertTrue(queueStrInt.offer("offer-value-1".getBytes(),1));
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}
	
	
	@Test
	public void testOfferPeekPoll() {
		try {
			String val = "offer-value-2";
			assertTrue(queueStrInt.offer(val.getBytes(),1));
			
			assertEquals(val, new String(queueStrInt.peek()));
			
			assertEquals(val, new String(queueStrInt.poll()));
			
			assertNull(null, queueStrInt.peek());
			assertNull(null, queueStrInt.poll());
			
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testOfferAndTake() {
		try {
			final String val = "offer-value-3";
			
			Runnable runOffer = new OfferRunnable<String, Integer>(queueStrInt,val,1,3);
			
			assertNull(null, queueStrInt.peek());
			
			(new Thread(runOffer)).run();
			
			// blocks until an item exists in the queue.
			System.out.println("Starting take() attempt...");
			assertEquals(val, new String(queueStrInt.take()));
			
			assertNull(null, queueStrInt.peek());
			
			
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}
	
	
	@Test
	public void testOfferAndRemove() {
		try {
			final String val = "offer-value-4";
			
			Runnable runOffer = new OfferRunnable<String, Integer>(queueStrInt,val,1,0);
			
			// throws exception if item does not exist in the queue.
			System.out.println("Starting remove() attempt...");
			
			boolean noSuchEl = false;
			try {
				queueStrInt.remove();
			} catch(NoSuchElementException e) {
				noSuchEl = true;
			}
			assertTrue(noSuchEl);
			
			Thread offerThread = new Thread(runOffer);
			offerThread.run();
			
			
			Thread.sleep(2000);
			assertEquals(val,new String(queueStrInt.remove()));
			
			assertNull(null, queueStrInt.peek());
			
			
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void testOfferAndElement() {
		try {
			String val = "offer-value-5";
			
			Runnable runOffer = new OfferRunnable<String, Integer>(queueStrInt,val,1,0);
			
			// throws exception if item does not exist in the queue.
			System.out.println("Starting element() attempt...");
			
			boolean noSuchEl = false;
			try {
				queueStrInt.element();
			} catch(NoSuchElementException e) {
				noSuchEl = true;
			}
			assertTrue(noSuchEl);
			
			Thread offerThread = new Thread(runOffer);
			offerThread.run();
			
			
			Thread.sleep(2000);
			assertEquals(val,new String(queueStrInt.element()));
			
			System.out.println("zk output: " + zkApp.getChildren(TEST_ZK_Q_NODE));
			// element() does not remove the item...
			assertEquals(val, new String(queueStrInt.peek()));
			
			
		} catch (KeeperException e) {
			
			handleKeeperException(e);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
			fail();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent)
	 */
	@Override
	public void process(WatchedEvent event) {
		System.out.println("Received event: " + event);
	}
	
	class OfferRunnable<RKEY, RPRIORITY extends Comparable<RPRIORITY>> implements Runnable {
		
		private String offerVal = "Default val";
		private PriorityDistributedQueue<RKEY, RPRIORITY> queue;
		private RPRIORITY usePriority;
		private int waitSeconds;
		
		public OfferRunnable(PriorityDistributedQueue<RKEY, RPRIORITY> queue, String val, RPRIORITY priority, int waitSeconds) {
			this.queue = queue;
			if(val != null) {
				offerVal = val;
			}
			usePriority = priority;
			this.waitSeconds = waitSeconds;
		}
		
		@Override
		public void run() {
			try {
				for(int w=0; w < waitSeconds; w++) {
					System.out.println("Waiting step " + w);
					Thread.sleep(1000);
				}
				
				assertTrue(this.queue.offer(offerVal.getBytes(),usePriority));
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeeperException e) {
				
				handleKeeperException(e);
			}					
		}
	};
}
