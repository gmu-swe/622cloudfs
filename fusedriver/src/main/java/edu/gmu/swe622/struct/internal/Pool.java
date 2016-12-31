package edu.gmu.swe622.struct.internal;

import java.util.ArrayList;
import java.util.LinkedList;

public class Pool<T extends Poolable> {
	public static final int DEFAULT_POOL_SIZE = 1000;
	public static final int GROWTH_RATE = 100;

	LinkedList<T> freeMembers;
	ArrayList<T> allMembers;
	ObjectFactory<T> factory;

	public Pool(ObjectFactory<T> factory) {
		freeMembers = new LinkedList<T>();
		allMembers = new ArrayList<T>(DEFAULT_POOL_SIZE);
		this.factory = factory;

		for (int i = 0; i < allMembers.size(); i++) {
			T obj = factory.newInst(i);
			freeMembers.add(obj);
			allMembers.add(obj);
		}
	}

	public T get(int idx) {
		synchronized (allMembers) {
			return allMembers.get(idx);
		}
	}

	public void release(T obj) {
		synchronized (freeMembers) {
			freeMembers.add(obj);
		}
	}

	public T getInst() {
		synchronized (freeMembers) {
			if (freeMembers.size() == 0)
				grow();
			return freeMembers.pop();
		}
	}

	private void grow() {
		synchronized (allMembers) {
			int size = allMembers.size();
			int newSize = size + GROWTH_RATE;
			allMembers.ensureCapacity(newSize);
			for (int i = 0; i < GROWTH_RATE; i++) {
				T obj = factory.newInst(i + size);
				freeMembers.add(obj);
				allMembers.add(obj);
			}
		}
	}

	public int size() {
		synchronized (allMembers) {
			return allMembers.size();
		}
	}
}
