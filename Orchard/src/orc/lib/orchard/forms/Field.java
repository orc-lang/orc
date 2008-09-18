package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;

public abstract class Field<V> implements Part<V> {

	protected String label;
	protected String key;
	protected V value;
	
	public Field(String key, String label, V value) {
		this.key = key;
		this.label = label;
		this.value = value;
	}

	public static String escapeHtml(String text) {
		StringBuilder sb = new StringBuilder();
		int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);
			switch (c) {
			case 34: sb.append("&quot;"); break;
			case 38: sb.append("&amp;"); break;
			case 39: sb.append("&apos;"); break;
			case 60: sb.append("&lt;"); break;
			case 62: sb.append("&gt;"); break;
			default:
				if (c > 0x7F) {
					sb.append("&#");
					sb.append(Integer.toString(c, 10));
					sb.append(';');
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	public String getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public void render(PrintWriter out) throws IOException {
		out.write("<label for='" + key + "'>" + label);
		renderControl(out);
		out.write("</label>");
	}
	
	public boolean isMultipart() {
		return false;
	}

	public abstract void renderControl(PrintWriter out) throws IOException;
}