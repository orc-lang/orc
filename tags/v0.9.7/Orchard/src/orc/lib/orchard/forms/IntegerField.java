package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public class IntegerField extends SingleField<Integer> {

	public IntegerField(String key, String label) {
		super(key, label, "");
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='textbox'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" value='" + posted + "'" +
				">");	
	}

	@Override
	public Integer requestToValue(String posted) throws ValidationException {
		try {
			if (posted.equals("")) return null;
			return Integer.parseInt(posted);
		} catch (NumberFormatException e) {
			throw new ValidationException(label + " must be an integer.");
		}
	}
}
