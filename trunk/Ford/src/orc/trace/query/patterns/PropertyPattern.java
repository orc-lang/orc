package orc.trace.query.patterns;

import java.io.IOException;
import java.io.Writer;

import orc.trace.RecordTerm;
import orc.trace.Term;
import orc.trace.query.Frame;

/**
 * Extract a property from a {@link RecordTerm}. Property patterns are special
 * in that they can only unify if the variable is already bound to a record
 * (it's not practical to enumerate every record which may bind a given
 * property).
 * 
 * @author quark
 */
public class PropertyPattern extends BindingPattern {
	/** Reference to the record */
	private final BindingPattern record;
	/** Key of property to extract */
	private final String key;
	public PropertyPattern(BindingPattern record, String key) {
		this.record = record;
		this.key = key;
	}
	public Frame unify(Frame frame, Term value) {
		// Evaluate the record variable
		Term record1 = record.evaluate(frame);
		if (record1 instanceof RecordTerm) {
			Term value1 = ((RecordTerm)record1).getProperty(key);
			return frame.unify(value1, value);
		} else {
			// If it's not bound yet, we can't unify
			return null;
		}
	}
	public Term evaluate(Frame frame) {
		Term record1 = record.evaluate(frame);
		if (record1 instanceof RecordTerm) {
			return ((RecordTerm)record1).getProperty(key);
		} else {
			return this;
		}
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		record.prettyPrint(out, indent);
		out.write(".");
		out.write(key);
	}
	public boolean occurs(Variable v) {
		return record.occurs(v);
	}
}
