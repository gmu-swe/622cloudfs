package edu.gmu.swe622.struct;

import java.nio.ByteBuffer;

import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.NodeType;

import com.dropbox.core.DbxEntry;

/**
 * Representation of a file (not directory) stored in our FS.
 * With that said, it CAN represent a directory when get() is called on a directory.
 * If so, the directory flag is set to true.
 */
public class CloudFile extends CloudPath implements Comparable<CloudFile> {

	@Override
	public String toString() {
		return "CloudFile [directory=" + directory + ", name=" + name
				+ ", size=" + size + ", mTime=" + mTime + "]";
	}

	private boolean directory;
	private long size;
	private long mTime;
	private ByteBuffer contents;

	private boolean dirty;

	public boolean isDirty() {
		return dirty;
	}

	public CloudFile(String name) {
		super(null, name, -1);
	}

	public CloudFile(int poolId) {
		super(null, null, poolId);
	}

	public CloudFile(DbxEntry e) {
		super(null, e.name, -1);
		initFrom(e);
	}

	public CloudFile initFrom(CloudFile other) {
		this.directory = other.directory;
		this.name = other.name;
		this.size = other.size;
		this.mTime = other.mTime;
		this.contents = other.contents;
		return this;
	}

	public CloudFile init(boolean isDirectory, String name, long size,
			long mTime) {
		this.directory = isDirectory;
		this.name = name;
		this.size = size;
		this.mTime = mTime;
		this.contents = null;
		if(this.name == null)
			throw new NullPointerException();
		return this;
	}

	public CloudFile initFrom(DbxEntry e) {
		if (e instanceof DbxEntry.File) {
			DbxEntry.File f = (DbxEntry.File) e;
			init(false, f.name, f.numBytes, f.clientMtime.getTime() / 1000);
		} else {
			init(e.isFolder(), e.name, 0, 0);
		}
		if(this.name == null)
			throw new NullPointerException();
		return this;
	}

	public String getName() {
		return name;
	}

	public ByteBuffer getContents() {
		return contents;
	}

	public void setContents(ByteBuffer contents) {
		this.contents = contents;
	}

	public void accept(StatWrapper stat) {
		stat.atime(0);
		stat.mtime(mTime);
		stat.ctime(0);
		if (directory)
			stat.setMode(NodeType.DIRECTORY);
		else
			stat.setMode(NodeType.FILE);
		stat.size(size);
	}

	@Override
	public int compareTo(CloudFile o) {
		return name.compareTo(o.getName());
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public void write() {
		dirty = true;
		mTime = System.currentTimeMillis() / 1000;
	}

	/**
	 * Unfortunately there's a nasty corner of our abstraction: 
	 * If you call get() on a directory, we still have to fill up a CloudFile,
	 * so we use this bit as a placeholder.
	 * @return
	 */
	public boolean isDirectory() {
		return true;
	}
}
