package orc.trace.query;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import orc.trace.EventCursor;
import orc.trace.Term;
import orc.trace.Terms;
import orc.trace.EventCursor.EndOfStream;
import orc.trace.events.Event;
import orc.trace.query.patterns.BindingPattern;
import orc.trace.query.patterns.Pattern;
import orc.trace.query.patterns.Variable;

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
	
	/** Empty binding list. */
	private static final Binding EMPTY_BINDING = new Binding() {
		public Term get(Variable v) {
			return null;
		}
		public void prettyPrintHead(Writer out, int indent) throws IOException {
			out.write("{");
		}
	};
	
	/** The variable bindings. */
	private Binding bindings;
	/** The current event cursor. */
	private EventCursor cursor;
	
	public Frame(EventCursor events) {
		this(EMPTY_BINDING, events);
	}
	
	private Frame(Binding bindings, EventCursor events) {
		this.bindings = bindings;
		this.cursor = events;
	}
	
	/**
	 * An unbound variable returns null.
	 */
	public Term get(Variable v) {
		if (v == CURRENT_EVENT) {
			return currentEvent();
		}
		return bindings.get(v);
	}
	
	public Frame bind(Variable v, Term p) {
		assert(p != null);
		if (v == p) return this;
		Term t = get(v);
		if (t != null) {
			return unify(t, p);
		} else if (!occurs(p, v)) {
			return new Frame(new FullBinding(this.bindings, v, evaluate(p)), cursor);
		} else {
			return null;
		}
	}
	
	public Event currentEvent() {
		return cursor.current();
	}
	
	public Frame at(EventCursor cursor) {
		return new Frame(bindings, cursor);
	}
	
	/**
	 * Return a frame located at the next event in the stream.
	 */
	public Frame forward() throws EndOfStream {
		return at(cursor.forward());
	}
	
	/**
	 * Return a frame located at the previous event in the stream.
	 */
	public Frame backward() throws EndOfStream {
		return at(cursor.backward());
	}
	
	/**
	 * Return a frame at the same stream as that,
	 * but with bindings from this.
	 */
	public Frame rewind(Frame that) {
		return at(that.cursor);
	}
	
	/**
	 * Unify two patterns. Checks the type of each term to preserve invariants
	 * of {@link Term#unify(Frame, Term)}.
	 */
	public Frame unify(Term left, Term right) {
		if (left instanceof Variable) {
			return ((Pattern)left).unify(this, right);
		} else if (right instanceof Variable) {
			return ((Pattern)right).unify(this, left);
		} else if (left instanceof BindingPattern) {
			return ((Pattern)left).unify(this, right);
		} else if (right instanceof BindingPattern) {
			return ((Pattern)right).unify(this, left);
		} else if (left instanceof Pattern) {
			return ((Pattern)left).unify(this, right);
		} else if (right instanceof Pattern) {
			return ((Pattern)right).unify(this, right);
		} else {
			return left.equals(right) ? this : null;
		}
	}
	
	/**
	 * Substitute bound variables to produce a more
	 * concrete term if possible.
	 */
	public Term evaluate(Term t) {
		if (t instanceof Pattern) {
			return ((Pattern)t).evaluate(this);
		} else return t;
	}
	
	/**
	 * Check if a variable occurs in this term.
	 */
	public static boolean occurs(Term t, Variable var) {
		if (t instanceof Pattern) {
			return ((Pattern)t).occurs(var);
		} else return false;
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
		cursor.current().prettyPrint(out, indent);
		out.write(" ");
		bindings.prettyPrint(out, indent);
	}
}
