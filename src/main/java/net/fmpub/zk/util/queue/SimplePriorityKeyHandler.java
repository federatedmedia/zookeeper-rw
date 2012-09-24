/**
 * Project: Zkrw
 * File: SimplePriorityKeyHandler.java
 * Created on: Jun 24, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 */
package net.fmpub.zk.util.queue;

import java.util.regex.Pattern;

import net.fmpub.zk.util.ZkUtils;

/**
 * @author brogosky
 *
 */
public class SimplePriorityKeyHandler implements
		PriorityKeyHandler<String, Integer> {

	protected static final String CHILD_NAME_DELIM = "-";
	
	public static final String PREFIX = "q"+CHILD_NAME_DELIM;
	
	
	protected static final int CHILD_NAME_PRIORITY_IDX = 1;
	//protected static final int CHILD_NAME_QID_IDX = 2;
	
	protected static final int CHILD_NAME_PRIORITY_LEN = 3;
	
	public static final int MAX_PRIORITY = (int) Math.pow(10.0, CHILD_NAME_PRIORITY_LEN) - 1;
	public static final int MIN_PRIORITY = 0; 
	public static final int HIGHEST_PRIORITY = MIN_PRIORITY;
		
	protected static final Pattern CHILD_PATTERN = Pattern.compile(PREFIX + "([0-9]{"+CHILD_NAME_PRIORITY_LEN+"})"+CHILD_NAME_DELIM+"([0-9]+)", Pattern.CASE_INSENSITIVE);
	
	//protected static final int CHILD_NAME_LEN_MAX = PREFIX.length() + CHILD_NAME_PRIORITY_LEN + CHILD_NAME_DELIM.length() + String.valueOf(Integer.MAX_VALUE).length();
	
	/**
	 * Format a child name by padding the priority with zeros
	 */
	protected static final String CHILD_NAME_FORMAT = "%0"+CHILD_NAME_PRIORITY_LEN+"d"+CHILD_NAME_DELIM;
	
	/* (non-Javadoc)
	 * @see net.fmpub.zk.util.queue.PriorityKeyHandler#generateChildNamePrefix(java.lang.Object)
	 */
	@Override
	public String generateChildNamePrefix(Integer priority) {
		checkArgPriority(priority);
		return PREFIX + String.format(CHILD_NAME_FORMAT, priority);
		
	}
	
	/* (non-Javadoc)
	 * @see net.fmpub.zk.util.queue.PriorityKeyHandler#generateKey(java.lang.String)
	 */
	@Override
	public String generateKey(String childName) {
		checkArgChildName(childName);
		
		return childName;
	}

	/* (non-Javadoc)
	 * @see net.fmpub.zk.util.queue.PriorityKeyHandler#generateKey(java.lang.String, java.lang.String)
	 */
	@Override
	public String generateKey(String childName, byte[] value) {
		return generateKey(childName);
	}

	/* (non-Javadoc)
	 * @see net.fmpub.zk.util.queue.PriorityKeyHandler#hasValidName(java.lang.String)
	 */
	@Override
	public boolean hasValidName(String childName) {
		return CHILD_PATTERN.matcher(childName).matches();
	}

	/* (non-Javadoc)
	 * @see net.fmpub.zk.util.queue.PriorityKeyHandler#getPriority(java.lang.String)
	 */
	@Override
	public Integer getPriority(String childNameOrPath) {
		
		String childName = (ZkUtils.isValidPath(childNameOrPath, true) ? ZkUtils.getPathEnd(childNameOrPath) : childNameOrPath);
		
		if(childName == null || childName.trim().length() == 0 || !hasValidName(childName)) {
			return null;
		}
		
		String[] childNameSplits = childName.split(CHILD_NAME_DELIM);
		if(childNameSplits.length <= CHILD_NAME_PRIORITY_IDX) {
			return null;
		}
		return Integer.valueOf(childNameSplits[CHILD_NAME_PRIORITY_IDX]);
		
	}
	
	

	
	private void checkArgChildName(String childName) {
		if(childName == null || childName.trim().length() == 0) {
			throw new IllegalArgumentException("Illegal argument empty or null for childName.");
		}
		
		if(!hasValidName(childName)) {
			throw new IllegalArgumentException("Invalid childName: '" + childName +"'");
		}
	}

	private void checkArgPriority(Integer priority) {
		if(priority == null) {
			throw new IllegalArgumentException("Null argument for priority.");
		}
		
		if(priority > MAX_PRIORITY) {
			throw new IllegalArgumentException("Priority exceeds maximum of " + MAX_PRIORITY);
		}
		
		if(priority < MIN_PRIORITY) {
			throw new IllegalArgumentException("Priority is less than minimum of " + MIN_PRIORITY);
		}
	}

}
