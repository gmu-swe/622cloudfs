package edu.gmu.swe622.struct;

import java.util.Iterator;
import java.util.LinkedList;

public class CloudDirectory extends CloudPath {

	@Override
	public String toString() {
		return "CloudDirectory [files=" + files + ", isInited=" + isInited
				+ ", name=" + name + "]";
	}

	private LinkedList<CloudPath> files;

	public CloudDirectory(String name, int idx) {
		super(null, name, idx);
		isInited = false;
		files = new LinkedList<CloudPath>();
	}

	public void init(String name, CloudDirectory parent) {
		this.name = name;
		this.parent = parent;
		files = new LinkedList<CloudPath>();
		isInited = false;
	}

	public void init(CloudDirectory from) {
		this.name = from.name;
		this.files = from.files;
	}

	public void add(CloudPath f) {
		files.add(f);
	}

	public LinkedList<CloudPath> listFiles() {
		return files;
	}

	public void remove(CloudPath f) {
		Iterator<CloudPath> iter = files.iterator();
		while(iter.hasNext())
		{
			CloudPath p = iter.next();
			if(p == f)
				iter.remove();
		}
	}
}
