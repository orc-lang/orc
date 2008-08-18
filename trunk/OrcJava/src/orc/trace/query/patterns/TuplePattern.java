package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.query.Frame;
import orc.trace.query.Term;
import orc.trace.query.Terms;
import orc.trace.values.SomeValue;
import orc.trace.values.TupleValue;
import orc.trace.values.Value;

public class TuplePattern extends Pattern {
	public final Term[] values;
	public TuplePattern(final Term[] values) {
		super();
		this.values = values;
	}
	public Frame unify(Frame frame, Term that_) {
		if (that_ instanceof TuplePattern) {
			TuplePattern that = (TuplePattern)that_;
			if (that.values.length != values.length) return null;
			Frame out = frame;
			for (int i = 0; i < values.length; ++i) {
				out = out.unify(values[i], that.values[i]);
				if (out == null) break;
			}
			return out;
		} else if (that_ instanceof TupleValue) {
			TupleValue that = (TupleValue)that_;
			if (that.values.length != values.length) return null;
			Frame out = frame;
			for (int i = 0; i < values.length; ++i) {
				out = out.unify(values[i], that.values[i]);
				if (out == null) break;
			}
			return out;
		}
		return null;
	}
	public Term evaluate(Frame frame) {
		// used to check if all substituted subterms are values
		boolean isValue = true;
		// substitute in subterms
		Term[] values1 = new Term[values.length];
		for (int i = 0; i < values.length; ++i) {
			values1[i] = values[i].evaluate(frame);
			isValue = isValue && values1[i] instanceof Value;
		}
		if (isValue) {
			return new TupleValue((Value[])values1);
		} else {
			return new TuplePattern(values1);
		}
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("(");
		Terms.prettyPrintList(out, indent+1, Arrays.asList(values), ", ");
		out.write(")");
	}
	public boolean occurs(Variable v) {
		for (int i = 0; i < values.length; ++i) {
			if (values[i].occurs(v)) return true;
		}
		return false;
	}
}
