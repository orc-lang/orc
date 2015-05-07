package orc.trace;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.events.ForkEvent;
import orc.trace.handles.Handle;
import orc.trace.values.Marshaller;

/**
 * Write trace events to stdout in human-readable form.
 * FIXME: make which events are written configurable.
 * 
 * @author quark
 */
public final class PrintStreamTracer extends AbstractTracer {
	private final OutputStreamWriter out;
	private int seq = 0;
	public PrintStreamTracer(OutputStream out) {
		this.out = new OutputStreamWriter(out);
	}

	protected synchronized void record(Handle<? extends Event> event) {
		try {
			event.get().setSeq(seq++);
			event.get().prettyPrint(out, 0);
			out.write('\n');
			out.flush();
		} catch (IOException e) {
			// FIXME: is there a better way to handle this?
			// I don't want to pass the exception on to the
			// caller since there's no way to recover from
			// it.  Maybe we should just print an error
			// message rather than kill the whole engine.
			throw new OrcError(e);
		}
	}
	
	public void finish() {}
}
