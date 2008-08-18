package orc.trace.query;

import java.io.IOException;
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
	public static Frame newFrame(EventStream events) {
		return new BindEvent(EMPTY, events);
	}
	public static final Frame EMPTY = new Frame() {
		public Term get(Variable v) {
			return null;
		}
		public String toString() {
			return "Frame.EMPTY";
		}
		protected EventStream events() throws NoSuchElementException {
			throw new NoSuchElementException();
		}
	};
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
		public String toString() {
			return "+" + parent;
		}
	}
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
	
		public String toString() {
			return variable + ":" + term + super.toString();
		}
	}
	private static class BindEvent extends Extended {
		private EventStream stream;
		public BindEvent(Frame parent, EventStream stream) {
			super(parent);
			this.stream = stream;
		}
		public String toString() {
			return stream + super.toString();
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
}
