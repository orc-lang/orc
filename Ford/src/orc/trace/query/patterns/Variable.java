package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.query.Frame;

public class Variable extends BindingPattern {
	private static int lastIndex = 0;
	private final String name;
	private final boolean anonymous;
	public Variable() {
		this.name = "_"+Integer.toString(++lastIndex);
		this.anonymous = true;
	}
	public Variable(String name) {
		this.name = name;
		this.anonymous = false;
	}
	public Frame unify(Frame frame, Term value) {
		return frame.bind(this, value);
	}
	public Term evaluate(Frame frame) {
		Term term = frame.get(this);
		if (term != null) {
			return frame.evaluate(term);
		} else {
			return this;
		}
	}
	public boolean isAnonymous() {
		return anonymous;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(name);
	}
	public boolean occurs(Variable v) {
		return equals(v);
	}
}
