package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.query.Frame;
import orc.trace.values.ConsValue;
import orc.trace.values.ListValue;
import orc.trace.values.Value;

public class ConsPattern extends Pattern {
	public final Term head;
	public final Term tail;
	public ConsPattern(final Term head, final Term tail) {
		super();
		this.head = head;
		this.tail = tail;
	}
	public Frame unify(Frame frame, Term that_) {
		if (that_ instanceof ConsPattern) {
			ConsPattern that = (ConsPattern)that_;
			Frame frame1 = frame.unify(head, that.head);
			if (frame1 == null) return null;
			return frame1.unify(tail, that.tail);
		} else if (that_ instanceof ConsValue) {
			ConsValue that = (ConsValue)that_;
			Frame frame1 = frame.unify(head, that.head);
			if (frame1 == null) return null;
			return frame1.unify(tail, that.tail);
		}
		return null;
	}
	public Term evaluate(Frame frame) {
		Term head1 = frame.evaluate(head);
		Term tail1 = frame.evaluate(tail);
		if (head1 instanceof Value && tail1 instanceof ListValue) {
			// if the variables are gone, use a value
			return new ConsValue((Value)head1, (ListValue)tail1);
		} else {
			return new ConsPattern(head1, tail1);
		}
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("(");
		head.prettyPrint(out, indent+1);
		out.write(":");
		tail.prettyPrint(out, indent+1);
		out.write(")");
	}
	public boolean occurs(Variable v) {
		return Frame.occurs(head, v) || Frame.occurs(tail, v);
	}
}
