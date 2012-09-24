/**
 * Project: Zkrw
 * File: SimplePriorityKeyHandlerTest.java
 * Created on: Jul 6, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 */
package net.fmpub.zk.util.queue;

import static org.junit.Assert.*;

import org.apache.log4j.PropertyConfigurator;
import org.junit.*;

/**
 * @author brogosky
 *
 */
public class SimplePriorityKeyHandlerTest {

	SimplePriorityKeyHandler keyHandler;
	Integer priorityNum;
	
	// assumes the priority part is length 3
	String childNamePrefix;
	String childName;
	
	@Before
	public void init() {
	    PropertyConfigurator.configure("src/main/resources/log4j.test.properties");
	    
		keyHandler = new SimplePriorityKeyHandler();
		priorityNum = 1;
		
		// assumes the priority part is length 3
		childNamePrefix = SimplePriorityKeyHandler.PREFIX + "00" + priorityNum + SimplePriorityKeyHandler.CHILD_NAME_DELIM;
		childName = childNamePrefix + "0000001";
	}
	
	/**
	 * Test method for {@link net.fmpub.zk.util.queue.SimplePriorityKeyHandler#generateChildNamePrefix(java.lang.Number)}.
	 */
	@Test
	public void testGenerateChildNamePrefix() {
		
		
		assertEquals(childNamePrefix, keyHandler.generateChildNamePrefix(1));
		
	}

	/**
	 * Test method for {@link net.fmpub.zk.util.queue.SimplePriorityKeyHandler#generateKey(java.lang.String)}.
	 */
	@Test
	public void testGenerateKeyString() {
		
		assertEquals(childName,keyHandler.generateKey(childName));
	}

	/**
	 * Test method for {@link net.fmpub.zk.util.queue.SimplePriorityKeyHandler#generateKey(java.lang.String, byte[])}.
	 */
	@Test
	public void testGenerateKeyStringByteArray() {
		
		assertEquals(childName,keyHandler.generateKey(childName,null));
		assertEquals(childName,keyHandler.generateKey(childName,new byte[]{}));
	}

	/**
	 * Test method for {@link net.fmpub.zk.util.queue.SimplePriorityKeyHandler#hasValidName(java.lang.String)}.
	 */
	@Test
	public void testHasValidName() {
		assertTrue(keyHandler.hasValidName(childName));
		
		assertTrue(keyHandler.hasValidName(SimplePriorityKeyHandler.PREFIX + "020" + SimplePriorityKeyHandler.CHILD_NAME_DELIM + "000000002"));
		
		assertTrue(keyHandler.hasValidName(SimplePriorityKeyHandler.PREFIX + "999" + SimplePriorityKeyHandler.CHILD_NAME_DELIM + "000000002"));
		
		assertFalse(keyHandler.hasValidName(SimplePriorityKeyHandler.PREFIX + "-1" + SimplePriorityKeyHandler.CHILD_NAME_DELIM + "000000002"));
		
		assertFalse(keyHandler.hasValidName("020" + SimplePriorityKeyHandler.CHILD_NAME_DELIM + "000000002"));
		assertFalse(keyHandler.hasValidName(SimplePriorityKeyHandler.PREFIX + "999000000002"));
		
	}

	/**
	 * Test method for {@link net.fmpub.zk.util.queue.SimplePriorityKeyHandler#getPriority(java.lang.String)}.
	 */
	@Test
	public void testGetPriority() {
		assertEquals(priorityNum,keyHandler.getPriority(childName));
		
		assertNull(keyHandler.getPriority(SimplePriorityKeyHandler.PREFIX + "-1" + SimplePriorityKeyHandler.CHILD_NAME_DELIM + "000000002"));
	}

}
