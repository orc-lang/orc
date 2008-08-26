package orc.trace.values;

import java.io.IOException;
import java.io.Writer;

/**
 * The only thing we can reliably record about a Java
 * object is its identity. We could represent object
 * identity by object identity (i.e. eschew any explicit
 * id property), but that would require serialization
 * to preserve object identity, which is not the case if
 * you call {@link java.io.ObjectOutputStream#reset()}.
 * So instead we use an explicit unique numeric id for
 * each instance.
 * 
 * @author quark
 */
public class ObjectValue extends AbstractValue {
	private static long lastID = 0;
	/**
	 * A long is necessary instead of an int because our id space needs to
	 * include a unique id for all values ever, not just for all values at a
	 * particular point in the program (like an address).
	 */
	public final long id;
	public final Class class_;
	public ObjectValue(Class class_) {
		this.class_ = class_;
		this.id = ++lastID;
	}
	@Override
	public boolean equals(Object that) {
		return that instanceof ObjectValue
			&& ((ObjectValue)that).id == id;
	}
	@Override
	public int hashCode() {
		return (int)(id % Integer.MAX_VALUE);
	}
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write(class_.getName());
		out.write("#");
		out.write(String.valueOf(id));
	}
}
