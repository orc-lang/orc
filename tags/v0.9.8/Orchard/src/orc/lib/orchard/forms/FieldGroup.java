package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class FieldGroup extends Aggregate {
	private String label;
	public FieldGroup(String key, String label) {
		super(key);
		this.label = label;
	}

	@Override
	public void render(PrintWriter out, Set<String> flags) throws IOException {
		out.write("<fieldset><legend>" + label + "</legend>");
		super.render(out, flags);
		out.write("</fieldset>");
	}
}
