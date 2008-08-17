package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.query.Frame;
import orc.trace.query.Term;

public class PropertyPattern extends Pattern {
	public boolean unify(Frame frame, Term that) {
		// TODO Auto-generated method stub
		return false;
	}

	public Term substitute(Frame frame) {
		// TODO Auto-generated method stub
		return null;
	}

	public void prettyPrint(Writer out, int indent) throws IOException {
		// TODO Auto-generated method stub
	}
	
	public boolean occurs(Variable v) {
		// TODO Auto-generated method stub
		return false;
	}
}
