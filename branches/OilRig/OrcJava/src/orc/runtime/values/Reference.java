package orc.runtime.values;

public interface Reference<E> {
	public E read();
	public void write(E value);
}
