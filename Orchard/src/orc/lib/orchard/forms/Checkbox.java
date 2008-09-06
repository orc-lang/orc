package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public class Checkbox extends Field<Boolean> {

	public Checkbox(String key, String label) {
		super(key, label, false);
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='checkbox'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				(value ? " checked" : "") + ">");
	}

	@Override
	public Boolean requestToValue(String value) throws ValidationException {
		return (value != null);
	}
}
