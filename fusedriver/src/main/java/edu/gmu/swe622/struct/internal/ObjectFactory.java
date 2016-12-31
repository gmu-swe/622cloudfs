package edu.gmu.swe622.struct.internal;

public interface ObjectFactory<T extends Poolable> {
	public T newInst(int pos);
}
