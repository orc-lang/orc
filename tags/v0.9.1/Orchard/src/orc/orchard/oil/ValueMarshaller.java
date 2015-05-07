package orc.orchard.oil;

import java.util.LinkedList;

import orc.runtime.values.Visitor;

public class ValueMarshaller implements Visitor<Value> {

	public Value visit(orc.runtime.values.Closure v) {
		return new UnrepresentableValue(v.toString());
	}

	public Value visit(orc.runtime.values.Constant v) {
		Object value = v.getValue();
		if (value instanceof String) {
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
		return new Constant(value);
	}

	public Value visit(orc.runtime.values.Field v) {
		return new Field(v.getKey());
	}

	public Value visit(orc.runtime.values.ListValue v) {
		java.util.List<orc.runtime.values.Value> list = v.enlist();
		java.util.List<Value> mlist = new LinkedList<Value>();
		for (orc.runtime.values.Value v2 : list) {
			mlist.add(v2.accept(this));
		}
		return new List(mlist.toArray(new Value[]{}));
	}

	public Value visit(orc.runtime.values.OptionValue v) {
		return new Option(v.isNone()
				? null
				: v.untag().accept(this));
	}

	public Value visit(orc.runtime.values.TupleValue v) {
		Value mvalues[] = new Value[v.size()];
		int i = 0;
		for (orc.runtime.values.Value v2 : v) {
			mvalues[i++] = v2.accept(this);
		}
		return new Tuple(mvalues);
	}

	public Value visit(orc.runtime.sites.Site v) {
		return new UnrepresentableValue(v.toString());
	}

}
