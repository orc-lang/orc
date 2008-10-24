package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

public class Mandatory<V> implements Part<V> {
	private Field<V> part;
	public Mandatory(Field<V> part) {
		this.part = part;
	}
	
	public String getKey() {
		return part.getKey();
	}

	public V getValue() {
		return part.getValue();
	}

	public boolean needsMultipartEncoding() {
		return part.needsMultipartEncoding();
	}

	public void readRequest(FormData request, List<String> errors) {
		part.readRequest(request, errors);
		if (getValue() == null) {
			errors.add("Please provide " + part.getLabel());
		}
	}

	public void render(PrintWriter out, Set<String> flags) throws IOException {
		part.renderHeader(out, flags);
		out.write("<label for='" + part.getKey() + "'>" + part.getLabel());
		part.renderControl(out);
		out.write(" <i>(required)</i>");
		out.write("</label>");
	}
}
