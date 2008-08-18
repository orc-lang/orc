package orc.trace;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import orc.error.OrcError;
import orc.runtime.values.Visitor;
import orc.trace.events.CallEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.Event;
import orc.trace.events.ForkEvent;
import orc.trace.events.ResumeEvent;
import orc.trace.handles.FirstHandle;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleOutputStream;
import orc.trace.handles.OnlyHandle;
import orc.trace.query.predicates.Predicate;
import orc.trace.values.ConsValue;
import orc.trace.values.ConstantValue;
import orc.trace.values.ListValue;
import orc.trace.values.Marshaller;
import orc.trace.values.NilValue;
import orc.trace.values.NoneValue;
import orc.trace.values.NullValue;
import orc.trace.values.ObjectValue;
import orc.trace.values.SomeValue;
import orc.trace.values.TraceableValue;
import orc.trace.values.TupleValue;
import orc.trace.values.Value;

/**
 * Serialize events to an OutputStream.
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
		this.out = new HandleOutputStream(out, STREAM_RESET_INTERVAL);
	}

	protected OutputStreamTracer(OutputStreamTracer that, ForkEvent fork) {
		super(that, fork);
		this.out = that.out;
	}
	
	@Override
	protected OutputStreamTracer forked(ForkEvent fork) {
		return new OutputStreamTracer(this, fork);
	}
	@Override
	protected void record(Handle<? extends Event> event) {
		synchronized (out) {
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
	}
}
