package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

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
	
	public String getLabel() {
		return label;
	}

	public V getValue() {
		return value;
	}

	public void render(PrintWriter out, Set<String> flags) throws IOException {
		renderHeader(out, flags);
		out.write("<label for='" + key + "'>" + label);
		renderControl(out);
		out.write("</label>");
	}
	
	public boolean needsMultipartEncoding() {
		return false;
	}
	
	public void renderHeader(PrintWriter out, Set<String> flags) throws IOException {
		// do nothing
	}

	public abstract void renderControl(PrintWriter out) throws IOException;
}