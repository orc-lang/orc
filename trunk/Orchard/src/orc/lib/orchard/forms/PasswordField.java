package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

import orc.lib.net.XMLUtils;

public class PasswordField extends SingleField<String> {
	public PasswordField(String key, String label) {
		super(key, label, "");
	}

	@Override
	public String requestToValue(String posted) throws ValidationException {
		return posted;
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='password'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" value='" + XMLUtils.escapeXML(posted) + "'" +
				">");	
	}
}
