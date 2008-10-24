package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

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

	public boolean needsMultipartEncoding() {
		return false;
	}

	public void readRequest(FormData request, List<String> errors) {
		// do nothing
	}

	public void render(PrintWriter out, Set<String> flags) throws IOException {
		out.write(value);
	}
}
