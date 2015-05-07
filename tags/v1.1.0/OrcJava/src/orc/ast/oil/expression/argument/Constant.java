//
// Constant.java -- Java class Constant
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.expression.argument;

import java.math.BigInteger;

import orc.ast.oil.visitor.Visitor;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.ground.ConstIntType;
import xtc.util.Utilities;

public class Constant extends Argument {

	public Object v;

	public Constant(final Object v) {
		this.v = v;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return v == null ? 0 : v.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Constant other = (Constant) obj;
		if (v == null) {
			if (other.v != null) {
				return false;
			}
		} else if (!v.equals(other.v)) {
			return false;
		}
		return true;
	}

	@Override
	public Object resolve(final Env<Object> env) {
		return v;
	}

	@Override
	public String toString() {
		if (v == null) {
			return "null";
		} else if (v instanceof String) {
			return '"' + Utilities.escape((String) v, Utilities.JAVA_ESCAPES) + '"';
		} else {
			return v.toString();
		}
	}

	@Override
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(final TypingContext ctx) throws TypeException {
		if (v == null) {
			return Type.BOT;
		} else if (v instanceof Integer) {
			return new ConstIntType((Integer) v);
		} else if (v instanceof BigInteger) {
			return Type.INTEGER;
		} else if (v instanceof Number) {
			return Type.NUMBER;
		} else if (v instanceof String) {
			return Type.STRING;
		} else if (v instanceof Boolean) {
			return Type.BOOLEAN;
		} else {
			// TODO: Expand to cover arbitrary Java classes
			return Type.TOP;
		}
	}

	@Override
	public orc.ast.xml.expression.argument.Argument marshal() throws CompilationException {
		return new orc.ast.xml.expression.argument.Constant(v);
	}
}
