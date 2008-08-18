package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.query.Frame;
import orc.trace.query.Term;

public class Variable extends BindingPattern {
	private static int lastIndex = 0;
	private int index;
	public Variable() {
		index = ++lastIndex;
	}
	public Frame unify(Frame frame, Term value) {
		return frame.bind(this, value);
	}
	public Term evaluate(Frame frame) {
		Term term = frame.get(this);
		if (term != null) {
			return term.evaluate(frame);
		} else {
			return this;
		}
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("?" + index);
	}
	public boolean occurs(Variable v) {
		return equals(v);
	}
}
