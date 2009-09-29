package orc.trace.handles;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @see Handle
 * @author quark
 */
public class OnlyHandle<E> extends Handle<E> {
	public OnlyHandle() {
		super();
	}
	public OnlyHandle(E value) {
		super(value);
	}
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		value = (E)in.readObject();
	}
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeObject(value);
	}
}
