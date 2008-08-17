package orc.trace.query;

import java.util.HashMap;
import java.util.Map;

import orc.trace.events.Event;
import orc.trace.query.patterns.ConsPattern;
import orc.trace.query.patterns.Pattern;
import orc.trace.query.patterns.Variable;
import orc.trace.values.ConstantValue;
import orc.trace.values.NilValue;
import orc.trace.values.RecordValue;
import orc.trace.values.Value;

/**
 * Environment of variable bindings.
 * @author quark
 */
public class Frame {
	private Map<Variable, Term> bindings = new HashMap<Variable, Term>();
	
	/**
	 * An unbound variable returns null.
	 */
	public Term get(Variable v) {
		return bindings.get(v);
	}
	
	public boolean bind(Variable v, Term p) {
		assert(p != null);
		if (v == p) {
			return true;
		} else if (bindings.containsKey(v)) {
			return unify(bindings.get(v), p);
		} else if (!p.occurs(v)) {
			bindings.put(v, p);
			return true;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return bindings.toString();
	}
	
	/**
	 * Unify two patterns.
	 */
	public boolean unify(Term left, Term right) {
		if (left instanceof Variable) {
			return left.unify(this, right);
		} else if (right instanceof Variable) {
			return right.unify(this, left);
		} else if (left instanceof Pattern) {
			return left.unify(this, right);
		} else {
			return right.unify(this, left);
		}
	}
	
	public static void main(String[] args) {
		Frame frame = new Frame();
		Variable x = new Variable();
		Term left = new ConsPattern(x, new Variable());
		RecordValue r = new RecordValue(new Object().getClass());
		r.put("foo", new ConstantValue("bar"));
		Term right = new ConsPattern(r, x);
		System.out.println(left);
		System.out.println(right);
		frame.unify(left, right);
		System.out.println(frame);
		System.out.println(left.substitute(frame));
	}
}
