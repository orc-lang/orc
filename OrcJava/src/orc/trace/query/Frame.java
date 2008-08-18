package orc.trace.query;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import orc.trace.events.Event;
import orc.trace.query.patterns.BindingPattern;
import orc.trace.query.patterns.ConsPattern;
import orc.trace.query.patterns.Pattern;
import orc.trace.query.patterns.PropertyPattern;
import orc.trace.query.patterns.Variable;
import orc.trace.values.ConstantValue;
import orc.trace.values.RecordValue;

/**
 * Environment of variable bindings.
 * TODO: currently implmented as a linked list; make this more efficient.
 * @author quark
 */
public abstract class Frame {
	/**
	 * Construct a new frame for the given event stream.
	 */
	public static Frame newFrame(EventStream events) {
		return new BindEvent(EMPTY, events);
	}
	/**
	 * The empty frame.
	 */
	public static final Frame EMPTY = new Frame() {
		public Term get(Variable v) {
			return null;
		}
		public void prettyPrintHead(Writer out, int indent) throws IOException {
			out.write("{");
		}
		protected EventStream events() throws NoSuchElementException {
			throw new NoSuchElementException();
		}
	};
	/**
	 * Base class for any frame which extends another frame.
	 */
	private static abstract class Extended extends Frame {
		private Frame parent;
		public Extended(Frame parent) {
			this.parent = parent;
		}
		public Term get(Variable v) {
			return parent.get(v);
		}
		public EventStream events() throws NoSuchElementException {
			return parent.events();
		}
		public void prettyPrintHead(Writer out, int indent) throws IOException {
			parent.prettyPrintHead(out, indent);
		}
	}
	/**
	 * Frame which binds a variable.
	 */
	private static class BindVariable extends Extended {
		private Variable variable;
		private Term term;
		public BindVariable(Frame parent, Variable variable, Term term) {
			super(parent);
			this.variable = variable;
			this.term = term;
		}
	
		public Term get(Variable v) {
			if (variable.equals(v)) return term;
			else return super.get(v);
		}
	
		public void prettyPrintHead(Writer out, int indent) throws IOException {
			super.prettyPrintHead(out, indent);
			if (!variable.isAnonymous()) {
				out.write("\n");
				Terms.indent(out, indent+1);
				variable.prettyPrint(out, indent+1);
				out.write(":");
				term.prettyPrint(out, indent+2);
			}
		}
	}
	private static class BindEvent extends Extended {
		private EventStream stream;
		public BindEvent(Frame parent, EventStream stream) {
			super(parent);
			this.stream = stream;
		}
		public EventStream events() {
			return stream;
		}
	}
	
	/**
	 * An unbound variable returns null.
	 */
	public abstract Term get(Variable v);
	/**
	 * If no stream is available, throws an exception.
	 */
	protected abstract EventStream events() throws NoSuchElementException;
	
	public Frame bind(Variable v, Term p) {
		assert(p != null);
		if (v == p) return this;
		Term t = get(v);
		if (t != null) {
			return unify(t, p);
		} else if (!p.occurs(v)) {
			return new BindVariable(this, v, p);
		} else {
			return null;
		}
	}
	
	public Event currentEvent() throws NoSuchElementException {
		return events().head();
	}
	
	public Frame nextEvent() throws NoSuchElementException {
		return new BindEvent(this, events().tail());
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
	
	public static void main(String[] args) {
		Frame frame = Frame.EMPTY;
		Variable x = new Variable();
		Term left = new ConsPattern(x, new PropertyPattern(x, "foo"));
		RecordValue r = new RecordValue(new Object().getClass());
		r.put("foo", new ConstantValue("bar"));
		Term right = new ConsPattern(r, new ConstantValue("bar"));
		System.out.print("LEFT = ");
		System.out.println(left);
		System.out.print("RIGHT = ");
		System.out.println(right);
		Frame frame1 = frame.unify(left, right);
		if (frame1 != null) {
			System.out.print("BINDINGS = ");
			System.out.println(frame1);
			System.out.print("RESULT = ");
			System.out.println(left.evaluate(frame1));
		} else {
			System.out.println("COULD NOT UNIFY");
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
		prettyPrintHead(out, indent);
		out.write("\n");
		Terms.indent(out, indent);
		out.write("}");
	}
	
	protected abstract void prettyPrintHead(Writer out, int indent) throws IOException;
}
