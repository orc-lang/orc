package orc.trace.events;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.trace.EventCursor;
import orc.trace.RecordTerm;
import orc.trace.Term;
import orc.trace.Terms;
import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;

/**
 * Base class for trace events.
 * @author quark
 */
public abstract class Event implements Serializable, RecordTerm, Locatable {
	protected Handle<ForkEvent> thread;
	protected SourceLocation location;
	transient protected EventCursor cursor;
	transient protected long seq;
	
	public void setThread(ForkEvent thread) {
		this.thread = new RepeatHandle<ForkEvent>(thread);
	}
	
	/**
	 * A thread is represented by the {@link ForkEvent} which spawned it.
	 */
	public ForkEvent getThread() {
		return thread.get();
	}
	
	public void setSourceLocation(SourceLocation location) {
		this.location = location;
	}
	
	public SourceLocation getSourceLocation() {
		return location;
	}
	
	/**
	 * Get the event cursor which produced this event.
	 */
	public EventCursor getCursor() {
		return cursor;
	}

	/**
	 * Used by implementations of {@link EventCursor} to
	 * associate this event with a location in an event stream.
	 * Clients shouldn't call this.
	 */
	public void setCursor(EventCursor cursor) {
		this.cursor = cursor;
	}

	/**
	 * Get the sequence number, which uniquely
	 * identifies this event.
	 */
	public long getSeq() {
		return seq;
	}

	/**
	 * Used by implementations of {@link EventCursor} to
	 * set the event's sequence number.
	 * Clients shouldn't call this.
	 */
	public void setSeq(long seq) {
		this.seq = seq;
	}
	
	/**
	 * Return a human-readable short label for the event.
	 */
	public String getLabel() {
		return getType() + ":" + Long.toHexString(getSeq());
	}
	
	public String toString() {
		return Terms.printToString(this);
	}
	
	/**
	 * Return a string name for the type of event. Used in pattern matching.
	 */
	public abstract String getType();
	
	///////////////////////////////////////////////////
	// Term unification for events
	
	public Term getProperty(String key) {
		if (key.equals("thread")) return thread.get();
		else if (key.equals("type")) return new ConstantValue(getType());
		return null;
	}
	
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		prettyPrintProperty(out, indent, "thread",
				new ConstantValue(thread.get().getLabel()));
	}
	
	protected void prettyPrintProperty(Writer out, int indent, String key, Term value) throws IOException {
		out.write("\n");
		Terms.indent(out, indent);
		out.write(key);
		out.write(": ");
		value.prettyPrint(out, indent+1);
	}
	
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(getLabel());
		out.write(" {");
		prettyPrintProperties(out, indent+1);
		out.write("\n");
		Terms.indent(out, indent);
		out.write("}");
	}
}
