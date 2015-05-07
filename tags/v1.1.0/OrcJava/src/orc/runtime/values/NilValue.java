//
// NilValue.java -- Java class NilValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.Token;

public class NilValue<E> extends ListValue<E> {
	public static final NilValue singleton = new NilValue();

	private NilValue() {
	}

	@Override
	public String toString() {
		return "[]";
	}

	@Override
	public void uncons(final Token caller) {
		caller.die();
	}

	@Override
	public void unnil(final Token caller) {
		caller.resume(Value.signal());
	}

	@Override
	public List<E> enlist() {

		return new LinkedList<E>();
	}

	@Override
	public <T> T accept(final Visitor<T> visitor) {
		return visitor.visit(this);
	}

	public boolean eqTo(final Object that) {
		return that instanceof NilValue;
	}

	@Override
	public int hashCode() {
		return NilValue.class.hashCode();
	}

	public boolean contains(final Object o) {
		return false;
	}

	public boolean isEmpty() {
		return true;
	}

	public int size() {
		return 0;
	}
}
