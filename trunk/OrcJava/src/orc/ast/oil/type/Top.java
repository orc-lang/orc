//
// Top.java -- Java class Top
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

package orc.ast.oil.type;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;

/**
 * The syntactic type 'Top', supertype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {

	/**
	 * All instances of Top return a constant hash code. 
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return -2070517161; //"orc.ast.oil.expression.Top".hashCode()
	}

	/**
	 * All instances of Top are equal.
	 * 
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
		return true;
	}

	@Override
	public orc.type.Type transform(final TypingContext ctx) throws TypeException {
		return orc.type.Type.TOP;
	}

	@Override
	public String toString() {
		return "Top";
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.Top();
	}
}
