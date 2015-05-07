package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.Term;
import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.values.ConstantValue;

/**
 * Indicate that a thread continued due to termination of the left side of a
 * semicolon combinator.
 * 
 * @author quark
 */
public class AfterEvent extends Event {
	public final Handle<BeforeEvent> before;
	public AfterEvent(BeforeEvent before) {
		this.before = new LastHandle<BeforeEvent>(before);
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "before",
				new ConstantValue(before.get()));
	}
	@Override
	public Term getProperty(String key) {
		if (key.equals("before")) return before.get();
		else return super.getProperty(key);
	}
	@Override
	public String getType() {
		return "after";
	}
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
