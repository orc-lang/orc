package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

import orc.lib.net.XMLUtils;

public class Textarea extends SingleField<String> {
	private boolean wrap;

	public Textarea(String key, String label, String value, boolean wrap) {
		super(key, label, value);
		this.wrap = wrap;
	}
	
	public Textarea(String key, String label, String value) {
		this(key, label, value, true);
	}

	@Override
	public String requestToValue(String posted) throws ValidationException {
		if (posted.trim().equals("")) return null;
		return posted.trim();
	}

	@Override
	public void renderControl(PrintWriter out) throws IOException {
		out.write("<textarea" +
				" id='" + key + "'" +
				" name='" + key + "'" +
				" rows='6' cols='60'" +
				(wrap ? "" : " wrap='off'") +
				">" + XMLUtils.escapeXML(posted) + "</textarea>");	
	}
}
