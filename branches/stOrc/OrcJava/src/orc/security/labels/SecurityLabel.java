//
// SecurityLabel.java -- Java class SecurityLabel
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

package orc.security.labels;

import orc.error.compiletime.typing.TypeException;

/**
 * Security label for a value. This label can be composed of a confidentiality
 * part and an integrity part.  Labels form a lattice under a "restrictiveness"
 * relation.  
 *
 * @author jthywiss
 */
public class SecurityLabel implements Comparable<SecurityLabel> {
	// When we get beyond experimenting, factor out interfaces for this
	// First experiment: a total order.  Lattices come later....

	public static final SecurityLabel DEFAULT = new SecurityLabel(0);
	// NOTE: Keep the above in sync with package orc.ast.extended.security.SecurityLabel.DEFAULT
	
	/**
	 * The greatest / top label -- all labels are sublabels of this.
	 */
	public static final SecurityLabel TOPLABEL = new SecurityLabel(9);

	/**
	 * The least / bottom label -- no labels are sublabels of this.
	 */
	public static final SecurityLabel BOTLABEL = new SecurityLabel(0);

	public SecurityLabel(final int level) {
		this.level = level;
	}

	public int compareTo(final SecurityLabel that) {
		if (this.level == that.level) {
			return 0;
		} else if (this.level < that.level) {
			return -1;
		} else /* (this.level > that.level) */{
			return 1;
		}
	}

	public boolean sublabel(final SecurityLabel that) {
		// equal -> true
		// less -> true
		// greater -> false
		// incomparable -> false

		return this.level <= that.level;
	}

	public void assertSublabel(final SecurityLabel that) throws TypeException {
		if (!this.sublabel(that)) {
			throw new TypeException("Expected label " + that + " or some sublabel, found label " + this + " instead.");
		}
	}

	public boolean superlabel(final SecurityLabel that) {
		return that.sublabel(this);
	}

	/* By default, equality is based on mutual sublabels.
	 */
	public boolean equal(final SecurityLabel that) {
		return this.sublabel(that) && that.sublabel(this);
	}

	/* Find the join (least upper bound) in the sublabel lattice
	 * of this label and another label.
	 */
	public SecurityLabel join(final SecurityLabel that) {
		if (this.sublabel(that)) {
			return that;
		} else if (that.sublabel(this)) {
			return this;
		} else {
			//TODO:stOrc: Broken for partial orders
			return TOPLABEL;
		}
	}

	/* Find the meet (greatest lower bound) in the sublabel lattice
	 * of this label and another label.
	 */
	public SecurityLabel meet(final SecurityLabel that) {
		if (this.sublabel(that)) {
			return this;
		} else if (that.sublabel(this)) {
			return that;
		} else {
			//TODO:stOrc: Broken for partial orders
			return BOTLABEL;
		}

	}

	@Override
	public String toString() {
		return "{" + Integer.toString(this.level) + "}";
	}

	private final int level;
}
