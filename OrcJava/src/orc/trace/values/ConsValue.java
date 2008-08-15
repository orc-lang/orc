package orc.trace.values;

import java.util.Iterator;

import xtc.util.Utilities;

public class ConsValue extends ListValue {
	public final Value head;
	public final ListValue tail;
	public ConsValue(final Value head, final ListValue tail) {
		super();
		this.head = head;
		this.tail = tail;
	}
}
