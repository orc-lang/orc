package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

import orc.lib.net.XMLUtils;

public class Textbox extends SingleField<String> {

	public Textbox(String key, String label) {
		super(key, label, "");
	}

	@Override
	public String requestToValue(String posted) throws ValidationException {
		if (posted.equals("")) return null;
		return posted;
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<input type='textbox'" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" value='" + XMLUtils.escapeXML(posted) + "'" +
				">");	
	}
}
