package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Form extends Aggregate {
	private Map<String, String> hiddens = new HashMap<String, String>();
	public Form() {
		super("");
	}
	
	public void setHidden(String key, String value) {
		hiddens.put(key, value);
	}

	public void render(PrintWriter out) throws IOException {
		out.write("<form method='post'");
		if (isMultipart()) out.write(" enctype='multipart/form-data'");
		out.write(">");
		for (Map.Entry<String, String> hidden : hiddens.entrySet()) {
			out.write("<input type='hidden'" +
					" name='" + hidden.getKey() + "'" +
					" value='" + SingleField.escapeHtml(hidden.getValue()) + "'" +
					">");
		}
		super.render(out);
		out.write("</form>");
	}
}
