package orc.trace.handles;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @see Handle
 * @author quark
 */
public class RepeatHandle<E> extends Handle<E> {
	public RepeatHandle() {
		super();
	}
	public RepeatHandle(E value) {
		super(value);
	}
	public void readExternal(final ObjectInput _in) throws IOException, ClassNotFoundException {
		final HandleInputStream in = (HandleInputStream)_in;
		int id = in.readInt();
		value = (E)in.getHandled(id);
	}
	public void writeExternal(final ObjectOutput _out) throws IOException {
		final HandleOutputStream out = (HandleOutputStream)_out;
		out.writeInt(out.getHandle(value));
	}
}
