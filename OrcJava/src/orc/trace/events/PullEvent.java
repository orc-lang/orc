package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

/**
 * This is just a way to uniquely identify a pull. It should preceed the
 * corresponding fork event.
 * 
 * @author quark
 */
public class PullEvent extends Event {
	@Override
	public String getType() {
		return "pull";
	}
	@Override
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		out.write("(");
		out.write(label());
		out.write(")");
	}
}
