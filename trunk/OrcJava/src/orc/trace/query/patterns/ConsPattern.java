package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import orc.trace.query.Frame;
import orc.trace.query.Term;
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
	public boolean unify(Frame frame, Term that_) {
		if (that_ instanceof ConsPattern) {
			ConsPattern that = (ConsPattern)that_;
			return frame.unify(head, that.head)
				&& frame.unify(tail, that.tail);
		} else if (that_ instanceof ConsValue) {
			ConsValue that = (ConsValue)that_;
			return head.unify(frame, that.head)
				&& tail.unify(frame, that.tail);
		}
		return false;
	}
	public Term substitute(Frame frame) {
		Term head1 = head.substitute(frame);
		Term tail1 = tail.substitute(frame);
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
		return head.occurs(v) || tail.occurs(v);
	}
}
