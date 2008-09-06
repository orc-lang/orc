package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public class IntegerField extends Field<Integer> {

	public IntegerField(String key, String label) {
		super(key, label, null);
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='textbox'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" value='" + (value == null ? "" : value) + "'" +
				">");	
	}

	@Override
	public Integer requestToValue(String value) throws ValidationException {
		try {
			if (value.equals("")) return null;
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new ValidationException(label + " must be an integer.");
		}
	}
}
