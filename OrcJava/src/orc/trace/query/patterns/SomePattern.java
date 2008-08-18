package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.query.Frame;
import orc.trace.query.Term;
import orc.trace.values.ConsValue;
import orc.trace.values.ListValue;
import orc.trace.values.SomeValue;
import orc.trace.values.Value;

public class SomePattern extends Pattern {
	public final Term content;
	public SomePattern(final Term content) {
		super();
		this.content = content;
	}
	public Frame unify(Frame frame, Term that_) {
		if (that_ instanceof SomePattern) {
			SomePattern that = (SomePattern)that_;
			return frame.unify(content, that.content);
		} else if (that_ instanceof SomeValue) {
			SomeValue that = (SomeValue)that_;
			return frame.unify(content, that.content);
		}
		return null;
	}
	public Term evaluate(Frame frame) {
		Term content1 = content.evaluate(frame);
		if (content1 instanceof Value) {
			return new SomeValue((Value)content1);
		} else {
			return new SomePattern(content1);
		}
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("Some(");
		content.prettyPrint(out, indent+1);
		out.write(")");
	}
	public boolean occurs(Variable v) {
		return content.occurs(v);
	}
}
