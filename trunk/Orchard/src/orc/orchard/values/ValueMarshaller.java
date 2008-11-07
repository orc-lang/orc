package orc.orchard.values;

import java.util.LinkedList;

import orc.runtime.values.Visitor;

public class ValueMarshaller extends Visitor<Object> {
	public Object visit(orc.runtime.values.ListValue v) {
		java.util.List<Object> list = v.enlist();
		java.util.List<Object> mlist = new LinkedList<Object>();
		for (Object v2 : list) {
			mlist.add(visit(this, v2));
		}
		return new List(mlist.toArray(new Object[]{}));
	}

	public Object visit(orc.runtime.values.TupleValue v) {
		Object mvalues[] = new Object[v.size()];
		int i = 0;
		for (Object v2 : v) {
			mvalues[i++] = visit(this, v2);
		}
		return new Tuple(mvalues);
	}
	
	public Object visit(orc.runtime.values.TaggedValue v) {
		Object mvalues[] = new Object[v.values.length];
		int i = 0;
		for (Object v2 : v.values) {
			mvalues[i++] = visit(this, v2);
		}
		return new Tagged(v.tag.tagName, mvalues);
	}

	public Object visit(orc.runtime.values.Value v) {
		return new UnrepresentableValue(v.toString());
	}

	@Override
	public Object visit(Object value) {
		if (value == null) {
			return value;
		} else if (value instanceof String) {
		} else if (value instanceof Boolean) {
		} else if (value instanceof Number) {
		} else if (value instanceof Character) {
		} else if (value instanceof	java.util.Calendar) {
		} else if (value instanceof	java.util.Date) {
		} else if (value instanceof	javax.xml.namespace.QName) {
		} else if (value instanceof	java.net.URI) {
		} else if (value instanceof	javax.xml.datatype.Duration) {
		} else if (value instanceof	java.util.UUID) {
		} else {
			return new UnrepresentableValue(value.toString());
		}
		return value;
	}
}
