package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import orc.trace.query.Frame;
import orc.trace.query.RecordTerm;
import orc.trace.query.Term;
import orc.trace.query.Terms;

public class RecordPattern extends Pattern {
	private Map<String,Term> properties = new HashMap<String,Term>();
	public void put(String key, Term value) {
		properties.put(key, value);
	}
	public boolean unify(Frame frame, Term that_) {
		if (that_ instanceof RecordPattern) {
			RecordPattern that = (RecordPattern)that_;
			// For two patterns to unify, their common properties
			// have to unify
			for (Map.Entry<String,Term> entry : properties.entrySet()) {
				Term value2 = that.properties.get(entry.getKey());
				if (value2 == null) continue;
				if (!frame.unify(entry.getValue(), value2)) return false;
			}
			return true;
		} else if (that_ instanceof RecordTerm) {
			RecordTerm that = (RecordTerm)that_;
			// For a pattern to unify with an arbitrary term, all
			// properties in the pattern must unify with those in
			// the value.
			// FIXME: it seems weird that terms and patterns are
			// treated differently. Is this right?
			for (Map.Entry<String,Term> entry : properties.entrySet()) {
				Term value2 = that.getProperty(entry.getKey());
				if (value2 == null) return false;
				if (!frame.unify(entry.getValue(), value2)) return false;
			}
			return true;
		}
		return false;
	}

	public Term substitute(Frame frame) {
		RecordPattern out = new RecordPattern();
		Map<String, Term> properties1 = new HashMap<String, Term>();
		for (Map.Entry<String,Term> entry : properties.entrySet()) {
			out.put(entry.getKey(), entry.getValue().substitute(frame));
		}
		return out;
	}
	
	public boolean occurs(Variable v) {
		for (Term t : properties.values()) {
			if (t.occurs(v)) return true;
		}
		return false;
	}

	public void prettyPrint(Writer out, int indent) throws IOException {
		Terms.prettyPrintMap(out, indent, properties.entrySet());
	}
}
