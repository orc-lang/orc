package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.query.Frame;
import orc.trace.query.Term;

public class Wildcard extends Pattern {
	public static Wildcard singleton = new Wildcard();
	private Wildcard() {}
	public boolean unify(Frame frame, Term value) {
		return true;
	}
	public Term substitute(Frame frame) {
		return this;
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("_");
	}
	public boolean occurs(Variable v) {
		return false;
	}
}
