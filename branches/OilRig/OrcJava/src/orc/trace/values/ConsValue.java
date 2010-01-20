package orc.trace.values;


public class ConsValue extends ListValue {
	public final Value head;
	public final ListValue tail;
	public ConsValue(final Value head, final ListValue tail) {
		super();
		this.head = head;
		this.tail = tail;
	}
	public boolean equals(Object that) {
		return that instanceof ConsValue
				&& ((ConsValue)that).head.equals(head)
				&& ((ConsValue)that).tail.equals(tail);
	}
}
