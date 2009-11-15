package orc.trace;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleOutputStream;

/**
 * Serialize and gzip events to an {@link OutputStream}.
 * The output stream will be closed when the execution is finished.
 * You can read the events back with an {@link InputStreamEventCursor}.
 * 
 * @author quark
 */
public class OutputStreamTracer extends AbstractTracer {
	/**
	 * How often should the output stream reset to conserve
	 * memory?  We should pick a value empirically, erring
	 * on the side of caution.  I pulled 100 out of my ass.
	 */
	private static final int STREAM_RESET_INTERVAL = 100;
	/** Output stream for events. */
	private final HandleOutputStream out;
	
	public OutputStreamTracer(OutputStream out) throws IOException {
		this.out = new HandleOutputStream(new GZIPOutputStream(out), STREAM_RESET_INTERVAL);
	}
	
	@Override
	protected synchronized void record(Handle<? extends Event> event) {
		try {
			// Handles manage sharing explicitly, so no need
			// to use the implicit writeObject sharing management
			out.writeUnshared(event);
			out.maybeReset();
		} catch (IOException e) {
			// FIXME: is there a better way to handle this?
			// I don't want to pass the exception on to the
			// caller since there's no way to recover from
			// it.  Maybe we should just print an error
			// message rather than kill the whole engine.
			throw new OrcError(e);
		}
	}
	
	public void finish() {
		try {
			out.close();
		} catch (IOException e) {
			throw new OrcError(e);
		}
	}
}
