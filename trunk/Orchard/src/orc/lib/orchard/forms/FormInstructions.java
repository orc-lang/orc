package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class FormInstructions implements Part<String> {
	private String key;
	private String value;
	public FormInstructions(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public boolean isMultipart() {
		return false;
	}

	public void readRequest(FormData request, List<String> errors) {
		// do nothing
	}

	public void render(PrintWriter out) throws IOException {
		out.write(value);
	}
}
