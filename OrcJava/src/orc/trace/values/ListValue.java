package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import orc.trace.Terms;

public abstract class ListValue extends AbstractValue implements Iterable<Value> {
	public void prettyPrint(Writer out, int indent) throws IOException {
		out.write("[");
		Terms.prettyPrintList(out, indent+1, this, ", ");
		out.write("]");
	}
	private static class ListIterator implements Iterator<Value> {
		private ListValue list;
		public ListIterator(ListValue list) {
			this.list = list;
		}
		public boolean hasNext() {
			return !(list instanceof NilValue);
		}
		public Value next() {
			if (!(list instanceof ConsValue))
				throw new NoSuchElementException();
			final ConsValue cons = (ConsValue)list;
			Value out = cons.head;
			list = cons.tail;
			return out;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	public Iterator<Value> iterator() {
		return new ListIterator(this);
	}
}
