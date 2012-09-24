/**
 * Project: Zkrw
 * File: PriorityKeyHandler.java
 * Created on: Jun 24, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 */
package net.fmpub.zk.util.queue;

/**
 * @author brogosky
 *
 */
public interface PriorityKeyHandler<KEY,PRIORITY> {

	
	/**
	 * Generate a key for sorting from the child node name
	 * @param childName the name of the child node in the queue.
	 * @return a KEY object
	 */
	KEY generateKey(String childName);
	
	/**
	 * Generate a key for sorting from the child node name and/or value
	 * of the child node.
	 * @param childName the name of the child node in the queue.
	 * @param value value of the child node
	 * @return a KEY object
	 */
	KEY generateKey(String childName, byte[] value);
	
	/**
	 * Return true if the child name is a valid name.
	 * @param childName the name of the child node in the queue.
	 * @return true if valid, false otherwise.
	 */
	boolean hasValidName(String childName);
	
	/**
	 * Generate a child name prefix to which the queue index can be
	 * added automatically.
	 * @param priority the priority object.
	 * @return a child name prefix.
	 */
	String generateChildNamePrefix(PRIORITY priority);
	
	/**
	 * Parses the child name and returns the priority.
	 * @param childNameOrPath the name of the child node or the entire path for the child node in the queue.
	 * @return the priority or null if does not exist.
	 */
	PRIORITY getPriority(String childNameOrPath);
	
}
