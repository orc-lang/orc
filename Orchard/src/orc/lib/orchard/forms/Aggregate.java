package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public abstract class Aggregate implements Part<Map<String, Object>> {
	protected String key;
	protected List<Part<?>> parts = new LinkedList<Part<?>>();
	
	public Aggregate(String key) {
		this.key = key;
	}
	
	public Map<String, Object> getValue() {
		HashMap<String, Object> out = new HashMap<String, Object>();
		for (Part<?> part : parts) {
			out.put(part.getKey(), part.getValue());
		}
		return out;
	}
	
	public void addPart(Part<?> part) {
		parts.add(part);
	}

	public void readRequest(HttpServletRequest request, List<String> errors) {
		for (Part<?> part : parts) {
			part.readRequest(request, errors);
		}
	}

	public void render(PrintWriter out) throws IOException {
		for (Part<?> part : parts) {
			part.render(out);
		}
	}

	public String getKey() {
		return key;
	}
}
