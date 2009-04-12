package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public class Checkbox extends SingleField<Boolean> {

	public Checkbox(String key, String label) {
		super(key, label, null);
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='checkbox'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				(posted == null ? "" : " checked") + ">");
	}

	@Override
	public Boolean requestToValue(String posted) throws ValidationException {
		return (posted != null);
	}
}
