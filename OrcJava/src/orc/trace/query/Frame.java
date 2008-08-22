package orc.trace.query;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.query.EventCursor.EndOfStream;
import orc.trace.query.patterns.BindingPattern;
import orc.trace.query.patterns.ConsPattern;
import orc.trace.query.patterns.Pattern;
import orc.trace.query.patterns.PropertyPattern;
import orc.trace.query.patterns.Variable;
import orc.trace.values.ConstantValue;
import orc.trace.values.RecordValue;

/**
 * Immutable environment of variable bindings and a location in the event stream.
 * TODO: currently implmented as a linked list; could be more efficient.
 * @author quark
 */
public class Frame {
	/** Special variable used to stand for the current event. */
	public final static Variable CURRENT_EVENT = new Variable();
	/**
	 * Base type for variable bindings, stored as a linked list.
	 */
	private static abstract class Binding {
		public abstract Term get(Variable v);
		
		public void prettyPrint(Writer out, int indent) throws IOException {
			prettyPrintHead(out, indent);
			out.write("\n");
			Terms.indent(out, indent);
			out.write("}");
		}
		
		protected abstract void prettyPrintHead(Writer out, int indent) throws IOException;
	}
	
	/** A node of a binding linked list. */
	private static class FullBinding extends Binding {
		private Binding parent;
		private Variable variable;
		private Term term;
		public FullBinding(Binding parent, Variable variable, Term term) {
			this.parent = parent;
			this.variable = variable;
			this.term = term;
		}
	
		public Term get(Variable v) {
			if (variable.equals(v)) return term;
			else return parent.get(v);
		}
	
		public void prettyPrintHead(Writer out, int indent) throws IOException {
			parent.prettyPrintHead(out, indent);
			if (!variable.isAnonymous()) {
				out.write("\n");
				Terms.indent(out, indent+1);
				variable.prettyPrint(out, indent+1);
				out.write(":");
				term.prettyPrint(out, indent+2);
			}
		}
	}
	
	/** Empty event cursor. */
	private static final EventCursor EMPTY_EVENTS = new EventCursor() {
		public Event current() throws EndOfStream {
			throw new EndOfStream();
		}
		public EventCursor forward() throws EndOfStream {
			throw new EndOfStream();
		}
		public EventCursor backward() throws EndOfStream {
			throw new EndOfStream();
		}
	};
	
	/** Empty binding list. */
	private static final Binding EMPTY_BINDING = new Binding() {
		public Term get(Variable v) {
			return null;
		}
		public void prettyPrintHead(Writer out, int indent) throws IOException {
			out.write("{");
		}
	};
	
	/**
	 * Construct a new frame for the given cursor.
	 */
	public static Frame newFrame(EventCursor events) {
		return new Frame(EMPTY_BINDING, events);
	}
	
	/**
	 * Empty frame.
	 */
	public static final Frame EMPTY = new Frame(EMPTY_BINDING, EMPTY_EVENTS);
	
	/** The variable bindings. */
	private Binding bindings;
	/** The current event cursor. */
	private EventCursor cursor;
	
	private Frame(Binding bindings, EventCursor events) {
		this.bindings = bindings;
		this.cursor = events;
	}
	
	/**
	 * An unbound variable returns null.
	 */
	public Term get(Variable v) {
		if (v == CURRENT_EVENT) {
			try {
				return currentEvent();
			} catch (EndOfStream e) {
				return null;
				// FIXME: currentEvent shouldn't be able
				// to throw this exception
			}
		}
		return bindings.get(v);
	}
	
	public Frame bind(Variable v, Term p) {
		assert(p != null);
		if (v == p) return this;
		Term t = get(v);
		if (t != null) {
			return unify(t, p);
		} else if (!p.occurs(v)) {
			return new Frame(new FullBinding(this.bindings, v, p.evaluate(this)), cursor);
		} else {
			return null;
		}
	}
	
	public Event currentEvent() throws EndOfStream {
		return cursor.current();
	}
	
	/**
	 * Return a frame located at the next event in the stream.
	 */
	public Frame forward() throws EndOfStream {
		return new Frame(bindings, cursor.forward());
	}
	
	/**
	 * Return a frame located at the previous event in the stream.
	 */
	public Frame backward() throws EndOfStream {
		return new Frame(bindings, cursor.backward());
	}
	
	/**
	 * Return a frame at the same stream as that,
	 * but with bindings from this.
	 */
	public Frame rewind(Frame that) {
		return new Frame(bindings, that.cursor);
	}
	
	/**
	 * Unify two patterns. Checks the type of each term to preserve invariants
	 * of {@link Term#unify(Frame, Term)}.
	 */
	public Frame unify(Term left, Term right) {
		if (left instanceof Variable) {
			return left.unify(this, right);
		} else if (right instanceof Variable) {
			return right.unify(this, left);
		} else if (left instanceof BindingPattern) {
			return left.unify(this, right);
		} else if (right instanceof BindingPattern) {
			return right.unify(this, left);
		} else if (left instanceof Pattern) {
			return left.unify(this, right);
		} else {
			return right.unify(this, left);
		}
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
	
	public void prettyPrint(Writer out, int indent) throws IOException {
		try {
			cursor.current().prettyPrint(out, indent);
			out.write(" ");
		} catch (EndOfStream _) {
			out.write("END ");
		}
		bindings.prettyPrint(out, indent);
	}
}
