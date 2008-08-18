package orc.trace.query;

import java.util.HashMap;
import java.util.Map;

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
	public static final Variable EVENT = new Variable();
	public static final Frame EMPTY = new Frame() {
		public Term get(Variable v) {
			return null;
		}
		public String toString() {
			return "Frame.EMPTY";
		}
	};
	public static class Binding extends Frame {
		private Frame parent;
		private Variable variable;
		private Term term;
		public Binding(Frame parent, Variable variable, Term term) {
			this.parent = parent;
			this.variable = variable;
			this.term = term;
		}
	
		public Term get(Variable v) {
			if (variable.equals(v)) return term;
			else return parent.get(v);
		}
	
		public String toString() {
			return variable + ":" + term + "+" + parent;
		}
	}
	
	/**
	 * An unbound variable returns null.
	 */
	public abstract Term get(Variable v);
	
	public Frame bind(Variable v, Term p) {
		assert(p != null);
		if (v == p) return this;
		Term t = get(v);
		if (t != null) {
			return unify(t, p);
		} else if (!p.occurs(v)) {
			return new Binding(this, v, p);
		} else {
			return null;
		}
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
