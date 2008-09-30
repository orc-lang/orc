package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

import orc.lib.net.XMLUtils;

public class PasswordField extends Textbox {
	public PasswordField(String key, String label) {
		super(key, label);
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
