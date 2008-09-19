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