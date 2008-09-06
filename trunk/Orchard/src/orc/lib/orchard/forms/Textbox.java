package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public class Textbox extends Field<String> {

	public Textbox(String key, String label) {
		super(key, label, "");
	}

	@Override
	public String requestToValue(String value) throws ValidationException {
		return value;
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='textbox'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" value='" + escapeHtml(value) + "'" +
				">");	
	}
}
