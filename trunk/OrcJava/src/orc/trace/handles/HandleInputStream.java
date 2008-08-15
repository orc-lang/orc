package orc.trace.handles;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Used in conjunction with {@link Handle} to explicitly manage
 * the lifetime of serialized references.
 * 
 * @see HandleOutputStream
 * @see Handle
 * @author quark
 */
public class HandleInputStream extends ObjectInputStream {
	public HandleInputStream() throws IOException, SecurityException {
		super();
	}

	public HandleInputStream(InputStream in) throws IOException {
		super(in);
	}

	private Map<Integer, Object> handled = new HashMap<Integer, Object>();
	
	/** Get the value referenced by a handle ID. */
	public Object getHandled(int id) {
		final Object out = handled.get(id);
		assert(out != null);
		return out;
	}
	
	/** Set the value referenced by a handle ID. */
	public void putHandled(int id, Object value) {
		assert(value != null);
		handled.put(id, value);
	}
	
	/** Free a handle ID. */
	public void freeHandled(int id) {
		Object out = handled.remove(id);
		assert(out != null);
	}
	
	public <E> Handle<E> readHandle() throws IOException {
		try {
			return (Handle<E>)readObject();
		} catch (ClassNotFoundException c) {
			// pretty unlikely, since the caller
			// had to load the class to typecheck
			// this call
			throw new AssertionError(c);
		}
	}
}
