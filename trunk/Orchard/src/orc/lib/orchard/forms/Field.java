package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public abstract class Field<V> implements Part<V> {
	public static class ValidationException extends Exception {
		private String message;
		public ValidationException(String message) {
			this.message = message;
		}
		public String getMessage() {
			return message;
		}
	}
	
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

	public void readRequest(HttpServletRequest request, List<String> errors) {
		try {
			value = requestToValue(request.getParameter(key));
		} catch (ValidationException e) {
			errors.add(e.getMessage());
		}
	}
	
	public void render(PrintWriter out) throws IOException {
		out.write("<label for='" + key + "'>" + label);
		renderControl(out);
		out.write("</label>");
	}
	
	public abstract V requestToValue(String value) throws ValidationException;
	public abstract void renderControl(PrintWriter out) throws IOException;
	
	public static String escapeHtml(String text) {
		return org.apache.commons.lang.StringEscapeUtils.escapeHtml(text);
	}
}
