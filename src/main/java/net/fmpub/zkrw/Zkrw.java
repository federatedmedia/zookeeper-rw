package net.fmpub.zkrw;

import java.io.*;
import java.util.*;

import net.fmpub.zk.util.queue.*;

import org.apache.log4j.*;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

/**
 * Zkrw simple CRUD for ZooKeeper
 * for use with command line apps.
 * Note: This application adheres to the convention for shell success or failure, e.g. $? == 0 for success.
 */
public class Zkrw implements Watcher
{
    private static Logger logger = Logger.getLogger(Zkrw.class);
    private final ZooKeeper zk;
    
    
    public static final String TRUE_RETURN_VAL = "true";
    public static final String FALSE_RETURN_VAL = "false";
    
    public static final String DEFAULT_LIST_DELIMITER = "\t";
    
    public static final String NEWLINE = System.getProperty("line.separator");
    
    public static final char ZK_PATH_DELIMITER = '/';
    
    public static final int DEFAULT_MAX_ATTEMPTS = 8;
    
    public static final int DEFAULT_RETRY_WAIT_MIN_MS = 2;
    public static final int DEFAULT_RETRY_WAIT_MAX_MS = 1000;
    
    public static final int DEFAULT_SESSION_MS = 3000;
    
    
    /**
     * program initialization.
     * 
     * @param args command line args
     */
    public static void main(String[] args)
    {

        
            
        // parse and verify parameters
        
        if (args.length < 5)
        {
            printUsageAndExit();
        }
        
        String hosts = null;
        int sessionTimeOutMs = DEFAULT_SESSION_MS;
        int maxNumRetries = DEFAULT_MAX_ATTEMPTS;
        int retryDelayMinInMS = DEFAULT_RETRY_WAIT_MIN_MS;
        int retryDelayRangeInMS = DEFAULT_RETRY_WAIT_MAX_MS - DEFAULT_RETRY_WAIT_MIN_MS;
        String command = null;
        String path = null;
        String dataOrSec = null;
        String priority = null;
        try
        {
            hosts = parseArg(args, 0, "hosts", true, false);
            
            String sessionTimeOutStr = parseArg(args, 1, "sessionTimeOut", false, true);
            sessionTimeOutMs = (sessionTimeOutStr == null || sessionTimeOutStr == "") ? DEFAULT_SESSION_MS : Integer.valueOf(sessionTimeOutStr);
            
            String maxRetriesStr = parseArg(args, 2, "maxNumRetries", false, true);
            maxNumRetries = (maxRetriesStr == null || maxRetriesStr == "") ? DEFAULT_MAX_ATTEMPTS : Integer.valueOf(maxRetriesStr);
            
            String retryDelayMinStr = parseArg(args, 3, "retryDelayMinStr", false, true);
            retryDelayMinInMS = (retryDelayMinStr == null || retryDelayMinStr == "") ? DEFAULT_RETRY_WAIT_MIN_MS : Integer.valueOf(retryDelayMinStr);
            
            String retryDelayMaxStr = parseArg(args, 4, "retryDelayMaxStr", false, true);
            int retryDelayMaxInMS = (retryDelayMaxStr == null || retryDelayMaxStr == "") ? DEFAULT_RETRY_WAIT_MAX_MS : Integer.valueOf(retryDelayMaxStr);
            retryDelayRangeInMS = retryDelayMaxInMS - retryDelayMinInMS;
            
            command = parseArg(args, 5, "command", true, false);
            
            path = parseArg(args, 6, "path", true, false);
            dataOrSec = parseArg(args, 7, "dataOrSeconds", false, true);
            priority = parseArg(args, 8, "priority", false, true);
        }
        catch(Exception e) {
            // any problem here means it will never succeed
            logger.fatal(e.getMessage());
            printUsageAndExit();
        }
        
        
        int exitVal = run(hosts, sessionTimeOutMs, maxNumRetries, retryDelayMinInMS, retryDelayRangeInMS, command, path, dataOrSec, priority);
        
        if(exitVal != 0) {
            System.exit(exitVal);
        }
    }

