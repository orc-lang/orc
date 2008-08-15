package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * This doesn't correspond to any specific Orc
 * type but is instead intended to be used by
 * implementors of {@link TraceableValue} to
 * encode object-like immutable structures.
 */
public class DictValue extends ObjectValue {
	Map<String, Value> properties = new HashMap<String, Value>();
	/**
	 * Ok, so this isn't <i>really</i> immutable; you
	 * have to call {@link #put(String, Value)} to actually
	 * set the mapped values. Just promise not to change
	 * anything after you serialize the object.
	 */
	public DictValue(Class class_) {
		super(class_);
	}
	public void put(String key, Value value) {
		properties.put(key, value);
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		super.prettyPrint(out, indent);
		if (properties.isEmpty()) {
			out.write("{}");
			return;
		}
		out.write("{\n");
		indent++;
		for (Map.Entry<String, Value> entry : properties.entrySet()) {
			indent(out, indent);
			out.write(entry.getKey());
			out.write(": ");
			entry.getValue().prettyPrint(out, indent);
			out.write("\n");
		}
		
		indent--;
		indent(out, indent);
		out.write("}");
	}
}
