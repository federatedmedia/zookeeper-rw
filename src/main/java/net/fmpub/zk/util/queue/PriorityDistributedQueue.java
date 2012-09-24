/**
 * Project: Zkrw
 * File: PriorityDistributedQueue.java
 * Created on: Jun 24, 2011
 * By: brogosky
 *  2011 Federated Media, Inc. 
 *
 * Derived from class org.apache.zookeeper.recipes.queue.DistributedQueue
 * provided in the recipes folder of zookeeper3.3.3.
 * 
 * The original work is licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fmpub.zk.util.queue;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import net.fmpub.zk.util.ZkUtils;

import org.apache.log4j.Logger;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

/**
 * @author brogosky
 * 
 */
public class PriorityDistributedQueue<KEY, PRIORITY extends Comparable<PRIORITY>> {
	private static final Logger LOGGER = Logger
			.getLogger(PriorityDistributedQueue.class);

	private final String dir;

	private ZooKeeper zookeeper;
	private List<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

	// NEW
	private PriorityKeyHandler<KEY, PRIORITY> keyHandler;

	public PriorityDistributedQueue(ZooKeeper zookeeper, String dir,
			PriorityKeyHandler<KEY, PRIORITY> keyHandler) {

		this(zookeeper, dir, keyHandler, null);
	}

	public PriorityDistributedQueue(ZooKeeper zookeeper, String dir,
			PriorityKeyHandler<KEY, PRIORITY> keyHandler, List<ACL> acl) {
		if (dir == null || dir.trim().length() == 0) {
			throw new IllegalArgumentException(
					"Illegal constructor argument: dir cannot be null or empty.");
		}

		this.dir = dir;

		if (acl != null) {
			this.acl = acl;
		}
		this.zookeeper = zookeeper;

		if (keyHandler == null) {
			throw new IllegalArgumentException(
					"Illegal constructor argumentL: keyHandler cannot be null.");
		}

		this.keyHandler = keyHandler;

	}

	/**
	 * Returns a Map of the children, ordered by id.
	 * 
	 * @param watcher
	 *            optional watcher on getChildren() operation.
	 * @return map from id to child name for all children
	 */
	private TreeMap<KEY, String> orderedChildren(Watcher watcher)
			throws KeeperException, InterruptedException {
		TreeMap<KEY, String> orderedChildren = new TreeMap<KEY, String>();

		List<String> childNames = null;
		try {
			childNames = zookeeper.getChildren(dir, watcher);
		} catch (KeeperException.NoNodeException e) {
			throw e;
		}

		for (String childName : childNames) {
			try {

				/*
				 * CHANGED FROM... if(!childName.regionMatches(0, prefix, 0,
				 * prefix.length())){
				 * LOGGER.warn("Found child node with improper name: " +
				 * childName); continue; } String suffix =
				 * childName.substring(prefix.length()); Long childId = new
				 * Long(suffix);
				 */

				// Check format
				if (!keyHandler.hasValidName(childName)) {
					LOGGER.warn("Found child node with improper name: "
							+ childName);
					continue;
				}

				KEY childId = keyHandler.generateKey(childName);
				orderedChildren.put(childId, childName);
			} catch (NumberFormatException e) {
				LOGGER.warn("Found child node with improper format : "
						+ childName + " " + e, e);
			}
		}

		return orderedChildren;
	}

	/**
	 * Return the head of the queue without modifying the queue.
	 * 
	 * @return the data at the head of the queue.
	 * @throws NoSuchElementException
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] element() throws NoSuchElementException, KeeperException,
			InterruptedException {

		return getNext(false, false);
	}

	/**
	 * Attempts to remove the head of the queue and return it.
	 * 
	 * @return The former head of the queue
	 * @throws NoSuchElementException
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] remove() throws NoSuchElementException, KeeperException,
			InterruptedException {
		return getNext(false, true);
	}

	private class PriorityLatchChildWatcher implements Watcher {

		CountDownLatch latchIfNoChange;

		/**
		 * Higher priority is defined by compareTo() < 0. For example, this will
		 * work for Number (lower is higher priority) and Strings (earlier in
		 * alphanumeric order is higher priority).
		 */
		PRIORITY highestPriorityChanged;

		public PriorityLatchChildWatcher() {
			latchIfNoChange = new CountDownLatch(1);

			highestPriorityChanged = null;

		}

		public void process(WatchedEvent event) {
			LOGGER.debug("Watcher fired on path: " + event.getPath()
					+ " state: " + event.getState() + " type "
					+ event.getType());

			latchIfNoChange.countDown();

			// ignore any children except valid children for a queue
			if(keyHandler.hasValidName(ZkUtils.getPathEnd(event.getPath()))) {
				
				if (highestPriorityChanged == null) {
					highestPriorityChanged = keyHandler
							.getPriority(event.getPath());
				} else {
					synchronized (highestPriorityChanged) {
						PRIORITY changedPriority = keyHandler.getPriority(event
								.getPath());
						if (changedPriority != null && changedPriority.compareTo(highestPriorityChanged) < 0) {
							highestPriorityChanged = changedPriority;
						}
					}
				}

			}
			
		}