    /**
     * 
     * @param rand
     * @param hosts
     * @param sessionTimeOutMs
     * @param maxNumRetries
     * @param retryDelayMinInMS
     * @param retryDelayRangeInMS
     * @param command
     * @param path
     * @param dataOrSec
     * @param priority
     */
    public static int run(String hosts, int sessionTimeOutMs,
            int maxNumRetries, int retryDelayMinInMS, int retryDelayRangeInMS, String command,
            String path, String dataOrSec, String priority) {
        
        Random rand = new Random();
        
        Zkrw rw = null;
        
        
        if(logger.isInfoEnabled()) {
            logger.info("Processing " + commandsToString(hosts,sessionTimeOutMs,maxNumRetries,retryDelayMinInMS,retryDelayRangeInMS,command,path,dataOrSec,priority));
        }
        
        List<Exception> exceptionList = new ArrayList<Exception>();
        
        boolean success = false;
        for(int attNum=0; !success && attNum < maxNumRetries; attNum++) {
            
            
            try
            {
                rw = new Zkrw(hosts,sessionTimeOutMs);
                
                // TODO: Strings will be supported in a switch statement in Java 7, due in July 2011
                
                if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("-h")) {
                	printUsage();
                	return 1;
                }
                else if (command.equalsIgnoreCase("create"))
                {
                    rw.create(path, dataOrSec);
                }
                else if (command.equalsIgnoreCase("createIfNotExists"))
                {
                    rw.createIfNotExists(path, dataOrSec);
                }
                else if (command.equalsIgnoreCase("createOrSet"))
                {
                    rw.createOrSet(path, dataOrSec);
                }
                else if (command.equalsIgnoreCase("createIfNotExistsWithParents"))
                {
                    rw.createIfNotExistsWithParents(path, dataOrSec);
                }
                else if (command.equalsIgnoreCase("createOrSetWithParents"))
                {
                    rw.createOrSetWithParents(path, dataOrSec);
                }
                else if (command.equalsIgnoreCase("delete"))
                {
                    rw.delete(path);
                }
                else if (command.equalsIgnoreCase("deleteAll"))
                {
                    rw.deleteAll(path);
                }
                else if (command.equalsIgnoreCase("exists")) 
                {
                	System.out.println(rw.exists(path));
                }
                else if (command.equalsIgnoreCase("get"))
                {
                    System.out.println(rw.get(path));
                }
                else if (command.equalsIgnoreCase("getAll")) 
                {
                	System.out.println(rw.getAll(path));
                }
                else if (command.equalsIgnoreCase("getChildren")) 
                {
                	System.out.println(rw.getChildren(path));
                }
                else if (command.equalsIgnoreCase("getChildrenOnly")) 
                {
                    System.out.println(rw.getChildrenOnly(path));
                }
                else if (command.equalsIgnoreCase("getCTime")) 
                {
                    System.out.println(rw.getCTime(path));
                }
                else if (command.equalsIgnoreCase("getMTime")) 
                {
                    System.out.println(rw.getMTime(path));
                }
                else if (command.equalsIgnoreCase("getNumChildren")) 
                {
                    System.out.println(rw.getNumChildren(path));
                }
                else if (command.equalsIgnoreCase("qAdd")) 
                {
                	int priorityNum = ( priority == null || priority.length() == 0 ? SimplePriorityKeyHandler.HIGHEST_PRIORITY : Integer.valueOf(priority) );
                	System.out.println(rw.queueAdd(path,dataOrSec,priorityNum));
                }
                else if (command.equalsIgnoreCase("qPoll")) 
                {
                	System.out.println(dataOrSec == null || dataOrSec.length() == 0 ? rw.queuePoll(path) : rw.queuePoll(path, Integer.valueOf(dataOrSec)));
                }
                else if (command.equalsIgnoreCase("set"))
                {
                    rw.set(path, dataOrSec);
                }
                else {
                    String msg = "could not find command named: '" + command + "' for " + commandsToString(hosts, sessionTimeOutMs,maxNumRetries, retryDelayMinInMS, retryDelayRangeInMS, command, path, dataOrSec, priority);    
                	logger.fatal(msg);
                	System.err.println(msg);
                	printUsage();
                	return 1;
                }
                success = true;
                
            }
            catch (IOException ex)
            {
                exceptionList.add(new RuntimeException("could not initialize zookeeper",ex));
                
            }
            catch (Exception ex)
            {
                exceptionList.add(ex);
                
            }
            finally {
            	try {
    				if(rw != null) {
    				    rw.close();
    				}
    				
    			} catch (InterruptedException e) {
    			    exceptionList.add(new RuntimeException("could not close zookeeper",e));
    			}
    			

            }
            
            if(!success) {
                
                
                // pause before next attempt
                try {
                    int delay = retryDelayMinInMS;
                    if(retryDelayRangeInMS > 0) {
                        delay += rand.nextInt(retryDelayRangeInMS);
                    }
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    // do nothing
                }
                
            }
            
            
        } // end for loop
        
