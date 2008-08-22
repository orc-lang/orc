package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.Handle;
import orc.trace.handles.LastHandle;
import orc.trace.query.Term;

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
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		before.get().prettyPrint(out, indent+1);
		out.write(")");
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
}
