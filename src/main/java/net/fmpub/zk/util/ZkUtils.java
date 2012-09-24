/**
 * Project: Zkrw
 * File: ZkUtils.java
 * Created on: Jul 7, 2011
 * By: brogosky
 * 2011 Federated Media, Inc.
 */
package net.fmpub.zk.util;

import org.apache.zookeeper.common.PathUtils;

/**
 * @author brogosky
 *
 */
public class ZkUtils {

	/** validate the provided znode path string
	 * @param path znode path string
	 * @param isSequential if the path is being created
	 * with a sequential flag
	 * @throws IllegalArgumentException if the path is invalid
	 */
	public static void validatePath(String path, boolean isSequential) 
		throws IllegalArgumentException {
		PathUtils.validatePath(path, isSequential);
	}
	
    /**
     * Validate the provided znode path string
     * @param path znode path string
     * @throws IllegalArgumentException if the path is invalid
     */
    public static void validatePath(String path) throws IllegalArgumentException {
    	PathUtils.validatePath(path);
    }
    
    public static boolean isValidPath(String path, boolean isSequential) {
    	try {
    		validatePath(path, isSequential);
    		return true;
    	}
    	catch(IllegalArgumentException e) {
    		return false;
    	}
    }
    
    public static boolean isValidPath(String path) {
    	try {
    		validatePath(path);
    		return true;
    	}
    	catch(IllegalArgumentException e) {
    		return false;
    	}
    }
    
	public static String getPathEnd(String path) {
		
		if (path == null) {
            return null;
        }
		
		if ("/".equals(path)) {
            return path;
        }
		
		String[] pathComponents = path.split("/");
		
		return pathComponents[pathComponents.length-1];
	}
}
