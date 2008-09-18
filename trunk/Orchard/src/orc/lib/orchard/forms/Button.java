package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


public class Button implements Part<Boolean> {
	private String key;
	private String label;
	private boolean clicked = false;

	public Button(String key, String label) {
		this.key = key;
		this.label = label;
	}

	public void render(PrintWriter out) throws IOException {
		out.write("<input type='submit'" +
				" name='" + key + "'" +
				" value='" + label + "'" +
				">");
	}

	public String getKey() {
		return key;
	}

	public Boolean getValue() {
		return clicked;
	}
	
	public boolean isMultipart() {
		return false;
	}

	public void readRequest(FormData request, List<String> errors) {
		clicked = (request.getParameter(key) != null);
	}
}