        if(!success) {
            String msg = "Quit after " + maxNumRetries + " failures. " + commandsToString(hosts,sessionTimeOutMs,maxNumRetries,retryDelayMinInMS,retryDelayRangeInMS,command,path,dataOrSec,priority);
            logger.fatal(msg);
            logExceptions(logger,exceptionList);
            System.err.println(msg);
            
        }
        else if(exceptionList.size() > 0) {
            logger.info("Succeeded after " + exceptionList.size() + " exceptions.");
            logExceptions(logger,exceptionList);
        }
        
        return (success ? 0 : 1);
    }
    
    /*
     *         String hosts = null;
        int maxNumRetries = 0;
        int retryDelayIncInMS = 0;
        String command = null;
        String path = null;
        String dataOrSec = null;
        String priority = null;
     */
    private static String commandsToString(String hosts, int sessionTimeOutMs, int maxNumRetries, int retryDelayMinInMS, int retryDelayRangeInMS,
            String command, String path, String dataOrSec, String priority) {
        return String.format("Command: (hosts='%s', session timeout ms=%d, max retries=%d, retry min delay in ms=%d, retry max delay in ms=%d) %s %s %s %s",hosts,sessionTimeOutMs,maxNumRetries,retryDelayMinInMS,(retryDelayMinInMS+retryDelayRangeInMS),command,path,dataOrSec,priority);
    }

    private static void logExceptions(Logger logger, List<Exception> exceptionList) {
        int exNum = 1;
        for(Exception ex : exceptionList) {
            logger.info("Exception #"+exNum, ex);
            exNum++;
        }
    }
    
	/**
	 * 
	 */
	private static void printUsageAndExit() {
	    printUsage();
	    System.exit(1);
	}
	
	/**
     * 
     */
    private static void printUsage() {
        logger.info("Malformed command. Printing usage and exiting.");
        System.out.println("Usage: java -jar Zkrw.jar <host> create|createIfNotExists|createIfNotExistsWithParents|createOrSet|createOrSetWithParents|delete|deleteAll|exists|get|getAll|getChildren|getChildrenOnly|getCTime|getMTime|getNumChildren|qAdd|qPoll|set <path> [data or wait in sec] [priority]");
        
    }

    /**
     * Initialize and connect to ZooKeeper
     * 
     * @param zkHosts ZooKeeper hosts string (server1:port,server2:port)
     * @throws IOException
     */
    public Zkrw(String zkHosts) throws IOException
    {
        zk = new ZooKeeper(zkHosts, DEFAULT_SESSION_MS, this);
        
    }
    
    /**
     * Initialize and connect to ZooKeeper
     * 
     * @param zkHosts ZooKeeper hosts string (server1:port,server2:port)
     * @throws IOException
     */
    public Zkrw(String zkHosts, int sessionTimeoutMs) throws IOException
    {
        zk = new ZooKeeper(zkHosts, sessionTimeoutMs, this);
        
    }

    /**
     * There is nothing to do during the callback because we do everything with
     * zk returns
     * @param we unused
     */
    @Override
    public void process(WatchedEvent we)
    {
        // do nothing
    }
    
    public void syncPath(String path) {
    	VoidCallback callback = new VoidCallback() {
			
			@Override
			public void processResult(int rc, String path, Object ctx) {
				System.out.println("sync callback: " + rc + ", path = " + path);
				
			}
		};
    	zk.sync(path, callback, null);
    }
    
    /**
     * get the string representation of the data for a given path
     * 
     * @param path ZooKeeper key path
     * @return value of the key
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String get(String path) throws KeeperException, InterruptedException
    {
        return new String(zk.getData(path, false, null));
    }
    
    /**
     * Gets the value for the path and all its child paths, recursively.
     * @param path
     * @return a String with {path} {value}{newline}, with the default delimiter.
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String getAll(String path) throws KeeperException, InterruptedException
    {
    	
    	return getChildrenRec(path, new StringBuffer()).toString();
    }
    
    /**
     * Gets the value for the path and all its immediate child paths.specsworksm4
     * 
     * @param path
     * @return a String with {path} {value}{newline}, with the default delimiter.
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String getChildren(String path) throws KeeperException, InterruptedException
    {
    	StringBuffer sbuff = new StringBuffer();
    	addGetLine(path,sbuff);
        for(String childName : zk.getChildren(path, false)) {
            String childPath = path + "/" + childName; 
            
            addGetLine(childPath,sbuff);
            
        }

    	return sbuff.toString();
    }
    
    /**
     * Gets the value for the path's immediate child paths only.
     * 
     * @param path
     * @return a String with {path} {value}{newline}, with the default delimiter.
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String getChildrenOnly(String path) throws KeeperException, InterruptedException
    {
        StringBuffer sbuff = new StringBuffer();
        
        for(String childName : zk.getChildren(path, false)) {
            String childPath = path + "/" + childName; 
            
            addGetLine(childPath,sbuff);
            
        }

        return sbuff.toString();
    }
    
    private StringBuffer getChildrenRec(String parentPath, StringBuffer sbuff) throws KeeperException, InterruptedException {
    	//System.out.println("getChildrenData for parentPath: " + parentPath);
    	addGetLine(parentPath,sbuff);
    	for(String childName : zk.getChildren(parentPath, false)) {
    		String childPath = parentPath + "/" + childName; 
    		getChildrenRec(childPath, sbuff);
    		
    	}
    	return sbuff;
    }
    
    private StringBuffer addGetLine(String path, StringBuffer sbuff) throws KeeperException, InterruptedException {
    	sbuff.append(path).append(DEFAULT_LIST_DELIMITER).append(new String(zk.getData(path, false, null))).append(NEWLINE);
    	return sbuff;
    }
    
    /**
     * set value of given path to data
     * 
     * @param path ZooKeeper key path
     * @param data ZooKeeper data to store in key
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void set(String path, String data) throws KeeperException, InterruptedException
    {
        zk.setData(path, data.getBytes(), -1);
    }
    
    /**
     * create a key and set the initial value
     * 
     * @param path ZooKeeper key path
     * @param data ZooKeeper data to store in key
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void create(String path, String data) throws KeeperException, InterruptedException
    {
    	if(data == null) {
    		throw new IllegalArgumentException("data cannot be null");
    	}
    	if(path == null) {
    		throw new IllegalArgumentException("path cannot be null");
    	}
        zk.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
    
    /**
     * delete the key specified by path
     * 
     * @param path ZooKeeper key path
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void delete(String path) throws KeeperException, InterruptedException
    {
        try {
            zk.delete(path, -1);
        }
        catch(KeeperException ke) {
            // do nothing if the node does not exist
            
            if(ke.code().equals(KeeperException.Code.NONODE)) {
                logger.info("Attempted delete but node does not exist: " + path);
            }
            else {
                
                throw ke;
            }
        }
        
    }
    
    /**
     * delete the key specified by path, and all children.
     * 
     * @param path ZooKeeper key path
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void deleteAll(String path) throws KeeperException, InterruptedException
    {
        
        deleteRec(path);
           
    }
    
    private void deleteRec(String parentPath) throws KeeperException, InterruptedException
    {
    	for(String childName : zk.getChildren(parentPath, false)) {
    		String childPath = parentPath + "/" + childName; 
    		
    		deleteRec(childPath);
    		
    	}
    	
    	// delete this node
    	try {
    	    zk.delete(parentPath, -1);
    	
    	}
        catch(KeeperException ke) {
            // do nothing if the node does not exist
            
            if(ke.code().equals(KeeperException.Code.NONODE)) {
                logger.debug("Attempted deleteRec but node does not exist: " + parentPath);
            }
            else {
                
                throw ke;
            }
        }
    }
    
    /**
     * Returns <code>TRUE_RETURN_VAL</code> if the path exists, or 
     * <code>FALSE_RETURN_VAL</code> if not.
     * @param path Zookeeper path
     * @return true/false
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String exists(String path) throws KeeperException, InterruptedException
    {
        return zk.exists(path, false) == null ? FALSE_RETURN_VAL : TRUE_RETURN_VAL;
    }
    
    /**
     * If the key exists, set the value, otherwise create & set the value.
     * @param path ZooKeeper key path
     * @param data ZooKeeper data to store in key
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void createOrSet(String path, String data) throws KeeperException, InterruptedException
    {
    	createOrSetEnd(path, data);
    }
    
    /**
     * Operates only on the last key in the path. Creates the key if it doesn't exist, 
     * otherwise just sets it.
     * @param path
     * @param data
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void createOrSetEnd(String path, String data) throws KeeperException, InterruptedException {
    	if(zk.exists(path, false) == null) {
        	create(path,data);
        }
        else {
        	set(path,data);
        }
    }
    
    /**
     * If the node (key) exists, set the value, otherwise create & set the value. 
     * If the sub-path (parent) does not exists, it is created first and set with the same value.
     * @param path ZooKeeper key path
     * @param data ZooKeeper data to store in key
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void createOrSetWithParents(String path, String data) throws KeeperException, InterruptedException
    {
        
        for(String subPath : getAllSubPaths(path)) {
        	//System.out.println("createOrSet for " + subPath);
        	createOrSetEnd(subPath, data);
        	
        }
    }
    
    /**
     * If the key does not exist, create it and set the value, otherwise do nothing.
     * @param path ZooKeeper key path
     * @param data ZooKeeper data to store in key
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void createIfNotExists(String path, String data) throws KeeperException, InterruptedException
    {
        createIfNotExistsEnd(path,data);
        
    }

	/**
	 * If the key does not exist, create it and set the value, otherwise do
	 * nothing.
	 * 
	 * @param path
	 *            ZooKeeper key path
	 * @param data
	 *            ZooKeeper data to store in key
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void createIfNotExistsWithParents(String path, String data)
			throws KeeperException, InterruptedException {
		
		for (String subPath : getAllSubPaths(path)) {
			//System.out.println("createIfNotExists for " + subPath);

			createIfNotExistsEnd(subPath, data);
			
		}
	}
	
	/**
	 * Get all subpaths (and the full path)
	 * @param fullPath the path to parse
	 * @return a list of subpaths and the full path in order of smallest to longest (sub)path.
	 */
	private List<String> getAllSubPaths(String fullPath) {
		
		List<String> resultList = new ArrayList<String>();
		
		if(fullPath == null || fullPath.isEmpty() ) {
			throw new IllegalArgumentException("Invalid path (empty)");
		}
		if(fullPath.charAt(0) != ZK_PATH_DELIMITER) {
			throw new IllegalArgumentException("Invalid path. Must start with '" + ZK_PATH_DELIMITER + "'. Path = "+fullPath);
		}
		//System.out.println("in getSubPaths: path="+fullPath);
		for(int delimIndex = 0; delimIndex > -1; ) {
			delimIndex = fullPath.indexOf(ZK_PATH_DELIMITER, delimIndex+1);
			
			if( delimIndex > -1) {
				//System.out.println("d="+delimIndex+", substr="+fullPath.substring(0, delimIndex));
				resultList.add(fullPath.substring(0, delimIndex));
			}
		}
		resultList.add(fullPath);
		return resultList;
	}
    
    /**
     * Operates only on the last key in the path. If the key does not exist, create it and set the value, otherwise do nothing.
     * @param path ZooKeeper key path
     * @param data ZooKeeper data to store in key
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void createIfNotExistsEnd(String path, String data) throws KeeperException, InterruptedException
    {
    	
        if(zk.exists(path, false) == null) {
        	create(path,data);
        }
    }
    
    
    public String queuePoll(String path) throws KeeperException, InterruptedException {
    	
    	return queuePoll(path,0);
    }
    
    public String queuePoll(String path, int numSeconds) throws KeeperException, InterruptedException {
    	PriorityDistributedQueue<String, Integer> queue = initQueue(path);
    	byte[] value = queue.poll(numSeconds);
    	
    	if(value == null) {
    		throw new RuntimeException("Queue still empty after waiting " + numSeconds + " sec");
    	}
    	return new String(value);
    	
    }
    
    public String queueAdd(String path, String data, int priority) throws KeeperException, InterruptedException {
    	PriorityDistributedQueue<String, Integer> queue = initQueue(path);
    	
    	return queue.offer(data.getBytes(), priority) ? TRUE_RETURN_VAL : FALSE_RETURN_VAL;
    	
    }
    
    /**
     * Get the creation time of a znode
     * @param path the path to the znode
     * @return creation time in unixtime
     * @throws InterruptedException
     * @throws KeeperException
     */
    public String getCTime(String path) throws InterruptedException, KeeperException {
        
        return String.valueOf(getStat(path).getCtime());
        
    }
    
    /**
     * Get the modified time of a znode
     * @param path the path to the znode
     * @return modified time in unixtime
     * @throws InterruptedException
     * @throws KeeperException
     */
    public String getMTime(String path) throws InterruptedException, KeeperException {
        
        return String.valueOf(getStat(path).getMtime());
        
    }
    
    /**
     * Get the number of children of a znode
     * @param path the path to the znode
     * @return the number of children
     * @throws InterruptedException
     * @throws KeeperException
     */
    public String getNumChildren(String path) throws InterruptedException, KeeperException {
        
        return String.valueOf(getStat(path).getNumChildren());
        
    }
    
    /**
     * Returns the Stat object for this znode
     * @param path the path to the znode
     * @return Stat object
     * @throws InterruptedException
     * @throws KeeperException
     */
    private Stat getStat(String path) throws InterruptedException, KeeperException {
        Stat stat = new Stat();
        zk.getData(path, false, stat);
            
        return stat;
        
    }
    
    private PriorityDistributedQueue<String, Integer> initQueue(String path) {
    	return new PriorityDistributedQueue<String, Integer>(zk, path, new SimplePriorityKeyHandler());
    }
    
    /**
     * close connection to the ZooKeeper server
     * 
     * @throws InterruptedException
     */
    public void close() throws InterruptedException
    {
        if(zk != null) {
        	zk.close();
        }
    }
    
    private static String parseArg(String[] args, int index, String argName, boolean isRequired, boolean allowEmpty) throws IllegalArgumentException, RuntimeException {
    	
    	if(args.length <= index || args[index] == null) {
    		if(isRequired) {
    			throw new IllegalArgumentException("Argument '"+argName+"' is required.");
    		}
    		else {
    			return null;
    		}
    	}
    	String fieldTrimmed = args[index].trim();
    	String out = fieldTrimmed;
    	
    	if (fieldTrimmed.startsWith("\"")) {
            // it must also end with enclosing string
            if (!fieldTrimmed.endsWith("\"")) {
                throw new RuntimeException(String.format(
                        "The argument '%s' is started by '%s' but not at the end.", args[index],
                        "\""));
            }
            
            out = fieldTrimmed.substring(1, fieldTrimmed.length() - 1);
    	}
    	
    	if(out.length() == 0 && !allowEmpty) {
    	    
    		if(isRequired) {
    			throw new IllegalArgumentException("Argument '"+argName+"' is required.");
    		}
    		else {
    			return null;
    		}
	    
    	}
    	
    	return out;
    }
}