		public void awaitChange() throws InterruptedException {
			latchIfNoChange.await();
		}

		public boolean hasChanged() {
			return highestPriorityChanged != null;
		}

		public boolean hasChangedHigherThan(PRIORITY priority) {
			return highestPriorityChanged != null
					&& highestPriorityChanged.compareTo(priority) < 0;
		}

		public void clearChange() {

			latchIfNoChange.countDown();

			if (highestPriorityChanged != null) {
				synchronized (highestPriorityChanged) {
					highestPriorityChanged = null;
				}
			}

		}
	}

	/**
	 * Removes the head of the queue and returns it, blocks until it succeeds.
	 * 
	 * @return The former head of the queue
	 * @throws NoSuchElementException
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] take() throws KeeperException, InterruptedException {
		return getNext(true, true);
	}

	/**
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws NoSuchElementException
	 */
	protected byte[] getNext(boolean waitForElement, boolean deleteElement)
			throws KeeperException, InterruptedException,
			NoSuchElementException {
		TreeMap<KEY, String> orderedChildren;

		// element, take, and remove follow the same pattern.
		// We want to return the child node with the smallest sequence number.
		// Since other clients are remove()ing and take()ing nodes concurrently,
		// the child with the smallest sequence number in orderedChildren might
		// be gone by the time we check.
		// We don't call getChildren again until we have tried the rest of the
		// nodes in sequence order,
		// BUT if the children change with a higher priority
		// than the current child we are getting, then we reload all the
		// children.

		while (true) {
			PriorityLatchChildWatcher childWatcher = new PriorityLatchChildWatcher();
			try {
				orderedChildren = orderedChildren(childWatcher);
			} catch (KeeperException.NoNodeException e) {

				if (waitForElement) {
					zookeeper.create(dir, new byte[0], acl,
							CreateMode.PERSISTENT);
					continue;
				} else {
					throw new NoSuchElementException();
				}
			}

			if (orderedChildren.size() == 0) {
				if (waitForElement) {
					childWatcher.awaitChange();
					continue;
				} else {
					throw new NoSuchElementException();
				}
			}

			for (String headNode : orderedChildren.values()) {
				String path = dir + "/" + headNode;
				try {
					byte[] data = zookeeper.getData(path, false, null);

					if (childWatcher.hasChangedHigherThan(keyHandler
							.getPriority(headNode))) {
						break;
					}

					if (deleteElement) {
						zookeeper.delete(path, -1);
					}
					return data;
				} catch (KeeperException.NoNodeException e) {
					// Another client deleted the node first.
				}
			}
		} // while loop end
	}

	/**
	 * Inserts data into queue.
	 * 
	 * @param data
	 * @return true if data was successfully added
	 */
	public boolean offer(byte[] data, PRIORITY priority)
			throws KeeperException, InterruptedException {
		while (true) {
			try {

				// WAS:
				// zookeeper.create(dir+"/"+prefix, data, acl,
				// CreateMode.PERSISTENT_SEQUENTIAL);
				zookeeper.create(
						dir + "/"
								+ keyHandler.generateChildNamePrefix(priority),
						data, acl, CreateMode.PERSISTENT_SEQUENTIAL);
				return true;
			} catch (KeeperException.NoNodeException e) {
				zookeeper.create(dir, new byte[0], acl, CreateMode.PERSISTENT);
			}
		}

	}

	/**
	 * Returns the data at the first element of the queue, or null if the queue
	 * is empty.
	 * 
	 * @return data at the first element of the queue, or null.
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] peek() throws KeeperException, InterruptedException {
		try {
			return element();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	/**
	 * Attempts to remove the head of the queue and return it. Returns null if
	 * the queue is empty.
	 * 
	 * @return Head of the queue or null.
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] poll() throws KeeperException, InterruptedException {
		try {
			return remove();
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	/**
	 * Attempts to remove the head of the queue and return it. If the queue is
	 * empty, checks periodically for an addition, up to numSeconds. Returns null if
	 * the queue is empty after wait.
	 * 
	 * @return Head of the queue or null.
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public byte[] poll(int numSeconds) throws KeeperException, InterruptedException {
		
		if(numSeconds <= 0) {
			return poll();
		}
		
		for(int cycle=0; cycle < numSeconds; cycle++) {
			try {
				return remove();
			} catch (NoSuchElementException e) {
				// do nothing but wait
				Thread.sleep(1000);
			}
		}
		return null;
	}

}
