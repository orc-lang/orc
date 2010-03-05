//
// ListValue.java -- Java class ListValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import orc.trace.Terms;

public abstract class ListValue extends AbstractValue implements Iterable<Value> {
	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write("[");
		Terms.prettyPrintList(out, indent + 1, this, ", ");
		out.write("]");
	}

	private static class ListIterator implements Iterator<Value> {
		private ListValue list;

		public ListIterator(final ListValue list) {
			this.list = list;
		}

		public boolean hasNext() {
			return !(list instanceof NilValue);
		}

		public Value next() {
			if (!(list instanceof ConsValue)) {
				throw new NoSuchElementException();
			}
			final ConsValue cons = (ConsValue) list;
			final Value out = cons.head;
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
