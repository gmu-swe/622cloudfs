package edu.gmu.swe622.struct;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.gmu.swe622.struct.internal.Poolable;

/**
 * Representation of a file or directory that's stored in our FS
 * 
 * Don't mess with poolIdx - it's used internally by the filesystem bridge. Use
 * the other constructors.
 *
 */
public abstract class CloudPath implements Poolable {
	private final int poolIdx;

	/**
	 * Hint: You might want to use the "parent" in your memory DB implementation
	 * :)
	 */
	protected CloudDirectory parent;
	protected String name;
	private Lock lock;

	public CloudPath(CloudDirectory parent, String name) {
		this(parent, name, -1);
	}

	public CloudPath(CloudDirectory parent, String name, int poolIdx) {
		this.parent = parent;
		this.name = name;
		this.poolIdx = poolIdx;
		this.lock = new ReentrantLock();
	}

	public int getPoolIdx() {
		return poolIdx;
	}

	public CloudDirectory getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public void setParent(CloudDirectory parent) {
		this.parent = parent;
	}

	public void markInited() {
		this.isInited = true;
	}

	protected boolean isInited = false;

	public boolean isInited() {
		return isInited;
	}

	public Lock getLock() {
		return lock;
	}

}
