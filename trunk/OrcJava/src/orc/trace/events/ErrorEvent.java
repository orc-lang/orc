package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.error.runtime.TokenException;
import orc.trace.Term;
import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.ConstantValue;
import orc.trace.values.Value;

/**
 * A fatal error in a thread.
 */
public class ErrorEvent extends Event {
	public final TokenException error;
	public ErrorEvent(TokenException error) {
		this.error = error;
	}
	@Override
	public void prettyPrintProperties(Writer out, int indent) throws IOException {
		super.prettyPrintProperties(out, indent);
		prettyPrintProperty(out, indent, "errorMessage",
				new ConstantValue(error.getMessage()));
		prettyPrintProperty(out, indent, "errorType",
				new ConstantValue(error.getClass().getName()));
	}
	public Term getProperty(String key) {
		if (key.equals("errorMessage")) return new ConstantValue(error.getMessage());
		if (key.equals("errorType")) return new ConstantValue(error.getClass().getName());
		else return super.getProperty(key);
	}
	@Override
	public String getType() { return "error"; }
	@Override
	public <V> V accept(Visitor<V> visitor) {
		return visitor.visit(this);
	}
}
