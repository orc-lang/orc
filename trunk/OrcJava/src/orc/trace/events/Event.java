package orc.trace.events;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.trace.handles.Handle;
import orc.trace.handles.RepeatHandle;
import orc.trace.query.Frame;
import orc.trace.query.RecordTerm;
import orc.trace.query.Term;
import orc.trace.query.patterns.Variable;
import orc.trace.values.ConstantValue;

/**
 * Base class for trace events.
 * @author quark
 */
public abstract class Event implements Serializable, RecordTerm, Locatable {
	protected Handle<ForkEvent> thread;
	protected SourceLocation location;
	
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
	 * Return a characteristic (but not guaranteed unique) 
	 * human-readable label for the event.
	 */
	public String label() {
		return Integer.toHexString(hashCode());
	}
	
	public String toString() {
		try {
			StringWriter writer = new StringWriter();
			prettyPrint(writer, 0);
			return writer.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
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
	
	public Frame unify(Frame frame, Term that) {
		return equals(that) ? frame : null;
	}
	
	public Term evaluate(Frame frame) {
		return this;
	}
	
	public boolean occurs(Variable var) {
		return false;
	}
	
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(String.valueOf(location));
		out.write(":");
		out.write(thread.get().label());
		out.write(":");
		out.write(getClass().getSimpleName());
	}
}
